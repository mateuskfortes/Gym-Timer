# Gym Timer 2

Aplicativo Android para cadastrar exercícios, definir descanso e associar trechos de músicas locais a cada exercício. Durante o treino, um timer flutuante acompanha séries e intervalos sobre outros aplicativos.

## Funcionalidades

- Cadastro e edição de exercícios com carga, unidade (`kg` ou posições da máquina), tempo de descanso e antecipação do trecho musical.
- Seleção de músicas do dispositivo, edição do nome salvo no app, cadastro de trechos e associação de trechos aos exercícios.
- Dias de treino com nome editável. Use o menu de três linhas ao lado do seletor para criar, renomear ou excluir dias. Cada exercício pertence a um dia; arraste a alça do cartão para definir a ordem. É possível mudar o dia de um exercício na tela de edição.
- Timer flutuante arrastável: toque para iniciar e encerrar uma série. Ao encerrar, um painel permite atualizar a carga (opcional), manter o exercício ou avançar para o próximo da ordem. A escolha inicia o descanso correspondente. Durante a contagem do descanso, o fundo fica com 30% de opacidade e a janela compacta mostra só os números. Arraste o timer até a área de remoção para fechá-lo.
- Reprodução de um trecho associado ao iniciar a série, quando há permissão de áudio.
- Aba **Ajustes** à direita da navegação: escolha tema claro ou escuro e uma das 12 cores de destaque. A escolha é salva no dispositivo e aplicada às telas e ao timer flutuante, inclusive se ele já estiver aberto.
- Aba **Corridas**: consulte o histórico local e abra a sessão ativa numa tela exclusiva, com mapa em tela cheia. Na preparação, crie um treino estruturado com etapas por tempo, distância, variação de elevação ou avanço manual, incluindo metas de pace, pausas e repetições; o plano e o progresso ficam salvos com a corrida e o painel mostra a etapa atual. Em **Ajustes**, controle os avisos visuais de afastamento, que podem ser silenciados até voltar à rota ou até o fim da corrida, as instruções de direção e a pausa automática, o botão para marcar voltas e a opção de manter a tela ligada e reduzir o brilho por inatividade e mostrar a corrida aberta na tela de bloqueio. A tela permanece ligada enquanto o painel da corrida estiver aberto; isso pode aumentar o consumo de bateria. Etapas de elevação avançam quando a altitude se afasta do extremo recente pelo valor configurado; leituras sem precisão vertical suficiente não avançam a etapa. Defina também uma faixa opcional de pace antes de começar; o painel informa se o pace atual está dentro da meta. A faixa fica salva com a corrida no banco local. Com uma rota planejada, o painel estima curvas a partir do traçado selecionado; em **Ajustes**, escolha exibir todas as direções futuras ou limitar a quantidade mostrada. A direção é inferida da geometria e não vem de um serviço de roteamento. Para voltar ao início, escolha o caminho gravado, disponível sem internet, ou peça uma rota a pé por ruas. Só essa escolha envia as coordenadas inicial e atual e a identificação técnica do app ao serviço público Valhalla; o pedido exige internet, pode falhar e não é salvo. O mapa não avalia as condições da via. O painel inferior mostra tempo ativo, distância, pace e controles; arraste ou toque na seta para recolher e expandir. Um botão oculta o painel para mostrar o mapa em tela cheia e restaura o estado anterior ao reabri-lo. Com altitude GPS confiável, outro controle colore os trechos como descida, plano ou subida. Voltar ao histórico mantém a gravação. O zoom e a navegação manual são preservados; quando a posição sai do centro, um botão permite centralizar novamente e restaurar o zoom de acompanhamento. O histórico destaca a sessão aberta, com estado, métricas e atalho para voltar à corrida. Ao finalizar, salve com um título opcional ou descarte a sessão após confirmar a exclusão do percurso. O título aparece no histórico e no detalhe; sessões sem título usam a data. Na finalização ou edição, selecione ou cadastre um equipamento local. O catálogo soma a distância das corridas concluídas, permite definir um padrão e editar equipamentos ao renomear, arquivar ou reativar. Nomes já salvos continuam no histórico após a migração. A nota pessoal fica só neste dispositivo. No detalhe, adicione até 10 fotos por corrida; o app copia cada imagem para armazenamento privado e remove os arquivos ao excluir a foto ou a corrida. Corridas concluídas podem ser excluídas com confirmação, junto com seus pontos do percurso. Durante a gravação ou pausa, uma notificação permite voltar ao app; se o Android bloquear o canal ou a permissão, o histórico mostra um atalho para ativar as notificações. O histórico e os pontos do percurso ficam no banco local. O **Registro de treino** agrupa atividades concluídas por semana, resume distância ou tempo ativo e abre a atividade selecionada pelo dia.
- Durante uma corrida ou na preparação, o menu **Camadas do mapa** alterna entre o mapa padrão, a inclinação registrada por GPS, a densidade dos últimos sete dias e o histórico local de corridas concluídas, quando cada camada tem dados. As camadas de atividade agrupam o histórico no aparelho, sem enviar os pontos das corridas a um servidor. Elas não consultam uma conta e não equivalem aos mapas de calor do Strava.
- A camada **Pontos de interesse (OpenStreetMap)** consulta água, banheiros, abrigos, bancos, locais de alimentação e parques somente quando selecionada. Ela envia apenas os limites da área visível ao Overpass público; o percurso da corrida permanece local. A disponibilidade depende da conexão e da capacidade compartilhada do serviço.
- Em **Ajustes**, escolha unidades métricas ou imperiais para as métricas da corrida. Corridas e pontos continuam salvos em metros; distância, pace, velocidade, elevação, parciais e notificações são convertidos apenas para exibição.
- Em **Ajustes**, ative ou desative a comparação de segmentos locais. Ela usa percursos salvos neste dispositivo e não sincroniza segmentos com o Strava. A opção vale para corridas futuras e fica bloqueada enquanto uma sessão estiver aberta.

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

