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
```

O último comando requer dispositivo ou emulador conectado. A configuração Gradle atual lê `key.properties` na configuração do módulo; prepare a assinatura local conforme o `README.md` antes dos comandos. `lintDevDebug` é fornecido pelo plugin Android Gradle e grava relatórios em `app/build/reports/lint-results-devDebug.html` e `.txt`. O projeto não usa um formatador automático.

Para compilar, instalar e abrir o aplicativo em um celular conectado por USB ou Wi-Fi, rode `./run-app.sh`. A [seção de conexão no README](../README.md#rodar-no-celular-por-usb-ou-wi-fi) explica a configuração do aparelho e o uso do depurador do Android Studio. Compilar com `assembleDevDebug` sozinho não instala nem inicia o app.

`WorkoutSequenceTest` cobre o avanço na ordem de uma sessão; `TimeInputTest` cobre a conversão e a rejeição de valores inválidos. `WorkoutMigrationTest` cria um banco da versão 7 com exercícios e verifica a preservação dos registros e sua associação ao “Dia 1” após migrar para a versão 8. `WorkoutScheduleTest` cobre ordem, consulta por dia, mudança de dia, carga e restrições de exclusão. Execute `connectedDevDebugAndroidTest` com um aparelho conectado para validar as operações Room.

## Verificação manual dos fluxos Android

| Mudança | Verificação mínima |
| --- | --- |
| Exercícios ou Room | Criar e renomear dias; criar, editar, mover, ordenar e excluir exercícios; reabrir o app e conferir ordem, persistência e associação de trechos. Tentar excluir um dia com exercícios deve ser impedido. |
| Músicas ou player | Conceder/recusar permissão de áudio; listar, salvar, reproduzir, pausar e excluir música/trecho; conferir volume ao terminar. |
| Overlay | Abrir após permissão já concedida e após concedê-la nas configurações; tocar para iniciar/encerrar série; editar carga no painel sobre outro app; testar **Manter**, **Próximo** e **Concluir treino**; conferir descanso do exercício escolhido, transparência, arrastar/fechar e notificação. |
| Migração Room | Atualizar instalação com dados da versão anterior; conferir que os registros sobrevivem. |

## Convenções

- Siga a organização e os nomes já usados nos pacotes. Prefira classes e funções pequenas, com uma responsabilidade clara, a comentários que expliquem lógica difícil de seguir.
- Coloque textos novos de interface em `app/src/main/res/values/strings.xml`. Mantenha a interface em português.
- Não bloqueie a thread principal com MediaStore, Room ou preparação de áudio. Observe `Flow` no ciclo de vida adequado e cancele trabalho iniciado por componentes que terminam.
- Acrescente testes para regras novas ou defeitos corrigidos, especialmente validação, conversão de unidades, transições do timer e persistência. Testes devem verificar comportamento, sem repetir a implementação.
- Evite dependências e camadas novas sem necessidade demonstrada. Se uma refatoração ampla for necessária, faça-a em passos verificáveis.
- Nunca versione keystores, `key.properties`, `local.properties` ou credenciais. Esses arquivos já são ignorados pelo Git.
