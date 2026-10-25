# Desenvolvimento

## Rotina de mudança

1. Encontre o fluxo afetado em `docs/ARCHITECTURE.md` e siga a cadeia tela → ViewModel → repositório → DAO/MediaStore, ou serviço → controlador → repositório/player.
2. Defina o comportamento observável antes de editar. Mantenha a mudança no pacote responsável e evite misturar lógica de persistência com Composables.
3. Use nomes que expressem intenção e unidades (`Ms`, `Seconds`), entradas validadas e estado somente leitura para a UI. Extraia funções quando isso reduzir duplicação ou separar responsabilidades reais.
4. Se mudar tabelas Room, atualize versão, migração e snapshot em `app/schemas/`. Verifique abertura de dados criados por uma versão anterior.
5. Execute verificações proporcionais ao risco e registre o que foi testado.

## Comandos úteis

```bash
./gradlew :app:assembleDevDebug
./gradlew :app:testDevDebugUnitTest
./gradlew :app:lintDevDebug
./gradlew :app:connectedDevDebugAndroidTest
./analyze-code.sh
```

`connectedDevDebugAndroidTest` requer dispositivo ou emulador conectado. A configuração Gradle atual lê `key.properties` na configuração do módulo; prepare a assinatura local conforme o `README.md` antes dos comandos. `lintDevDebug` é fornecido pelo plugin Android Gradle e grava relatórios em `app/build/reports/lint-results-devDebug.html` e `.txt`. O projeto não usa um formatador automático.

### Testar o script de conexão

```bash
bash -n run-app.sh run-usb.sh
python3 -m unittest discover -s tests -p 'test_run_app.py' -v
```

Os testes usam ADB e Gradle simulados em diretórios temporários. Não conectam, compilam nem instalam em aparelhos reais. Cobrem USB, recusa TCP com status de saída zero, nova tentativa, descoberta assíncrona e mudança de porta, serial mDNS, anúncios de pareamento, entrada `IP:porta`, validação e vários aparelhos. Uma conexão real recusada continua dependendo da disponibilidade da Depuração sem fio no celular e da rede.

### Analisar complexidade e qualidade Kotlin

Rode o script na raiz do repositório; ele chama `:app:detekt` pelo Gradle Wrapper e não requer dispositivo ou emulador:

```bash
./analyze-code.sh
```

Argumentos adicionais são repassados ao Gradle, por exemplo `./analyze-code.sh --console=plain`. O Detekt habilita as regras embutidas com `allRules`; `config/detekt/detekt.yml` desativa convenções de baixo sinal e verificações de formatação/documentação automática, além de excluir `ui/` da complexidade cognitiva por causa do ruído das lambdas do Compose. A complexidade ciclomática segue ativa em todos os pacotes, com limite 10; métodos têm limite de 120 linhas. Os relatórios HTML, XML, Markdown e SARIF ficam em `app/build/reports/detekt/`. Violações fazem o comando falhar e informam o arquivo e a linha.

