# Arquitetura

## Visão geral

O projeto tem um módulo Android (`:app`). A interface principal é Compose, com navegação controlada por `MainScreen` e seus índices de tela. A sobreposição usa um `Service`, `WindowManager` e View Binding de layouts XML. A persistência é local com Room; as músicas disponíveis vêm do `MediaStore`.

```text
MainActivity → MainScreen → telas Compose → ViewModels
                                        ↓
                             WorkoutRepository
                              ↙          ↘
                         DAOs/Room     MediaStore

GymApplication → AppContainer → WorkoutRepository
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
| `data/database/WorkoutDatabase.kt` | Criação do banco, versão e migrações. |
| `ui/screens`, `ui/components` | Telas Compose, ViewModels e componentes visuais. |
| `player/MusicPlayerManager.kt` | Reprodução, foco de áudio, prévia de trechos e estado do player. |
| `timer_overlay_service` | Permissão, serviço em primeiro plano e timer sobre outros apps. |
| `util` | Permissão de áudio e formatação de tempo. |

`GymApplication` inicializa `AppContainer` e um `MusicPlayerManager` compartilhado. As telas criam ViewModels por factories, com o repositório e o player como dependências. Os DAOs retornam `Flow`; o repositório converte entidades em modelos; as telas observam o estado dos ViewModels.

## Dados e unidades

- `ExerciseEntity` guarda nome, carga, unidade, descanso e antecipação musical. `restPeriod` e `chorusDelay` são milissegundos; as telas recebem segundos e convertem antes de salvar.
- `WorkoutDayEntity` guarda o nome do dia. `ExerciseScheduleEntity` associa cada exercício a exatamente um dia e registra sua posição. A lista e a sequência do overlay usam essa ordem. Criar, mover, excluir e ordenar exercícios ocorre em transações Room.
- `SongEntity.id` é o ID do áudio no MediaStore. O banco guarda também o URI, título, artista e duração. O arquivo de áudio permanece no dispositivo.
- `ChorusEntity` guarda o início do trecho em milissegundos e volume opcional. `ExerciseChorusEntity` representa a relação muitos para muitos entre exercícios e trechos.
- As chaves estrangeiras dos trechos e das associações usam exclusão em cascata.

O banco está na versão 8 e exporta snapshots em `app/schemas/`. Existem migrações automáticas de 1 a 7; a migração manual 7 → 8 cria o “Dia 1” e associa todos os exercícios anteriores a ele, ordenados por ID. Não há migração destrutiva. Toda mudança de esquema deve incluir uma migração válida e o novo snapshot.

## Fluxos principais

1. **Exercícios e dias:** `ExerciseListScreen` observa os dias e pede ao ViewModel a lista do dia selecionado. O repositório consulta o Room por dia; enquanto aguarda a primeira emissão, a tela usa o componente compartilhado `LoadingScreen`. Trocar de dia cancela a coleta anterior para não exibir exercícios do dia anterior. As telas de criação e edição recebem o dia selecionado e persistem por `WorkoutRepository`. Arrastar diretamente a alça de seis pontos do cartão salva a nova ordem no dia.
2. **Músicas e trechos:** `LocalSongsScreen` solicita acesso a áudio, lista músicas do MediaStore e salva seleções no Room. `SavedSongsScreen` permite acessar músicas salvas; `EditSongChorusScreen` edita os trechos. Exercícios podem associar vários trechos.
3. **Treino:** ao abrir o overlay, `MainActivity` verifica `SYSTEM_ALERT_WINDOW` e inicia `OverlayService`. O ID selecionado é preservado durante a solicitação da permissão. `FloatTimerController` captura a ordem do dia a partir do exercício escolhido. O estado alterna entre `READY_TO_START`, `ON_SET`, `FINISH_PROMPT` e `RESTING`. Ao encerrar cada série, `FinishSetDialogController` abre uma janela editável sobre outros apps; pode salvar uma nova carga padrão para o exercício atual e manter ou avançar. Não há histórico por série. Manter inicia o descanso atual; avançar inicia o descanso do próximo exercício. Durante o descanso, a janela se ajusta à contagem e oculta o nome do exercício; ao terminar, volta ao tamanho normal. No último, concluir encerra o overlay. O serviço mantém uma notificação em primeiro plano.

## Pontos de atenção conhecidos

- A sequência de exercícios é uma cópia tirada quando o overlay abre. Mudanças de ordem feitas enquanto ele está ativo valem somente para a próxima sessão.
- O controlador flutuante carrega dados de modo assíncrono. Revise a disponibilidade dos dados e o cancelamento da coleta ao alterar o ciclo de vida do overlay.
- `MusicPlayerManager` altera o volume do fluxo de mídia do dispositivo e o restaura ao parar. Qualquer mudança no player deve verificar foco de áudio, volume e liberação do `MediaPlayer`.
