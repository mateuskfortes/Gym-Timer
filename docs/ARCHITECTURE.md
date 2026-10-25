# Arquitetura

## Visão geral

O projeto tem um módulo Android (`:app`). A interface principal é Compose; `MainScreenViewModel` mantém o destino atual e o dia selecionado, e `MainScreen` só apresenta o estado e encaminha ações. A sobreposição usa um `Service`, `WindowManager` e View Binding de layouts XML. A persistência é local com Room; as músicas disponíveis vêm do `MediaStore`.

```text
MainActivity → MainScreen → telas Compose → ViewModels
                                        ↓
                             WorkoutRepository
                              ↙          ↘
                         DAOs/Room     MediaStore

GymApplication → AppContainer → WorkoutRepository
                           ↘ AppSettingsRepository → SharedPreferences
               → MusicPlayerManager → MediaPlayer

MainActivity → OverlayService → FloatTimerController
                                ↘ repositório + player

MainActivity → RunScreen → RunViewModel → RunRepository → RunDao/Room
                                      ↘ RunTrackingService → GPS
                                      ↘ RunMap → blocos OpenStreetMap
```

## Responsabilidades por pacote

| Local | Responsabilidade |
| --- | --- |
| `domain/model` | Modelos usados pela UI e pelo timer: exercício, dia de treino, carga, música, trecho e associações. |
| `data/entity`, `data/dao`, `data/relation` | Tabelas, consultas e relações Room. |
| `data/mapper` | Conversão entre modelos de domínio e estruturas de persistência. |
| `data/repository/WorkoutRepository.kt` | Operações de exercícios, músicas, trechos e consulta ao MediaStore. |
| `data/repository/RunRepository.kt` | Sessões de corrida, pontos GPS e métricas persistidas no Room. |
| `data/repository/SongArtworkRepository.kt` | Extração assíncrona, redução e cache limitado das capas de áudio. |
| `data/preferences/AppSettingsRepository.kt` | Persiste aparência e expõe as escolhas por `StateFlow`. |
| `data/database/WorkoutDatabase.kt` | Criação do banco, versão e migrações. |
| `ui/screens`, `ui/components` | Telas Compose, ViewModels e componentes visuais. |
| `ui/theme` | Paletas Compose, cor de destaque e formas compartilhadas. |
| `player/MusicPlayerManager.kt` | Reprodução, foco de áudio, prévia de trechos e estado do player. |
| `timer_overlay_service` | Permissão, serviço em primeiro plano e timer sobre outros apps. |
| `ui/screens/runs` | Tela, ViewModel, mapa e serviço de gravação de corridas. |
| `util` | Permissão de áudio e formatação de tempo. |

`GymApplication` inicializa `AppContainer` e um `MusicPlayerManager` compartilhado. As telas criam ViewModels por factories, com o repositório e o player como dependências. Os DAOs retornam `Flow`; o repositório converte entidades em modelos; as telas observam o estado dos ViewModels. O rascunho de edição de um refrão (nome, início e volume) fica no `EditSongChorusViewModel`; a UI envia alterações e não modifica diretamente o estado de reprodução compartilhado.

## Dados e unidades

- `ExerciseEntity` guarda nome, carga, unidade, descanso e antecipação musical. `restPeriod` e `chorusDelay` são milissegundos; as telas recebem segundos e convertem antes de salvar.
- `WorkoutDayEntity` guarda o nome do dia. `ExerciseScheduleEntity` associa cada exercício a exatamente um dia e registra sua posição. A lista e a sequência do overlay usam essa ordem. Criar, mover, excluir e ordenar exercícios ocorre em transações Room.
- `SongEntity.id` é o ID do áudio no MediaStore. O banco guarda também o URI, título, artista e duração. O nome pode ser alterado na tela de edição do refrão e fica salvo no Room como nome de exibição do Gym Timer; isso não altera os metadados nem o arquivo no MediaStore.
- `ChorusEntity` guarda o início do trecho em milissegundos e volume opcional. `ExerciseChorusEntity` representa a relação muitos para muitos entre exercícios e trechos.
- As chaves estrangeiras dos trechos e das associações usam exclusão em cascata.

