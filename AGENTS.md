# Instruções para agentes

Este arquivo vale para todo o repositório. Leia `README.md`, `docs/ARCHITECTURE.md` e `docs/DEVELOPMENT.md` antes de alterar código.

## Objetivo e organização

- O Gym Timer 2 é um aplicativo Android em Kotlin. A interface principal usa Jetpack Compose; o timer flutuante usa um `Service` e layouts XML.
- Preserve a direção das dependências: `ui` e `timer_overlay_service` consomem modelos de `domain` e operações de `data`; entidades Room e consultas SQL ficam em `data`.
- `GymApplication` cria `AppContainer` e `MusicPlayerManager`. As telas recebem essas dependências por factories de ViewModel; siga esse padrão enquanto não houver uma mudança arquitetural deliberada.
- Mantenha nomes de tipos, funções e arquivos em inglês, como no código atual. Textos exibidos ao usuário devem ficar em recursos `strings.xml` quando novos ou alterados.

## Ao implementar

- Faça a menor alteração que resolva o comportamento pedido. Separe leitura de dados, regras de estado e apresentação quando isso reduzir acoplamento real; evite abstrações sem uso concreto.
- Use funções curtas com nomes que expressem a intenção. Evite duplicar validações e conversões de segundos para milissegundos entre telas. Identifique explicitamente a unidade em nomes de propriedades quando houver ambiguidade.
- Exponha estado de ViewModel de forma somente leitura (`StateFlow`/`Flow`); mantenha mutações e operações assíncronas dentro do ViewModel ou da camada de dados.
- Não faça consultas ao MediaStore, operações Room ou preparação de mídia na thread principal. Libere player, timer, listeners e corrotinas quando o componente terminar.
- Trate permissões de áudio e sobreposição nos fluxos que dependem delas. Verifique o comportamento em versões Android suportadas pelo projeto (`minSdk 24`, `targetSdk 36`).
- Em alterações de esquema Room, aumente a versão, forneça migração que preserve dados e inclua o novo JSON em `app/schemas/`. Não use migração destrutiva como solução para uma mudança normal de esquema.
- Nunca inclua `key.properties`, `local.properties`, chaves de assinatura ou senhas no Git, em exemplos ou em saídas de ferramentas.

## Verificação e entrega

- Para código Kotlin, execute ao menos `./gradlew :app:assembleDevDebug :app:testDevDebugUnitTest :app:lintDevDebug` quando o ambiente permitir. Para fluxos de permissão, áudio e overlay, descreva também a verificação manual em dispositivo/emulador.
- Acrescente testes de comportamento para regras novas ou bugs corrigidos quando forem testáveis; os testes atuais são apenas exemplos gerados pelo template.
- Atualize a documentação quando mudar arquitetura, requisitos de execução, permissões ou persistência.
- Relate o que mudou, como verificou e qualquer limitação concreta. Não declare que um fluxo Android foi testado sem executá-lo.
