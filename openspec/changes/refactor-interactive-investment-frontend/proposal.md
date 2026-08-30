## Why

As telas atuais expõem identificadores internos, pressupõem uma carteira com ID fixo e apresentam erros globais repetidos, o que pode deixar a interface vazia mesmo quando o usuário possui dados válidos. O produto precisa orientar investidores iniciantes por nomes e próximos passos, preservando dados reais e erros contextualizados.

## What Changes

- Substituir a navegação fixa do dashboard por seleção e persistência de uma carteira válida, com onboarding quando não houver carteiras.
- Separar as páginas de dashboard, carteiras, corretoras e ações em módulos próprios e criar apenas componentes reutilizáveis que agreguem comportamento de interface.
- Eliminar notificações HTTP duplicadas com contexto silencioso para fluxos tratados pela tela e deduplicação no serviço de notificações.
- Reconstruir os fluxos de carteira, posição, corretora e ação com labels, validação, estados de carregamento/erro/vazio e ações orientadas ao domínio, sem IDs técnicos.
- Diagnosticar e comunicar corretamente as falhas de validação de corretoras, mantendo as validações e integrações reais do backend.
- Adicionar testes dos fluxos críticos de seleção, navegação, formulários, erro e responsividade.

## Capabilities

### New Capabilities

- `interactive-investment-workflows`: experiência frontend acessível e orientada ao usuário para dashboard, carteiras, posições, corretoras e ações.

### Modified Capabilities

Nenhuma. Os contratos de negócio e validação existentes de carteiras, corretoras e ações permanecem como fonte de verdade; o change altera como o frontend os seleciona, consome e comunica.

## Impact

Afeta rotas Angular, shell de navegação, serviços HTTP e de notificação, páginas e testes em `frontend/src/app`. Poderá exigir diagnóstico e ajustes compatíveis de adaptadores/configuração externa do backend se a investigação da validação de corretora confirmar uma falha na integração, sem contornar a validação financeira.