O banco está na versão 10 e exporta snapshots em `app/schemas/`. Existem migrações automáticas de 1 a 7; a migração manual 7 → 8 cria o “Dia 1” e associa todos os exercícios anteriores a ele, ordenados por ID. A migração manual 8 → 9 cria `runs` e `run_points`, com exclusão em cascata dos pontos ao remover uma corrida. A migração manual 9 → 10 acrescenta a coluna nullable `runs.title`; títulos antigos ficam nulos e os percursos permanecem intactos. Não há migração destrutiva. Toda mudança de esquema deve incluir uma migração válida e o novo snapshot.

## Fluxos principais

As escolhas de aparência ficam em `AppSettingsRepository`, fora do banco Room. `MainActivity` observa o estado pelo `SettingsViewModel` e recompõe o `GymTimerTheme`; a tela de ajustes altera as preferências pelo mesmo ViewModel. O serviço de overlay observa esse estado e reaplica cores aos layouts XML enquanto está aberto. Novas preferências devem entrar no modelo `AppSettings` e no repositório, mantendo a tela responsável apenas por interação e apresentação.

1. **Exercícios e dias:** `ExerciseListScreen` observa os dias e pede ao ViewModel a lista do dia selecionado. O repositório consulta o Room por dia; enquanto aguarda a primeira emissão, a tela usa o componente compartilhado `LoadingScreen`. Trocar de dia cancela a coleta anterior para não exibir exercícios do dia anterior. As telas de criação e edição recebem o dia selecionado e persistem por `WorkoutRepository`. A seleção de refrões aparece numa lista compartilhada nas duas telas e fica como rascunho no ViewModel; **Salvar** persiste as associações com o exercício e **Cancelar** descarta o rascunho. Ao editar, **Salvar** substitui as associações em uma transação Room. Arrastar diretamente a alça de seis pontos do cartão salva a nova ordem no dia.
2. **Músicas e trechos:** a aba abre `MusicHomeScreen`, que organiza destinos em cartões; atualmente, **Biblioteca de músicas** abre `SavedSongsScreen`, onde a pessoa gerencia as músicas já escolhidas. Dali, `LocalSongsScreen` solicita acesso a áudio, lista músicas do MediaStore e salva novas seleções no Room. Os botões de reprodução nas telas de músicas, refrões e exercícios passam pelo pedido compartilhado de permissão; após a concessão, a ação solicitada é executada. `MusicPlayerManager` também verifica a permissão antes de abrir o arquivo, usa `prepareAsync()` e libera o player se a preparação falhar. `EditSongChorusScreen` renomeia os dados de exibição e edita os trechos. Exercícios podem associar vários trechos.
   A consulta ao MediaStore lê duração e ID do álbum numa passagem única. `SongArtworkRepository` tenta carregar miniaturas do álbum no Android 10 ou superior e usa `MediaMetadataRetriever` como fallback e em versões anteriores. Durante a rolagem, carrega uma prévia de 96 px; quando a lista para, troca pela versão de 256 px. O cache compartilha a capa entre faixas do mesmo álbum. A leitura ocorre fora da thread principal, com cancelamento, concorrência limitada e cache em memória com limite de tamanho. `SongCover` recebe o repositório e cuida apenas da apresentação da capa.
