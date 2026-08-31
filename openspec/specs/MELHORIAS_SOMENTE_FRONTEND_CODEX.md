# Prompt para o Codex — Melhorias Visuais e Correções Pontuais do Frontend

## Objetivo

Melhore somente o frontend Angular existente da aplicação Carteira de Investimentos.

O trabalho deve se limitar a:

1. tornar as telas mais bonitas, modernas e interativas;
2. facilitar o entendimento dos formulários;
3. corrigir os erros visíveis nas telas enviadas;
4. melhorar mensagens, estados vazios e feedbacks;
5. manter as funcionalidades e regras de negócio atuais.

Não ampliar o projeto e não reescrever o backend.

---

# 1. Fora do escopo

Não realizar os seguintes trabalhos:

- não criar um novo sistema de contexto de carteira;
- não criar uma nova arquitetura de rotas;
- não extrair todas as páginas para componentes standalone novos;
- não reorganizar todo o projeto;
- não implementar um CRUD novo ou completo;
- não alterar regras de negócio de carteiras ou posições;
- não alterar adapters externos;
- não criar testes dos adapters externos;
- não alterar integrações da BrasilAPI, ViaCEP, CVM ou APIs de cotação;
- não criar smoke tests reais para desktop, tablet e mobile;
- não adicionar autenticação;
- não adicionar novas entidades;
- não criar funcionalidades que o backend atual não suporta;
- não inventar dados financeiros;
- não instalar bibliotecas grandes desnecessárias.

Preservar os serviços, endpoints, DTOs, models e regras existentes.

---

# 2. Problemas que realmente precisam ser corrigidos

## Visão geral

- o menu tenta abrir `/dashboard/1`, mas a carteira de ID `1` pode não existir;
- aparecem três mensagens iguais de “registro não encontrado”;
- a tela fica praticamente vazia quando a carteira não existe;
- a mensagem menciona backend e porta `8081`, linguagem inadequada para cliente;
- o seletor de carteira não ajuda quando não existe carteira válida;
- falta uma próxima ação clara.

## Minhas carteiras

- os campos de posição começam preenchidos com `0`;
- o cliente precisa informar `ID ação` e `ID corretora`;
- não existem labels explicando os campos;
- o formulário está apertado em uma única linha;
- o link “Abrir dashboard” parece texto comum;
- os botões excluir chamam atenção demais;
- a lista de carteiras parece uma lista de botões soltos;
- o estado “Sem posições” não orienta suficientemente.

## Corretoras

- formulário comprimido em uma única linha;
- placeholders usados como único rótulo;
- cliente não entende quais dados deve informar;
- falta máscara de CNPJ e CEP;
- falta texto explicando o processo;
- botão de cadastrar fica desabilitado sem explicar o motivo;
- busca por CNPJ fica visualmente misturada ao cadastro;
- estado vazio ocupa a tela sem apresentar orientação;
- erro de cadastro não é mostrado próximo ao formulário.

## Ações

- funciona, mas ainda parece um formulário técnico;
- falta explicar o que é ticker;
- falta explicar a diferença entre Brasil e Estados Unidos;
- falta busca e organização visual na lista;
- atualizar cotação poderia ter feedback melhor;
- ações importantes não possuem hierarquia visual.

---

# 3. Direção visual

Manter a identidade atual em roxo e azul-escuro, mas refinar:

```scss
:root {
  --primary: #635bdf;
  --primary-hover: #524acb;
  --primary-soft: #efedff;
  --sidebar: #201f3a;
  --background: #f6f7fb;
  --surface: #ffffff;
  --border: #e4e7f0;
  --text: #22233b;
  --muted: #71738a;
  --success: #07875f;
  --success-soft: #e9f8f2;
  --danger: #c53d4d;
  --danger-soft: #fff0f2;
  --warning: #a56d09;
  --warning-soft: #fff8e6;
}
```

Aplicar:

- fonte `Inter` ou fonte atual do sistema;
- fundo cinza muito claro;
- cards brancos;
- bordas suaves;
- raio de 12 a 16 px;
- sombra discreta;
- títulos com hierarquia clara;
- botões com altura mínima de 44 px;
- labels permanentes;
- textos auxiliares;
- hover e focus;
- espaçamentos consistentes;
- animações curtas e discretas;
- layout responsivo usando media queries simples.

Não alterar a identidade inteira da aplicação.

---

# 4. Melhorias da Visão geral

## Corrigir carteira inválida

Remover o link fixo `/dashboard/1` do menu.

Usar `/carteiras` como destino da opção “Visão geral” quando nenhuma carteira estiver selecionada, ou navegar para o dashboard a partir do botão da carteira selecionada.

Não criar um serviço complexo de contexto. Usar a estrutura atual e a navegação já existente.

## Evitar três mensagens iguais

Antes de adicionar uma notificação, verificar se a mesma mensagem já está visível:

```typescript
show(message: string, tone: Notification['tone'] = 'info'): void {
  const duplicated = this.notifications().some(
    notification => notification.message === message
  );

  if (duplicated) {
    return;
  }

  const item = { id: ++this.nextId, message, tone };
  this.notifications.update(items => [...items, item]);
  window.setTimeout(() => this.dismiss(item.id), 4500);
}
```

O dashboard também deve tratar as chamadas opcionais separadamente. Falha em evolução, indicadores ou proventos não deve apagar o resumo principal.

## Mensagem de carteira inexistente

Substituir:

```text
Verifique se a carteira existe e se o backend está respondendo na porta 8081.
```

por:

```text
Não encontramos esta carteira
Ela pode ter sido removida ou ainda não foi criada.
[Voltar para minhas carteiras]
```

Não mostrar endpoint, backend, porta ou código HTTP ao cliente.

## Aparência

Quando existirem dados, exibir:

- card principal com patrimônio atual;
- total investido;
- resultado acumulado;
- rentabilidade;
- quantidade de ativos;
- gráfico de evolução atual;
- distribuição atual;
- tabela de posições;
- botão de atualizar;
- última atualização.

Quando não houver posições:

```text
Sua carteira ainda não possui investimentos
Adicione sua primeira posição para acompanhar patrimônio e rentabilidade.
[Adicionar posição]
```

---

# 5. Melhorias de Minhas carteiras

## Lista de carteiras

Transformar os botões soltos em cards:

```text
┌─────────────────────────────┐
│ Jhonatan                 ⋮  │
│ Primeira carteira           │
│ 0 posições                  │
│ [Selecionar carteira]       │
└─────────────────────────────┘
```

Requisitos:

- card selecionado com borda roxa;
- descrição visível;
- menu discreto para editar/excluir;
- excluir não deve ser o botão principal;
- hover suave;
- estado selecionado fácil de identificar.

## Formulário de carteira

Manter a funcionalidade atual, mas organizar em card:

```text
Nome da carteira
[Ex.: Carteira de longo prazo]

Descrição
[Ex.: Investimentos para aposentadoria]

[Cancelar] [Criar carteira]
```

Se estiver editando, mostrar título “Editar carteira” e botão “Salvar alterações”.

## Botão Abrir dashboard

Substituir o link simples por botão:

```html
<a class="dashboard-action" [routerLink]="['/dashboard', selected.id]">
  <span>
    <strong>Abrir visão geral</strong>
    <small>Ver patrimônio e resultados</small>
  </span>
  <span aria-hidden="true">→</span>
</a>
```

O botão deve:

- possuir fundo roxo suave ou roxo principal;
- ter ícone de gráfico ou seta;
- ter altura mínima de 48 px;
- ficar ao lado do nome da carteira;
- ter hover e foco visível;
- não parecer link sublinhado comum.

## Remover zeros dos campos

Trocar valores iniciais `0` por `null`:

```typescript
readonly positionForm = this.fb.group({
  acaoId: this.fb.control<number | null>(null, Validators.required),
  corretoraId: this.fb.control<number | null>(null, Validators.required),
  quantidade: this.fb.control<number | null>(null, [
    Validators.required,
    Validators.min(0.000001)
  ]),
  precoMedio: this.fb.control<number | null>(null, [
    Validators.required,
    Validators.min(0.01)
  ]),
  dataPrimeiraCompra: this.fb.control<string | null>(null, Validators.required)
});
```

