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
```

## Responsabilidades por pacote

| Local | Responsabilidade |
| --- | --- |
| `domain/model` | Modelos usados pela UI e pelo timer: exercício, dia de treino, carga, música, trecho e associações. |
| `data/entity`, `data/dao`, `data/relation` | Tabelas, consultas e relações Room. |
| `data/mapper` | Conversão entre modelos de domínio e estruturas de persistência. |
| `data/repository/WorkoutRepository.kt` | Operações de exercícios, músicas, trechos e consulta ao MediaStore. |
| `data/repository/SongArtworkRepository.kt` | Extração assíncrona, redução e cache limitado das capas de áudio. |
| `data/preferences/AppSettingsRepository.kt` | Persiste aparência e expõe as escolhas por `StateFlow`. |
| `data/database/WorkoutDatabase.kt` | Criação do banco, versão e migrações. |
| `ui/screens`, `ui/components` | Telas Compose, ViewModels e componentes visuais. |
| `ui/theme` | Paletas Compose, cor de destaque e formas compartilhadas. |
| `player/MusicPlayerManager.kt` | Reprodução, foco de áudio, prévia de trechos e estado do player. |
| `timer_overlay_service` | Permissão, serviço em primeiro plano e timer sobre outros apps. |
| `util` | Permissão de áudio e formatação de tempo. |

`GymApplication` inicializa `AppContainer` e um `MusicPlayerManager` compartilhado. As telas criam ViewModels por factories, com o repositório e o player como dependências. Os DAOs retornam `Flow`; o repositório converte entidades em modelos; as telas observam o estado dos ViewModels. O rascunho de edição de um refrão (nome, início e volume) fica no `EditSongChorusViewModel`; a UI envia alterações e não modifica diretamente o estado de reprodução compartilhado.

## Dados e unidades

- `ExerciseEntity` guarda nome, carga, unidade, descanso e antecipação musical. `restPeriod` e `chorusDelay` são milissegundos; as telas recebem segundos e convertem antes de salvar.
- `WorkoutDayEntity` guarda o nome do dia. `ExerciseScheduleEntity` associa cada exercício a exatamente um dia e registra sua posição. A lista e a sequência do overlay usam essa ordem. Criar, mover, excluir e ordenar exercícios ocorre em transações Room.
- `SongEntity.id` é o ID do áudio no MediaStore. O banco guarda também o URI, título, artista e duração. O nome pode ser alterado na tela de edição do refrão e fica salvo no Room como nome de exibição do Gym Timer; isso não altera os metadados nem o arquivo no MediaStore.
- `ChorusEntity` guarda o início do trecho em milissegundos e volume opcional. `ExerciseChorusEntity` representa a relação muitos para muitos entre exercícios e trechos.
- As chaves estrangeiras dos trechos e das associações usam exclusão em cascata.

O banco está na versão 8 e exporta snapshots em `app/schemas/`. Existem migrações automáticas de 1 a 7; a migração manual 7 → 8 cria o “Dia 1” e associa todos os exercícios anteriores a ele, ordenados por ID. Não há migração destrutiva. Toda mudança de esquema deve incluir uma migração válida e o novo snapshot.

## Fluxos principais

As escolhas de aparência ficam em `AppSettingsRepository`, fora do banco Room. `MainActivity` observa o estado pelo `SettingsViewModel` e recompõe o `GymTimerTheme`; a tela de ajustes altera as preferências pelo mesmo ViewModel. O serviço de overlay observa esse estado e reaplica cores aos layouts XML enquanto está aberto. Novas preferências devem entrar no modelo `AppSettings` e no repositório, mantendo a tela responsável apenas por interação e apresentação.

1. **Exercícios e dias:** `ExerciseListScreen` observa os dias e pede ao ViewModel a lista do dia selecionado. O repositório consulta o Room por dia; enquanto aguarda a primeira emissão, a tela usa o componente compartilhado `LoadingScreen`. Trocar de dia cancela a coleta anterior para não exibir exercícios do dia anterior. As telas de criação e edição recebem o dia selecionado e persistem por `WorkoutRepository`. A seleção de refrões aparece numa lista compartilhada nas duas telas e fica como rascunho no ViewModel; **Salvar** persiste as associações com o exercício e **Cancelar** descarta o rascunho. Ao editar, **Salvar** substitui as associações em uma transação Room. Arrastar diretamente a alça de seis pontos do cartão salva a nova ordem no dia.
2. **Músicas e trechos:** `LocalSongsScreen` solicita acesso a áudio, lista músicas do MediaStore e salva seleções no Room. `SavedSongsScreen` permite acessar músicas salvas; `EditSongChorusScreen` renomeia o nome de exibição salvo e edita os trechos. Exercícios podem associar vários trechos.
   A consulta ao MediaStore lê duração e ID do álbum numa passagem única. `SongArtworkRepository` tenta carregar miniaturas do álbum no Android 10 ou superior e usa `MediaMetadataRetriever` como fallback e em versões anteriores. Durante a rolagem, carrega uma prévia de 96 px; quando a lista para, troca pela versão de 256 px. O cache compartilha a capa entre faixas do mesmo álbum. A leitura ocorre fora da thread principal, com cancelamento, concorrência limitada e cache em memória com limite de tamanho. `SongCover` recebe o repositório e cuida apenas da apresentação da capa.
3. **Treino:** ao abrir o overlay, `MainActivity` verifica `SYSTEM_ALERT_WINDOW` e inicia `OverlayService`. O ID selecionado é preservado durante a solicitação da permissão. `FloatTimerController` captura a ordem do dia a partir do exercício escolhido. O estado alterna entre `READY_TO_START`, `ON_SET`, `FINISH_PROMPT` e `RESTING`. Ao encerrar cada série, `FinishSetDialogController` abre uma janela editável sobre outros apps; pode salvar uma nova carga padrão para o exercício atual e manter ou avançar. Não há histórico por série. Manter inicia o descanso atual; avançar inicia o descanso do próximo exercício. Fora do descanso, a largura do overlay acompanha o conteúdo até 280 dp, respeitando 16 dp de margem nas laterais; o nome pode ocupar até duas linhas. Só durante a contagem do descanso o fundo do overlay tem 30% de opacidade. Nesse estado, a janela se ajusta à contagem e oculta o nome do exercício; ao terminar, volta ao tamanho normal. No último, concluir encerra o overlay. O serviço mantém uma notificação em primeiro plano.

## Pontos de atenção conhecidos

- A sequência de exercícios é uma cópia tirada quando o overlay abre. Mudanças de ordem feitas enquanto ele está ativo valem somente para a próxima sessão.
- O controlador flutuante carrega dados de modo assíncrono. Revise a disponibilidade dos dados e o cancelamento da coleta ao alterar o ciclo de vida do overlay.
- `MusicPlayerManager` altera o volume do fluxo de mídia do dispositivo e o restaura ao parar. Qualquer mudança no player deve verificar foco de áudio, volume e liberação do `MediaPlayer`.