3. **Treino:** ao abrir o overlay, `MainActivity` verifica `SYSTEM_ALERT_WINDOW` e inicia `OverlayService`. O ID selecionado é preservado durante a solicitação da permissão. `FloatTimerController` captura a ordem do dia a partir do exercício escolhido. O estado alterna entre `READY_TO_START`, `ON_SET`, `FINISH_PROMPT` e `RESTING`. Ao encerrar cada série, `FinishSetDialogController` abre uma janela editável sobre outros apps; pode salvar uma nova carga padrão para o exercício atual e manter ou avançar. Não há histórico por série. Manter inicia o descanso atual; avançar inicia o descanso do próximo exercício. Fora do descanso, a largura do overlay acompanha o conteúdo até 280 dp, respeitando 16 dp de margem nas laterais; o nome pode ocupar até duas linhas. Só durante a contagem do descanso o fundo do overlay tem 30% de opacidade. Nesse estado, a janela se ajusta à contagem e oculta o nome do exercício; ao terminar, volta ao tamanho normal. No último, concluir encerra o overlay. O serviço mantém uma notificação em primeiro plano.
4. **Corridas:** `RunScreen` seleciona entre `history/RunHomeScreen`, `history/RunDetailsScreen` e `active/ActiveRunScreen`, com navegação mantida em `RunViewModel`. Iniciar abre a sessão ativa em tela cheia, sem a barra de abas. `BottomSheetScaffold` mantém um painel inferior que abre expandido e pode ser recolhido por gesto ou botão, preservando o tempo no cabeçalho. Voltar ao histórico não pausa nem encerra o serviço; **Voltar à corrida** reabre o mapa. Cada componente Compose fica em seu próprio arquivo. Os pacotes `active`, `history`, `components`, `map`, `permissions`, `format`, `notifications` e `tracking` separam apresentação, formatação, permissões e gravação; `RunRepository` grava sessões e pontos no Room. `RunTrackingService` usa `LocationManager` como serviço em primeiro plano do tipo `location`, filtra leituras imprecisas e mantém tempo ativo durante bloqueio da tela. Pausar encerra o trecho atual; retomar inicia outro segmento para que o mapa não desenhe uma linha durante a pausa. Sessões interrompidas pelo sistema ficam disponíveis para retomar ou salvar. O pace médio usa tempo ativo em milissegundos e distância em metros. `RunMap` renderiza a rota sobre blocos online do OpenStreetMap, com atribuição visível; não há pré-carregamento nem mapas offline. A disponibilidade dos blocos segue a política pública do OSM.

## Histórico e notificação de corridas

`history/ActiveRunCard` destaca a sessão aberta com estado, métricas e ação para voltar à corrida. O cartão diferencia gravação, pausa e interrupção. `RunScreen` guarda somente o ID da corrida pendente de confirmação em estado salvável. `RunDeleteDialog` identifica a data da sessão; cancelar ou fechar o modal não altera o banco. Confirmar envia o ID ao `RunViewModel` e ao `RunRepository`. A consulta `RunDao.deleteCompletedRun` aceita apenas `COMPLETED`, protegendo sessões abertas mesmo de chamadas diretas. A chave estrangeira existente remove os pontos em cascata na mesma exclusão. O histórico usa os IDs das corridas como chaves estáveis e `Modifier.animateItem` para apagar suavemente o cartão removido e deslocar os vizinhos para o espaço livre. A animação acompanha a lista emitida pelo Room, sem atrasar a exclusão nem manter uma cópia local do histórico. O título opcional é persistido a partir da versão 10 do banco.

`active/RunFinishDialog` mantém o título opcional como rascunho salvável, com limite compartilhado em `domain/RunTitle`. Salvar normaliza o texto e encaminha título e ID pelo ViewModel ao serviço. `RunDao.finishRun` conclui a sessão em uma atualização condicional e preserva o maior tempo acumulado; repetir o comando não substitui uma corrida já concluída. Históricos e detalhes mostram a data como identificação quando o título está ausente. A lixeira usa `onSurface`, seguindo a cor de ícones do tema.

Descartar abre `RunDiscardDialog`; voltar à edição preserva o título e não altera a corrida. O serviço serializa seus comandos por `Mutex`, bloqueia transições depois de começar o encerramento e aguarda o cancelamento dos trabalhos de duração e pontos antes de salvar ou descartar. `RunDao.discardUnfinishedRun` remove apenas sessões ainda abertas, com cascata do percurso. A finalização do serviço remove a notificação. Sessões interrompidas sem serviço também podem ser salvas ou descartadas pelo ViewModel.

`RunTrackingService` publica a notificação ao iniciar ou retomar, atualiza seu texto ao pausar e a remove ao concluir. O aviso solicita exibição imediata, usa `setOngoing(true)` e abre a Activity por um `PendingIntent` imutável. O Android ainda controla a permissão e a visibilidade do canal. `notifications/RunNotifications` compartilha o identificador do canal com o serviço, consulta bloqueios gerais e específicos e oferece o atalho às configurações. A consulta é refeita quando a tela retorna ao primeiro plano; a gravação não é interrompida por uma negativa de notificações.