No reset:

```typescript
this.positionForm.reset({
  acaoId: null,
  corretoraId: null,
  quantidade: null,
  precoMedio: null,
  dataPrimeiraCompra: null
});
```

## Trocar IDs por seletores

Usar os endpoints que já existem para listar ações e corretoras.

Em vez de:

```text
ID ação: 0
ID corretora: 0
```

mostrar:

```text
Ação
[Selecione uma ação ▾]

Corretora
[Selecione uma corretora ▾]
```

Opções:

```html
<option [ngValue]="null" disabled>Selecione uma ação</option>
@for (acao of acoes(); track acao.id) {
  <option [ngValue]="acao.id">
    {{ acao.ticker }} — {{ acao.nomeEmpresa }}
  </option>
}
```

```html
<option [ngValue]="null" disabled>Selecione uma corretora</option>
@for (corretora of corretoras(); track corretora.id) {
  <option [ngValue]="corretora.id">
    {{ corretora.nomeFantasia || corretora.razaoSocial }}
  </option>
}
```

## Formulário de posição

Organizar em duas linhas, com labels:

```text
Ação *                       Corretora *
[Selecione uma ação]         [Selecione uma corretora]

Quantidade comprada *        Preço médio por unidade *
[Ex.: 100]                   [Ex.: 28,50]

Data da primeira compra *
[dd/mm/aaaa]

[Cancelar] [Adicionar posição]
```

Se não houver ação cadastrada, mostrar botão “Cadastrar ação”. Se não houver corretora, mostrar “Cadastrar corretora”.

---

# 6. Melhorias de Corretoras

## Separar busca e cadastro

Cabeçalho:

```text
Corretoras
Cadastre as instituições onde seus investimentos estão guardados.
[+ Cadastrar corretora]
```

Busca acima da lista:

```text
Buscar corretora cadastrada
[Pesquise por CNPJ]
```

O cadastro pode abrir em modal simples ou card expansível. Não criar wizard complexo.

## Formulário

```text
CNPJ da corretora *
[00.000.000/0000-00]
Usaremos o CNPJ para consultar os dados da instituição.

CEP *                  Número *
[00000-000]            [Ex.: 75]

Complemento
[Ex.: Torre Sul — opcional]

[Cancelar] [Cadastrar corretora]
```

Aplicar:

- máscara visual de CNPJ;
- máscara visual de CEP;
- enviar somente números;
- mensagens abaixo dos campos;
- manter valores quando ocorrer erro;
- spinner no botão durante cadastro;
- impedir clique duplo;
- explicar por que o botão está desabilitado.

## Mensagens de erro

Usar o erro já retornado pelo backend e traduzi-lo no frontend:

| Situação | Mensagem |
|---|---|
| CNPJ inválido | Confira o CNPJ informado. |
| CEP inválido | Confira o CEP informado. |
| Corretora duplicada | Esta corretora já está cadastrada. |
| Empresa inativa | A empresa encontrada não está ativa. |
| Instituição não confirmada | Não foi possível confirmar esta instituição agora. |
| Integração indisponível | O serviço de consulta está indisponível. Tente novamente. |

Não modificar adapters ou backend. Apenas apresentar corretamente o erro que já chega ao Angular.

## Lista

Quando houver corretoras, mostrar cards ou tabela com:

- nome fantasia;
- razão social;
- CNPJ formatado;
- cidade/UF;
- situação “Validada” ou “Pendente”;
- data da validação, quando existir.

Estado vazio:

```text
Nenhuma corretora cadastrada
Cadastre a instituição utilizada nas suas compras para adicionar posições à carteira.
[Cadastrar primeira corretora]
```

---

# 7. Melhorias de Ações

## Cabeçalho

```text
Ações
Cadastre ativos para acompanhar cotações e adicioná-los às suas carteiras.
[+ Cadastrar ação]
```

## Formulário