O flavor `dev` usa o ID `com.example.gymtimer.dev` e aparece no launcher como **Gym Timer (Desenvolvimento)**, com ícone laranja; `prod` usa o ID `com.example.gymtimer2` e mantém o ícone original. O script `./run-app.sh` instala a variante `devDebug`. O banco tem nomes diferentes em builds debug e não debug. O app usa banco local Room e músicas do MediaStore do dispositivo; não há servidor configurado.

## Verificação estática

O projeto já inclui o **Android Lint** por meio do plugin Android Gradle. Para verificar a variante de desenvolvimento, rode:

```bash
./gradlew :app:lintDevDebug
```

O relatório fica em `app/build/reports/lint-results-devDebug.html`. Erros fazem a tarefa falhar; avisos ficam registrados no relatório.

### Detekt: complexidade e qualidade Kotlin

Na raiz do repositório, execute o script:

```bash
./analyze-code.sh
```

O script executa `:app:detekt` pelo Gradle Wrapper; não precisa de emulador. Para passar opções ao Gradle, acrescente-as ao comando, por exemplo `./analyze-code.sh --console=plain`. O Detekt habilita as regras embutidas com `allRules`; `config/detekt/detekt.yml` desativa convenções de baixo sinal e verificações de formatação/documentação automática. A complexidade cognitiva fica excluída de `ui/`, onde as lambdas do Compose geravam ruído. A complexidade ciclomática continua ativa em todo o Kotlin, com limite 10 por método; métodos têm limite de 120 linhas. Os relatórios HTML, XML, Markdown e SARIF são gravados em `app/build/reports/detekt/`. Se houver violações, o comando termina com erro e mostra arquivo e linha para orientar a correção.

Na primeira abertura após atualizar uma instalação antiga, todos os exercícios existentes aparecem em **Dia 1**, na ordem dos IDs. Para criar ou renomear dias, abra o menu **Gerenciar dias de treino**, ao lado do seletor. Só é possível excluir um dia vazio, e deve restar ao menos um dia.

## Rodar no celular por USB ou Wi-Fi

Rode **na raiz do projeto**:

```bash
./run-app.sh
```

Se já houver um único dispositivo autorizado por USB ou Wi-Fi, o script compila a variante `devDebug`, instala o APK e abre o app. Se não houver dispositivo conectado, ele tenta descobrir o endereço atual da **Depuração sem fio** por mDNS. Com um único endereço disponível, conecta automaticamente. Sem descoberta ou com vários celulares, pede o IP e a porta; também aceita colar `IP:porta` no primeiro campo. Se a conexão falhar, consulta novamente a porta anunciada pelo mesmo IP e permite tentar de novo sem reiniciar o script. O IP padrão é `192.168.1.104` quando o campo fica vazio. O pareamento inicial do Android 11+ ainda precisa ser feito uma vez, conforme os passos abaixo. O antigo `./run-usb.sh` continua funcionando como atalho. Para usar breakpoints, selecione o celular e clique em **Debug** no Android Studio; o script executa o app sem anexar o depurador.

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

