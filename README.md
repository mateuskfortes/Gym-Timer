# Gym Timer 2

Aplicativo Android para cadastrar exercícios, definir descanso e associar trechos de músicas locais a cada exercício. Durante o treino, um timer flutuante acompanha séries e intervalos sobre outros aplicativos.

## Funcionalidades

- Cadastro e edição de exercícios com carga, unidade (`kg` ou posições da máquina), tempo de descanso e antecipação do trecho musical.
- Seleção de músicas do dispositivo, edição do nome salvo no app, cadastro de trechos e associação de trechos aos exercícios.
- Dias de treino com nome editável. Cada exercício pertence a um dia; arraste a alça do cartão para definir a ordem. É possível mudar o dia de um exercício na tela de edição.
- Timer flutuante arrastável: toque para iniciar e encerrar uma série. Ao encerrar, um painel permite atualizar a carga (opcional), manter o exercício ou avançar para o próximo da ordem. A escolha inicia o descanso correspondente. Durante a contagem do descanso, o fundo fica com 30% de opacidade e a janela compacta mostra só os números. Arraste o timer até a área de remoção para fechá-lo.
- Reprodução de um trecho associado ao iniciar a série, quando há permissão de áudio.
- Aba **Ajustes** à direita da navegação: escolha tema claro ou escuro e uma das 12 cores de destaque. A escolha é salva no dispositivo e aplicada às telas e ao timer flutuante, inclusive se ele já estiver aberto.

## Requisitos

- Android Studio com Android SDK instalado, JDK 21 e o Gradle Wrapper incluído no repositório.
- SDK de compilação Android 36 (com minor API level 1, conforme `app/build.gradle.kts`).
- Dispositivo ou emulador Android 7.0/API 24 ou superior para executar o app. Para testar músicas e sobreposição, use um dispositivo com arquivos de áudio locais e suporte à permissão de sobreposição.

## Executar em desenvolvimento

1. Abra a pasta do projeto no Android Studio e sincronize o Gradle.
2. Configure `local.properties` com `sdk.dir` se o Android Studio não o criar automaticamente.
3. Configure localmente a assinatura exigida pela configuração Gradle atual. O arquivo `key.properties` na raiz deve conter `storeFile`, `storePassword`, `keyAlias` e `keyPassword`; `storeFile` aponta para uma keystore acessível ao módulo `app`. **Não compartilhe nem versione esses arquivos ou credenciais.** A configuração de release é lida mesmo ao configurar tarefas de debug.
4. Selecione a variante `devDebug` e execute o aplicativo, ou rode:

   ```bash
   ./gradlew :app:assembleDevDebug
   ```

O flavor `dev` usa o ID `com.example.gymtimer.dev`; `prod` usa `com.example.gymtimer2`. O banco tem nomes diferentes em builds debug e não debug. O app usa banco local Room e músicas do MediaStore do dispositivo; não há servidor configurado.

## Verificação estática

O projeto já inclui o **Android Lint** por meio do plugin Android Gradle. Para verificar a variante de desenvolvimento, rode:

```bash
./gradlew :app:lintDevDebug
```

O relatório fica em `app/build/reports/lint-results-devDebug.html`. Erros fazem a tarefa falhar; avisos ficam registrados no relatório.

Na primeira abertura após atualizar uma instalação antiga, todos os exercícios existentes aparecem em **Dia 1**, na ordem dos IDs. Para criar ou renomear dias, use os botões ao lado de **Dias de treino**. Só é possível excluir um dia vazio, e deve restar ao menos um dia.

## Rodar no celular por USB ou Wi-Fi

Rode **na raiz do projeto**:

```bash
./run-app.sh
```

Se já houver um único dispositivo autorizado por USB ou Wi-Fi, o script compila a variante `devDebug`, instala o APK e abre o app. Se não houver dispositivo conectado, ele pede o IP e a porta de conexão da **Depuração sem fio** e executa `adb connect` automaticamente. O IP padrão é `192.168.1.104` quando o campo fica vazio. O pareamento inicial do Android 11+ ainda precisa ser feito uma vez, conforme os passos abaixo. O antigo `./run-usb.sh` continua funcionando como atalho. Para usar breakpoints, selecione o celular e clique em **Debug** no Android Studio; o script executa o app sem anexar o depurador.