```text
Ticker *
[Ex.: PETR4]
É o código de negociação do ativo na bolsa.

Mercado *
[Brasil ▾]
Brasil: ativos negociados na B3. Estados Unidos: ações americanas.

Exemplos: [PETR4] [VALE3] [ITUB4] [AAPL]

[Cancelar] [Cadastrar ação]
```

Regras:

- converter ticker para maiúsculas;
- remover espaços;
- exemplos podem somente preencher o campo;
- preservar formulário depois de erro;
- mostrar “Buscando ação e cotação…” no carregamento.

## Lista

Melhorar tabela atual com:

- busca por ticker;
- filtro por mercado;
- ticker em destaque;
- nome da empresa;
- mercado;
- cotação;
- data da cotação;
- botão “Ver histórico”;
- botão “Atualizar cotação” com spinner somente na linha clicada.

---

# 8. Interações simples

Adicionar somente interações pequenas e úteis:

- cards clicáveis com hover suave;
- botões com spinner;
- modal/card expansível para formulários;
- skeleton durante carregamento;
- toast de sucesso;
- mensagem de erro dentro do formulário;
- confirmação antes de excluir;
- transição curta de 150–200 ms;
- foco visível.

Não criar animações complexas.

---

# 9. Responsividade básica

Sem criar smoke tests ou infraestrutura nova.

Apenas ajustar CSS:

- desktop: cards em colunas;
- tablet: duas colunas;
- celular: uma coluna;
- formulários empilhados no celular;
- sidebar atual vira menu recolhível;
- tabelas com rolagem horizontal;
- botões principais com largura total no celular.

---

# 10. Arquivos que podem ser alterados

Priorizar somente os arquivos existentes relacionados ao frontend:

```text
frontend/src/app/features/pages.ts
frontend/src/app/features/insights-dashboard.page.ts
frontend/src/app/features/position-table.page.ts
frontend/src/app/app.html
frontend/src/app/app.scss
frontend/src/styles.scss
frontend/src/app/core/feedback/notification.service.ts
frontend/src/app/core/api/api-error.ts
frontend/src/app/core/api/api-error.interceptor.ts
```

Alterar outros arquivos frontend somente quando necessário para compilar.

Não alterar Java, banco, migrations, adapters ou integrações externas.

---

# 11. Validação mínima

Depois das alterações, executar apenas as verificações necessárias do frontend:

```bash
cd frontend
npm run build
npm test -- --run
```

Corrigir erros de compilação ou testes causados pelas mudanças.

Não criar uma suíte nova de smoke tests, testes de adapters ou testes end-to-end.

---

# 12. Critérios de aceite

- [ ] O frontend está visualmente mais moderno e organizado.
- [ ] O menu não abre automaticamente uma carteira inexistente.
- [ ] Não aparecem três mensagens de erro iguais.
- [ ] Mensagens não mencionam backend ou porta para o cliente.
- [ ] Campos de posição não começam com `0`.
- [ ] Ação é selecionada por ticker/nome.
- [ ] Corretora é selecionada por nome.
- [ ] Todos os campos possuem labels.
- [ ] O botão “Abrir visão geral” está bonito e destacado.
- [ ] Carteiras aparecem como cards organizados.
- [ ] Cadastro de corretora possui máscara, ajuda e erros claros.
- [ ] Cadastro de ação explica ticker e mercado.
- [ ] Estados vazios possuem botão para o próximo passo.
- [ ] Formulários mantêm os valores quando ocorre erro.
- [ ] Loading e sucesso possuem feedback visual.
- [ ] Layout se adapta ao celular com CSS.
- [ ] Nenhuma regra do backend foi alterada.
- [ ] Nenhum adapter foi alterado.
- [ ] Nenhum dado fictício foi criado.
- [ ] Build do Angular passa.

---

# 13. Entrega esperada

Ao finalizar, informar somente:

1. arquivos frontend alterados;
2. melhorias visuais realizadas;
3. erros de interface corrigidos;
4. resultado do build e dos testes do frontend;
5. como iniciar o Angular.

Não apresentar novas fases de backend nem tarefas fora deste escopo.
