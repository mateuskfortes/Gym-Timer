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

# Só um transporte autorizado pode receber o APK; não seleciona o primeiro de vários aparelhos.
read_connected_devices() {
    mapfile -t connected_devices < <("$adb_cmd" devices | awk 'NR > 1 && $2 == "device" { print $1 }')
}

# Ignora anúncios de pareamento, pois eles não aceitam adb connect.
read_wireless_services() {
    wireless_services="$("$adb_cmd" mdns services 2>/dev/null | awk '
        $2 ~ /^_adb-tls-connect\._tcp\.?$/ && !seen[$3]++ { print $1, $3 }
    ' | sort -u)" || wireless_services=""
}

# Resolve também o serial mDNS: o ADB pode registrá-lo em vez do endereço IP:porta.
find_authorized_transport() {
    local endpoint="$1" service_name service_endpoint candidate
    if [[ "$("$adb_cmd" -s "$endpoint" get-state 2>/dev/null || true)" == "device" ]]; then
        device_serial="$endpoint"
        return 0
    fi
    while read -r service_name service_endpoint; do
        [[ "$service_endpoint" == "$endpoint" ]] || continue
        candidate="${service_name%.}"
        if [[ "$candidate" != *._adb-tls-connect._tcp ]]; then
            candidate+="._adb-tls-connect._tcp"
        fi
        if [[ "$("$adb_cmd" -s "$candidate" get-state 2>/dev/null || true)" == "device" ]]; then
            device_serial="$candidate"
            return 0
        fi
    done <<< "$wireless_services"
    return 1
}

# O status de saída de adb connect não basta: algumas falhas são retornadas como sucesso.
connect_endpoint() {
    local endpoint="$1" attempt
    echo "Conectando ao celular em ${endpoint}..."
    "$adb_cmd" connect "$endpoint" || true
    for attempt in {1..5}; do
        if find_authorized_transport "$endpoint"; then
            echo "Celular conectado: ${device_serial}"
            return 0
        fi
        if (( attempt < 5 )); then sleep 1; fi
    done
    return 1
}

# Descobre a porta anunciada agora; após uma falha manual, limita a busca ao IP informado.
connect_discovered_phone() {
    local requested_ip="${1:-}" service_name endpoint
    local -a endpoints=()
    read_wireless_services
    while read -r service_name endpoint; do
        [[ -n "$endpoint" ]] || continue
        if [[ -z "$requested_ip" || "${endpoint%:*}" == "$requested_ip" ]]; then
            endpoints+=("$endpoint")
        fi
    done <<< "$wireless_services"
    if [[ ${#endpoints[@]} -eq 1 ]]; then
        echo "Depuração sem fio encontrada em ${endpoints[0]}."
        connect_endpoint "${endpoints[0]}"
        return $?
    fi
    if [[ ${#endpoints[@]} -gt 1 ]]; then
        echo "Mais de um endereço anunciado. Informe o endereço do celular desejado:" >&2
        printf '  %s\n' "${endpoints[@]}" >&2
    fi
    return 1
}

# Valida antes de enviar endereços e portas ao ADB, inclusive valores com zeros à esquerda.
is_valid_ipv4() {
    local octet
    local -a octets
    [[ "$1" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]] || return 1
    IFS=. read -r -a octets <<< "$1"
    for octet in "${octets[@]}"; do
        if (( 10#$octet > 255 )); then return 1; fi
    done
}

# Aceita tanto IP e porta separados quanto o endereço IP:porta copiado da tela do Android.
read_phone_endpoint() {
    local entered_address phone_port
    read -r -p "IP do celular ou IP:porta [${phone_ip}]: " entered_address || return 1
    entered_address="${entered_address:-$phone_ip}"
    if [[ "$entered_address" == *:* ]]; then
        phone_ip="${entered_address%:*}"
        phone_port="${entered_address##*:}"
    else
        phone_ip="$entered_address"
        read -r -p "Porta de conexão da Depuração sem fio: " phone_port || return 1
    fi
    if ! is_valid_ipv4 "$phone_ip"; then
        echo "IP inválido: informe um endereço IPv4, como 192.168.1.104." >&2
        return 2
    fi
    if [[ ! "$phone_port" =~ ^[0-9]{1,5}$ ]] || (( 10#$phone_port < 1 || 10#$phone_port > 65535 )); then
        echo "Porta inválida: informe um número entre 1 e 65535." >&2
        return 2
    fi
    phone_endpoint="${phone_ip}:$((10#$phone_port))"
}

# Permite corrigir ou repetir a conexão sem reiniciar o script; EOF encerra sem instalar.
connect_manually() {
    local input_status
    phone_ip="192.168.1.104"
    while true; do
        if read_phone_endpoint; then
            if connect_endpoint "$phone_endpoint" || connect_discovered_phone "$phone_ip"; then return 0; fi
            echo "O ADB não confirmou a conexão com ${phone_endpoint}." >&2
            echo "Se houve 'Connection refused', a conexão TCP foi recusada; não é erro de digitação detectado pelo script." >&2
            echo "Com o endereço correto, desative e ative a Depuração sem fio e confira se o Android mudou a porta." >&2
            echo "Se o ADB pedir autenticação, use 'adb pair IP:PORTA_DE_PAREAMENTO' com o código do celular." >&2
            echo "Tente novamente abaixo, ou pressione Ctrl+C para sair." >&2
        else
            input_status=$?
            if [[ "$input_status" -eq 1 ]]; then
                echo "Entrada encerrada. Nenhum aplicativo foi instalado." >&2
                return 1
            fi
        fi
    done
}

"$adb_cmd" start-server
read_connected_devices
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
    wireless_services=""
    # A descoberta mDNS é assíncrona quando o servidor acaba de iniciar.
    if ! connect_discovered_phone; then
        sleep 1
        if ! connect_discovered_phone; then
            connect_manually || exit 1
        fi
    fi
fi

./gradlew :app:assembleDevDebug
"$adb_cmd" -s "$device_serial" install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
"$adb_cmd" -s "$device_serial" shell am start -n com.example.gymtimer.dev/com.example.gymtimer2.MainActivity
