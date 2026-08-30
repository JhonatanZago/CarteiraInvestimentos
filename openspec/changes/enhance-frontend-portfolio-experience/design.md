## Context

O dashboard Angular atual junta a consulta principal da carteira com indicadores, evolução e proventos em uma única agregação reativa. Como uma falha encerra a agregação inteira, a tela não diferencia erro principal de indisponibilidade suplementar. A navegação e os estilos existentes são funcionais, mas não oferecem o seletor, a hierarquia visual e os estados definidos pela nova capacidade. Consulte `proposal.md` e `specs/portfolio-dashboard-experience/spec.md` para o escopo comportamental.

## Goals / Non-Goals

**Goals:**

- Carregar o resumo financeiro como dado obrigatório e tratar dados suplementares de forma independente.
- Preservar o contrato atual da API e manter o backend como fonte de verdade financeira.
- Criar uma experiência responsiva consistente sem dependências adicionais de visualização.

**Non-Goals:**

- Alterar cálculos, persistência, endpoints ou integrações externas do backend.
- Criar indicadores financeiros sintéticos quando a API não os retornar.
- Introduzir uma biblioteca de gráficos ou um serviço de terceiros no navegador.

## Decisions

### Separate primary and supplementary request outcomes

O carregamento continuará agregado para permitir uma atualização coesa, mas cada consulta suplementar converterá seu próprio erro em um valor de indisponibilidade/coleção vazia. A consulta do resumo será a única que determina o erro principal e habilita a tentativa novamente. Isso evita perder um resumo válido e reduz estados concorrentes na interface.

Alternativa considerada: carregar cada cartão com assinaturas independentes. Ela permitiria atualização isolada, mas aumenta a coordenação de loading, risco de renderização inconsistente e complexidade sem benefício para o escopo atual.

### Drive selection from route and portfolio list

O componente obterá a lista de carteiras pelo serviço existente, usará o identificador da rota como seleção atual e navegará para a rota da carteira escolhida antes de recarregar. Uma falha na listagem não impedirá o dashboard da rota atual; ela apenas torna o seletor indisponível.

Alternativa considerada: manter a seleção somente em estado local. Essa opção quebra links diretos, atualização da página e navegação previsível.

### Use native SVG and CSS for visualizations

A evolução será convertida em pontos SVG proporcionais aos valores retornados, e a alocação será exibida por segmentos CSS calculados dos valores atuais. Ambos terão fallback textual/estado vazio e atributos acessíveis.

Alternativa considerada: instalar uma biblioteca de gráficos. Ela entregaria mais tipos de gráfico, mas adiciona peso e manutenção para duas visualizações simples.

### Centralize responsive presentation in existing shell and styles

O shell da aplicação ganhará uma navegação lateral em telas amplas e menu recolhível em telas pequenas. Tokens no stylesheet global serão usados pelos cards, avisos, skeletons e tabela com contêiner de rolagem horizontal, evitando estilos isolados e divergentes.

## Risks / Trade-offs

- [Respostas suplementares vazias podem parecer ausência de dados reais] → Usar mensagens distintas para indisponibilidade e histórico inexistente.
- [A lista de carteiras pode falhar enquanto uma rota válida funciona] → Manter o resumo da rota e comunicar somente a limitação do seletor.
- [Escalas com todos os valores iguais ou um único ponto podem degenerar] → Normalizar a amplitude mínima e definir uma posição central para ponto único.
- [Mudanças na marcação podem afetar testes existentes] → Atualizar testes por comportamento, priorizando rótulos acessíveis e estados observáveis.

## Migration Plan

1. Implementar os serviços/contratos ausentes e o carregamento tolerante a falhas atrás das rotas existentes.
2. Atualizar o shell e os estilos, preservando todos os destinos de navegação atuais.
3. Adicionar testes de carga, falha principal e falha parcial; executar build de produção e a suíte frontend.
4. Fazer rollback revertendo os arquivos Angular e de estilo deste change; não há migração de dados ou alteração de API para desfazer.