Para compilar, instalar e abrir o aplicativo, rode `./run-app.sh`. O script usa o único dispositivo autorizado que já estiver conectado por USB ou Wi-Fi; se não houver um, tenta descobrir um único endereço de conexão por mDNS antes de pedir IP e porta. Aceita `IP:porta` e permite repetir a tentativa, com nova consulta da porta anunciada pelo mesmo IP após falha. A instalação só ocorre após confirmar um transporte autorizado, seja pelo endereço ou pelo serial mDNS. O IP padrão é `192.168.1.104` quando o campo fica vazio. O pareamento inicial por Wi-Fi continua sendo feito no Android, uma vez. A [seção de conexão no README](../README.md#rodar-no-celular-por-usb-ou-wi-fi) explica o processo e o uso do depurador do Android Studio. Compilar com `assembleDevDebug` sozinho não instala nem inicia o app.

`WorkoutSequenceTest` cobre o avanço na ordem de uma sessão; `TimeInputTest` cobre a conversão e a rejeição de valores inválidos. `RunMetricsTest` cobre filtragem de pontos GPS, cálculo de distância, pace e formatação. `RunMapCameraStateTest` cobre o enquadramento único, bloqueio durante gestos, navegação livre após deslocar a câmera, zoom preservado ao seguir após centralizar, mudanças do painel e ausência de reenquadramento do histórico; roda localmente sem Android. `RunMapCenteringTest` cobre a tolerância visual e geográfica, inclusive na escala global. `RunMapIntegrationTest` usa um MapView real para verificar localização recebida após carregar o estilo, deslocamento com gesto nativo, botão que restaura o zoom de acompanhamento, posição acima do painel e preservação da escala nas atualizações GPS. Também verifica os pixels da rota visível no histórico, percursos curtos e coordenadas repetidas; deve ser executado em dispositivo ou emulador, com coordenadas sintéticas e sem criar corridas no Room. `ActiveRunScreenTest` cobre recolher/expandir o painel após atualizar métricas e voltar/cancelar sem finalizar a sessão; requer dispositivo ou emulador. `WorkoutMigrationTest` migra um banco da versão 7 até a versão 10, verifica os dados de treino e grava uma corrida com ponto GPS. `WorkoutScheduleTest` cobre ordem, consulta por dia, mudança de dia, carga e restrições de exclusão. Execute `connectedDevDebugAndroidTest` com um aparelho conectado para validar as operações Room.

`RunTitleTest` cobre título opcional, espaços externos e limite de 80 caracteres. `RunFinishDialogTest` cobre salvamento com e sem título, cancelamento e confirmação do descarte preservando o rascunho ao voltar. `RunPersistenceTest` cobre título e percurso persistidos, finalização idempotente e descarte com remoção dos pontos. `WorkoutMigrationTest` também cria um banco a partir do snapshot 9 e confirma que a migração 9 → 10 mantém sessões e percursos sem título. `RunTrackingNotificationTest` verifica ainda o título enviado ao serviço e o descarte da sessão com remoção da notificação.

`RunDeletionTest` verifica exclusão Room com remoção dos pontos em cascata e proteção de todos os estados não concluídos. `RunHistoryScreenTest` verifica confirmação, cancelamento, exclusão pelo detalhe, estado da sessão aberta e deslocamento gradual do cartão vizinho após excluir uma corrida, com relógio de animação controlado. `RunTrackingNotificationTest` verifica início, pausa, retomada e remoção da notificação do serviço real; requer GPS habilitado e concede localização e notificações pela regra de teste. Ele cria uma sessão temporária e a remove ao terminar. Execute testes do serviço em emulador dedicado, sem outra corrida ativa.

## Verificação manual dos fluxos Android

| Mudança | Verificação mínima |
| --- | --- |
| Exercícios ou Room | Criar e renomear dias; criar, editar, mover, ordenar e excluir exercícios; adicionar/remover refrões e conferir que **Salvar** persiste e **Cancelar** descarta; reabrir o app e conferir ordem e associações. Tentar excluir um dia com exercícios deve ser impedido. |
| Músicas ou player | Negar e depois conceder a permissão ao tocar música inteira, refrão e prévias nos exercícios; revogar o acesso com a tela aberta e tentar reproduzir novamente; listar, salvar e excluir música/trecho; conferir que o player para e restaura o volume ao terminar. |
| Overlay | Abrir após permissão já concedida e após concedê-la nas configurações; tocar para iniciar/encerrar série; editar carga no painel sobre outro app; testar **Manter**, **Próximo** e **Concluir treino**; conferir descanso do exercício escolhido, transparência, arrastar/fechar e notificação. |
| Aparência | Alternar claro/escuro e as 12 cores; abrir outras abas e reabrir o app para conferir persistência; com overlay aberto, alterar aparência e conferir timer e painel de carga. |
| Migração Room | Atualizar instalação com dados da versão anterior; conferir que os registros sobrevivem. |
| Corridas | Conceder localização precisa, iniciar e conferir mapa em tela cheia sem barra de abas; recolher e expandir o painel por gesto e botão; pausar, retomar e concluir uma corrida; salvar com título e sem título, reabrir o app e conferir identificação no histórico e detalhe; cancelar o salvamento e voltar da confirmação de descarte para conferir preservação do rascunho; confirmar o descarte e conferir remoção da sessão, percurso e notificação; voltar ao histórico e reabrir a sessão sem interromper a gravação; reduzir o zoom e aguardar novas posições GPS para conferir que a escala não volta ao padrão; deslocar o mapa, conferir que ele permanece na área escolhida e que o botão de centralizar aparece; centralizar e conferir que o zoom de acompanhamento foi restaurado e a posição aparece acima do painel; abrir/recolher o painel durante a navegação manual e conferir que ela permanece livre; girar a tela e conferir o painel, o botão Voltar e a atribuição OSM; bloquear a tela durante a gravação; conferir mapa e rota visíveis no histórico, inclusive após rolar a tela e em percursos curtos ou sem deslocamento, além de tempo ativo, distância e pace. Negar notificações e localização para conferir as mensagens e tentar iniciar novamente após conceder acesso. Desligar o GPS ou encerrar o processo e validar a recuperação da sessão. Excluir uma corrida pelo histórico ou detalhe: cancelar deve preservá-la; confirmar deve removê-la e excluir sua rota. No histórico, conferir o desaparecimento suave e o movimento dos cartões vizinhos ao excluir o primeiro, um intermediário e o último cartão; ao esvaziar a lista, conferir a mensagem de histórico vazio. Com sessão aberta, conferir o cartão de estado no histórico e a notificação durante gravação e pausa; tocar a notificação deve abrir o app. Bloquear notificações gerais ou o canal de corrida, conferir o atalho no cartão e voltar das configurações para verificar a atualização. Concluir deve remover o aviso. O mapa precisa de internet e atribuição OSM visível. |

## Convenções

- Siga a organização e os nomes já usados nos pacotes. Prefira classes e funções pequenas, com uma responsabilidade clara, a comentários que expliquem lógica difícil de seguir.
- Coloque textos novos de interface em `app/src/main/res/values/strings.xml`. Mantenha a interface em português.
- Não bloqueie a thread principal com MediaStore, Room ou preparação de áudio. Observe `Flow` no ciclo de vida adequado e cancele trabalho iniciado por componentes que terminam.
- Acrescente testes para regras novas ou defeitos corrigidos, especialmente validação, conversão de unidades, transições do timer e persistência. Testes devem verificar comportamento, sem repetir a implementação.
- Evite dependências e camadas novas sem necessidade demonstrada. Se uma refatoração ampla for necessária, faça-a em passos verificáveis.
- Nunca versione keystores, `key.properties`, `local.properties` ou credenciais. Esses arquivos já são ignorados pelo Git.

## Tema visual

As cinco abas principais não repetem seus nomes no topo e usam margem superior de 16 dp abaixo da área segura. `MainScreen` consome os insets após reservar o espaço do sistema, para evitar aplicação dupla em telas com `Scaffold` próprio. Títulos de seções e de telas internas continuam identificando seu conteúdo.

`GymTimerTheme` usa por padrão a paleta clara com fundo cinza suave, superfícies brancas e roxo escuro para ações principais. Em **Ajustes**, escolha o tema claro ou escuro e uma das 12 cores. `AppSettingsRepository` guarda as preferências em `SharedPreferences`, sem alterar o esquema Room, e as expõe por `StateFlow`. A configuração de superfícies fica em `ui/theme/AppStyle.kt`; as cores de destaque têm valores claro e escuro no modelo `AccentColor`. Os controles compartilhados ficam em `ui/components/AppComponents.kt`. Botões secundários e de ícone não recebem contornos decorativos.

O timer flutuante usa layouts XML do serviço. `OverlayAppearance` aplica o tema e a cor salvos às janelas, inclusive durante uma sessão aberta. Os recursos em `res/values/colors.xml` e `res/drawable/` definem a aparência inicial antes da aplicação das preferências. Verifique no dispositivo os dois temas, cores de destaque, modal de carga e a transparência durante o descanso.
