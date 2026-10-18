#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
sdk_adb="$sdk_dir/platform-tools/adb"

if [[ -x "$sdk_adb" ]]; then
    adb_cmd="$sdk_adb"
elif command -v adb >/dev/null 2>&1; then
    adb_cmd="$(command -v adb)"
else
    echo "ADB não encontrado. Instale Android SDK Platform-Tools ou configure ANDROID_HOME." >&2
    exit 1
fi


mapfile -t connected_devices < <("$adb_cmd" devices | awk 'NR > 1 && $2 == "device" { print $1 }')

if [[ ${#connected_devices[@]} -gt 1 ]]; then
    echo "Mais de um dispositivo ADB está conectado. Deixe apenas o celular desejado conectado." >&2
    "$adb_cmd" devices >&2
    exit 1
fi

if [[ ${#connected_devices[@]} -eq 1 ]]; then
    device_serial="${connected_devices[0]}"
else
    mapfile -t unauthorized_devices < <(
        "$adb_cmd" devices | awk 'NR > 1 && NF >= 2 && ($2 == "unauthorized" || $2 == "no") { print $1 " (" $2 ")" }'
    )
    if [[ ${#unauthorized_devices[@]} -gt 0 ]]; then
        echo "O ADB encontrou dispositivos que ainda não estão autorizados:" >&2
        printf '  %s\n' "${unauthorized_devices[@]}" >&2
        echo "Confira a autorização na tela do celular e rode o script novamente." >&2
        exit 1
    fi

    default_phone_ip="192.168.1.104"
    read -r -p "IP do celular [${default_phone_ip}]: " phone_ip
    phone_ip="${phone_ip:-$default_phone_ip}"

    if [[ ! "$phone_ip" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
        echo "IP inválido: informe um endereço IPv4, como 192.168.1.104." >&2
        exit 1
    fi
    IFS=. read -r -a ip_octets <<< "$phone_ip"
    for octet in "${ip_octets[@]}"; do
        if (( 10#$octet > 255 )); then
            echo "IP inválido: cada parte do endereço deve estar entre 0 e 255." >&2
            exit 1
        fi
    done

    read -r -p "Porta de conexão da Depuração sem fio: " phone_port
    if [[ ! "$phone_port" =~ ^[0-9]{1,5}$ ]]; then
        echo "Porta inválida: informe um número entre 1 e 65535." >&2
        exit 1
    fi
    phone_port_number=$((10#$phone_port))
    if (( phone_port_number < 1 || phone_port_number > 65535 )); then
        echo "Porta inválida: informe um número entre 1 e 65535." >&2
        exit 1
    fi

    device_serial="${phone_ip}:${phone_port_number}"
    echo "Conectando ao celular em ${device_serial}..."
    if ! "$adb_cmd" connect "$device_serial"; then
        echo "Não foi possível conectar ao celular em ${device_serial}." >&2
        "$adb_cmd" devices >&2
        exit 1
    fi

    device_status="$("$adb_cmd" devices | awk -v target="$device_serial" '$1 == target { print $2; exit}')"
    if [[ "$device_status" != "device" ]]; then
        echo "Não foi possível conectar ao celular em ${device_serial}." >&2
        echo "Confira o IP e a porta da tela principal de Depuração sem fio; não use a porta de pareamento." >&2
        "$adb_cmd" devices >&2
        exit 1
    fi
fi

./gradlew :app:assembleDevDebug
"$adb_cmd" -s "$device_serial" install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
"$adb_cmd" -s "$device_serial" shell am start -n com.example.gymtimer.dev/com.example.gymtimer2.MainActivity