3. Volte à tela principal de **Depuração sem fio**. Rode `./run-app.sh`. Se a descoberta automática não conectar, informe o **Endereço IP** e a **porta** exibidos nessa tela, separados ou como `IP:porta`. Essa porta costuma ser diferente da porta de pareamento. Se o IP for `192.168.1.104`, basta pressionar Enter no primeiro prompt para usar o padrão:

   ```bash
   ./run-app.sh
   ```

4. O script conecta, compila, instala e abre o app. O pareamento precisa ser feito apenas uma vez, salvo se for revogado no celular.

Se `adb pair` ou `adb connect` falhar, confira o IP e a porta **atuais** no celular: eles podem mudar. `Connection refused` significa que o endereço recusou a conexão TCP; a recusa pode ocorrer mesmo quando o valor foi digitado corretamente. Nesse caso, desative e ative a Depuração sem fio, confira o endereço mostrado novamente e repita no próprio script. O script aguarda a autorização do transporte antes de compilar ou instalar, inclusive quando o ADB retorna sucesso no comando mas não conecta de fato. `~/Android/Sdk/platform-tools/adb mdns services` mostra os serviços `_adb-tls-pairing._tcp` e `_adb-tls-connect._tcp` encontrados na rede. Veja o [guia oficial do ADB](https://developer.android.com/tools/adb#connect-to-a-device-over-wi-fi).

Para depurar com breakpoints, abra este projeto no Android Studio, aguarde a sincronização do Gradle, escolha **`devDebug`** em **Build Variants**, selecione o celular e a configuração **`app`**, e clique em **Debug** (ícone de inseto).

Se o dispositivo não aparecer, teste outro cabo ou porta USB e use **Tools > Troubleshoot Device Connections** no Android Studio. No Ubuntu, talvez seja necessário configurar o grupo `plugdev` e as regras `udev`; veja o [guia oficial para dispositivo físico](https://developer.android.com/studio/run/device). A configuração local de assinatura descrita acima também é necessária para a sincronização do Gradle neste projeto.

## Permissões

- **Áudio local:** `READ_MEDIA_AUDIO` no Android 13+ ou `READ_EXTERNAL_STORAGE` até Android 12L. Necessária para listar e reproduzir músicas do dispositivo. A biblioteca e a seleção mostram **Permitir** quando falta acesso; tocar em uma música ou refrão abre o pedido de permissão do Android. Se o acesso for negado, a reprodução não começa.
- **Sobreposição:** `SYSTEM_ALERT_WINDOW`. O app encaminha às configurações ao abrir o timer sem essa permissão.
- **Serviço em primeiro plano:** o timer flutuante é executado por `OverlayService` com uma notificação persistente.
- **Localização:** a aba Corridas pede localização precisa e confere GPS e permissão novamente antes de criar a sessão. Se o GPS estiver desligado, o app oferece abrir as configurações de localização. Se a permissão precisa for negada ou ficar apenas aproximada, o app oferece abrir as configurações de permissão. O serviço mantém o GPS ativo com a tela bloqueada. Em **Ajustes**, conceda **Permitir o tempo todo** para o app retomar automaticamente uma corrida ativa após uma falha; sem esse acesso, a sessão fica salva e pode ser retomada manualmente. A notificação de corrida depende da permissão de notificações no Android 13 ou superior. O mapa usa blocos online do OpenStreetMap e requer internet; os dados da corrida continuam salvos localmente.

## Documentação

O desenho original do ícone do aplicativo está em [`design/app-icon.svg`](design/app-icon.svg). O Android usa os vetores em `app/src/main/res/drawable/` para o ícone adaptativo e versões WebP em `mipmap-*` para Android 7 e launchers sem suporte a ícones adaptativos.

- [Arquitetura e fluxo de dados](docs/ARCHITECTURE.md)
- [Desenvolvimento, testes e convenções](docs/DEVELOPMENT.md)
- [Instruções para agentes](AGENTS.md)