## Câmera do mapa de corridas

`map/RunMapRoute` atualiza somente a rota e o marcador. As emissões GPS e as recomposições da tela não redefinem o zoom nem repetem o enquadramento inicial. `RunMapCameraState` contém as regras sem dependência do SDK; `RunMapCameraController` executa as decisões no MapLibre e registra os listeners de movimento e de câmera parada. Esses listeners são removidos antes de destruir o `MapView`. O `AndroidView` inicializa o SDK e publica o mapa depois de carregar o estilo. Um `LaunchedEffect` observa explicitamente esse mapa, seu tamanho, os pontos e a localização inicial; somente depois do estilo e da medição aplica os dados e a câmera. Isso garante que uma localização recebida após o callback inicial também seja aplicada, sem depender de leituras de estado em callbacks assíncronos do SDK.

O primeiro posicionamento define o zoom da sessão ativa ou enquadra o percurso do histórico. Durante a gravação, novas coordenadas podem mover o centro, preservando o zoom, a rotação e a inclinação. Um gesto suspende os movimentos automáticos enquanto estiver em andamento. Ao terminar, se a posição permanecer no centro, o acompanhamento continua com o zoom escolhido. Se o usuário estiver explorando outra área, o mapa fica nessa área e apenas a rota e o marcador continuam sendo atualizados.

A centralização compara a projeção em pixels da posição com o alvo da câmera, usando tolerância visual de 16 dp e distância geográfica máxima de 25 metros para evitar oscilações por pequenos desvios. As duas condições precisam ser satisfeitas; a visão global não pode ocultar o botão só porque uma grande distância ocupa poucos pixels. O centro corresponde à área útil acima do painel inferior. O botão **Centralizar na minha localização** aparece na tela ativa somente quando existe uma posição fora dessa tolerância. Tocá-lo restaura o zoom de acompanhamento (17), centraliza a posição na área útil acima do painel e reativa o acompanhamento. As atualizações GPS posteriores preservam a escala escolhida em novos gestos. Alterações do painel também preservam o zoom e não reativam o acompanhamento suspenso por um gesto.

O estado da câmera pertence à instância do mapa e não é gravado no Room. Abrir outra tela de mapa faz um novo enquadramento inicial. A câmera usa o último ponto da sessão ou a localização inicial já obtida, sem criar leituras adicionais para o percurso. No histórico, o enquadramento é aplicado uma vez e a navegação posterior fica livre. O zoom inicial é limitado a 17 para manter o contexto em percursos curtos; coordenadas repetidas são tratadas como uma posição única. A fonte raster declara `maxzoom: 19`, limite dos blocos OSM, permitindo ampliar os blocos existentes acima dessa escala.

O mapa do histórico usa `TextureView`, pois fica dentro de uma coluna com rolagem e recorte do Compose. A superfície padrão do SDK pode carregar e consultar a rota sem compor sua imagem visível nesse contêiner. A corrida em tela cheia mantém a superfície padrão.

## Pontos de atenção conhecidos

O mapa da corrida usa `map/RunMapLocationSource` para centralizar a câmera numa localização recente do aparelho antes do primeiro ponto GPS válido. Essa leitura aceita GPS ou rede e não entra no percurso nem no cálculo da distância. Os pedidos são cancelados quando a tela sai do primeiro plano ou quando a corrida já tem pontos. Sem uma posição disponível, a tela informa que aguarda a localização em vez de exibir o mapa global. Corridas salvas continuam enquadrando seu próprio percurso. Na tela ativa, a câmera e a atribuição OSM reservam o espaço do painel para manter a posição visível quando ele está aberto ou recolhido.

- A sequência de exercícios é uma cópia tirada quando o overlay abre. Mudanças de ordem feitas enquanto ele está ativo valem somente para a próxima sessão.
- O controlador flutuante carrega dados de modo assíncrono. Revise a disponibilidade dos dados e o cancelamento da coleta ao alterar o ciclo de vida do overlay.
- `MusicPlayerManager` altera o volume do fluxo de mídia do dispositivo e o restaura ao parar. Qualquer mudança no player deve verificar foco de áudio, volume e liberação do `MediaPlayer`.