### Primeira conexão por USB

1. No celular, ative **Opções do desenvolvedor** (em geral, toque sete vezes em **Número da versão**) e habilite **Depuração USB**. O caminho exato varia conforme o fabricante. Consulte as [instruções oficiais do Android](https://developer.android.com/studio/debug/dev-options).
2. Conecte o celular por um cabo USB com transferência de dados, desbloqueie a tela e aceite **Permitir depuração USB?** quando o aviso aparecer.
3. Se precisar conferir a conexão, rode:

   ```bash
   ~/Android/Sdk/platform-tools/adb devices
   ```

   O aparelho deve aparecer com o estado `device`. Se aparecer `unauthorized`, aceite a autorização na tela do celular.

### Primeira conexão por Wi-Fi (Android 11+)

1. Conecte computador e celular à mesma rede. No celular, abra **Opções do desenvolvedor > Depuração sem fio**, ative a opção e toque em **Parear dispositivo com código de pareamento**.
2. Use o **IP e a porta exibidos na tela de pareamento** e digite o código quando o ADB pedir:

   ```bash
   ~/Android/Sdk/platform-tools/adb pair IP:PORTA_DE_PAREAMENTO
   ```

3. Volte à tela principal de **Depuração sem fio**. Rode `./run-app.sh` e informe o **Endereço IP** e a **porta** exibidos nessa tela. Essa porta costuma ser diferente da porta de pareamento. Se o IP for `192.168.1.104`, basta pressionar Enter no primeiro prompt para usar o padrão:

   ```bash
   ./run-app.sh
   ```

4. O script conecta, compila, instala e abre o app. O pareamento precisa ser feito apenas uma vez, salvo se for revogado no celular.

Se `adb pair` ou `adb connect` falhar, confira o IP e a porta **atuais** no celular: eles podem mudar. `~/Android/Sdk/platform-tools/adb mdns services` mostra os serviços `_adb-tls-pairing._tcp` e `_adb-tls-connect._tcp` encontrados na rede. Veja o [guia oficial do ADB](https://developer.android.com/tools/adb#connect-to-a-device-over-wi-fi).

Para depurar com breakpoints, abra este projeto no Android Studio, aguarde a sincronização do Gradle, escolha **`devDebug`** em **Build Variants**, selecione o celular e a configuração **`app`**, e clique em **Debug** (ícone de inseto).

Se o dispositivo não aparecer, teste outro cabo ou porta USB e use **Tools > Troubleshoot Device Connections** no Android Studio. No Ubuntu, talvez seja necessário configurar o grupo `plugdev` e as regras `udev`; veja o [guia oficial para dispositivo físico](https://developer.android.com/studio/run/device). A configuração local de assinatura descrita acima também é necessária para a sincronização do Gradle neste projeto.

## Permissões

- **Áudio local:** `READ_MEDIA_AUDIO` no Android 13+ ou `READ_EXTERNAL_STORAGE` até Android 12L. Necessária para listar e reproduzir músicas do dispositivo. A biblioteca e a seleção mostram **Permitir** quando falta acesso; tocar em uma música ou refrão abre o pedido de permissão do Android. Se o acesso for negado, a reprodução não começa.
- **Sobreposição:** `SYSTEM_ALERT_WINDOW`. O app encaminha às configurações ao abrir o timer sem essa permissão.
- **Serviço em primeiro plano:** o timer flutuante é executado por `OverlayService` com uma notificação persistente.

## Documentação

O desenho original do ícone do aplicativo está em [`design/app-icon.svg`](design/app-icon.svg). O Android usa os vetores em `app/src/main/res/drawable/` para o ícone adaptativo e versões WebP em `mipmap-*` para Android 7 e launchers sem suporte a ícones adaptativos.

- [Arquitetura e fluxo de dados](docs/ARCHITECTURE.md)
- [Desenvolvimento, testes e convenções](docs/DEVELOPMENT.md)
- [Instruções para agentes](AGENTS.md)
