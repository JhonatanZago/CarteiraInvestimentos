# Frontend reformulado — Carteira+

Esta entrega altera somente o frontend Angular. O backend, os endpoints e as regras de negócio existentes foram preservados.

## O que foi melhorado

### Minhas carteiras

- Nova organização em duas áreas: lista de carteiras e detalhes da carteira selecionada.
- Cards com total investido, valor atual, resultado e quantidade de ativos.
- Botão **Ver visão geral** com hierarquia visual adequada.
- Criação e edição de carteira em formulário contextual.
- Estado vazio com orientação clara para criar a primeira carteira.
- Campos de posição não começam mais com `0`.
- Ação e corretora agora são escolhidas pelo nome em listas, sem exigir IDs numéricos.
- Quando falta ação ou corretora, a interface explica o pré-requisito e apresenta o atalho correto.
- Posições aparecem em uma lista financeira com ticker, quantidade, valores e resultado.

### Corretoras

- Formulário dividido em campos identificados, com exemplos e textos auxiliares.
- Máscaras visuais para CNPJ e CEP; a API continua recebendo somente números.
- Explicação de que o nome da instituição é obtido pelo sistema.
- Cards de instituição com nome, CNPJ, localização e situação de validação.
- Busca local por nome ou CNPJ.
- Resumo com quantidade cadastrada e validada.

### Ações

- Cadastro guiado por ticker e mercado, com exemplos fáceis de entender.
- Ticker convertido automaticamente para letras maiúsculas.
- Cards com empresa, mercado, cotação, atualização e acesso ao histórico.
- Busca por ticker ou nome da empresa.
- Feedback específico ao atualizar uma cotação.

### Visão geral e mensagens

- Removido o acesso fixo a `/dashboard/1`, que causava erro quando a carteira 1 não existia.
- O menu **Visão geral** agora direciona para a escolha de uma carteira válida.
- O dashboard é aberto a partir da carteira selecionada, usando seu ID real.
- Mensagens idênticas não são mais exibidas várias vezes ao mesmo tempo.
- Mantido o dashboard financeiro já existente, com indicadores, composição e estados de carregamento.

### Interface e responsividade

- Visual inspirado em plataforma de investimentos: roxo escuro, cartões financeiros, indicadores e hierarquia clara.
- Estados vazios mais explicativos e botões com ações objetivas.
- Layout responsivo para desktop, tablet e celular.
- Foco visível e navegação por teclado nos principais controles.

## Arquivos principais

- `frontend/src/app/features/investment-pages.ts`
- `frontend/src/investment-pages.scss`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/app.html`
- `frontend/src/app/core/feedback/notification.service.ts`

## Como executar

O projeto enviado continha dependências instaladas no Windows. Não reutilize essa pasta `node_modules` em Linux, WSL ou Docker.

```bash
cd frontend
rm -rf node_modules
npm ci
npm start
```

Mantenha o backend ativo na porta `8081`. O Angular usa o proxy já configurado para encaminhar `/api/v1` ao backend.

Abra:

```text
http://localhost:4200/carteiras
```

Fluxo recomendado para conferir a interface:

1. Cadastre uma ação.
2. Cadastre uma corretora.
3. Crie ou selecione uma carteira.
4. Adicione uma posição escolhendo a ação e a corretora pelo nome.
5. Clique em **Ver visão geral**.

## Validação realizada

- TypeScript: aprovado com `tsc --noEmit`.
- Templates Angular: aprovados com o compilador Angular (`ngc`).
- O bundle local não foi gerado neste ambiente porque o ZIP continha o binário Windows do esbuild. Após executar `npm ci` no sistema onde o projeto será rodado, o Angular instalará o binário correto.
