# Especificação para o Codex — Refatoração do Frontend da Carteira de Investimentos

## 1. Instrução principal

Analise o projeto completo antes de alterar qualquer arquivo e implemente integralmente esta especificação. O objetivo é corrigir problemas funcionais e reconstruir as telas **Visão geral**, **Minhas carteiras**, **Corretoras** e **Ações**, deixando o fluxo compreensível para uma pessoa que não conhece IDs internos nem regras técnicas do backend.

Não implemente dados financeiros fictícios. Todos os valores apresentados como reais devem vir do backend. Estados sem dados devem orientar o usuário sobre a próxima ação possível.

Antes de concluir:

1. execute o backend e o frontend;
2. teste todas as rotas e requisições;
3. execute os testes automatizados;
4. execute o build de produção;
5. corrija todos os erros encontrados;
6. valide desktop, tablet e celular;
7. apresente um resumo dos arquivos alterados e dos testes executados.

## 2. Referências de produto

Usar como referência de hierarquia e experiência, sem copiar marca, código ou identidade visual:

- [Carteira Status Invest](https://statusinvest.com.br/produtos/carteira-de-investimentos): patrimônio, preço médio, rentabilidade, composição detalhada, proventos e transações apresentados com contexto.
- [Investidor10](https://investidor10.com.br/): categorias claras, busca de ativos, indicadores, ferramentas e explicação do conteúdo para investidores iniciantes.

Princípios extraídos das referências:

- o usuário vê primeiro o que possui e como está o desempenho;
- ações primárias são visualmente evidentes;
- campos usam nomes do domínio, nunca IDs técnicos;
- estados vazios explicam o que falta e oferecem um próximo passo;
- informações relacionadas aparecem agrupadas;
- valores importantes possuem hierarquia visual;
- lucro e prejuízo usam cores sem depender apenas da cor;
- tabelas possuem cabeçalhos claros, filtros e ações identificáveis.

## 3. Diagnóstico confirmado pelas telas e pelo código

### 3.1. Visão geral abre uma carteira inexistente

O menu lateral possui uma rota fixa:

```html
<a routerLink="/dashboard/1">Visão geral</a>
```

Isso pressupõe que sempre existe uma carteira com ID `1`. Na imagem existem carteiras como **Jhonatan** e **Laura**, mas não há garantia de que alguma possua ID `1`.

Consequência:

- `GET /dashboard/carteiras/1` retorna `404`;
- `GET /carteiras/1/evolucao` retorna `404`;
- `GET /carteiras/1/proventos` retorna `404`;
- a página fica vazia;
- aparecem três mensagens iguais: “O registro solicitado não foi encontrado”.

### 3.2. O interceptor cria três toasts iguais

O interceptor mostra um toast para toda resposta HTTP com erro. Mesmo quando o componente trata a falha com `catchError`, o interceptor já exibiu a mensagem.

Resultado: as três requisições do dashboard geram três toasts iguais.

### 3.3. Formulário de posição começa com zeros

O formulário atual inicia assim:

```typescript
acaoId: [0, Validators.min(1)]
corretoraId: [0, Validators.min(1)]
quantidade: [0, Validators.min(0.000001)]
precoMedio: [0, Validators.min(.01)]
```

Por isso os campos mostram `0` antes de o usuário digitar. Além de visualmente ruim, “ID ação” e “ID corretora” expõem detalhes internos que um cliente não deveria precisar conhecer.

### 3.4. Botão “Abrir dashboard” é apenas um link simples

O link não tem hierarquia visual, ícone, área de clique adequada nem contexto. Ele fica misturado ao formulário e parece texto comum.

### 3.5. Cadastro de corretora não informa o processo

O formulário atual contém apenas placeholders:

```text
CNPJ | CEP | Número | Complemento | Cadastrar
```

Problemas:

- não existem labels persistentes;
- não há máscara de CNPJ ou CEP;
- não há exemplos;
- não há validação visual por campo;
- não explica que o backend consulta empresa, endereço e registro financeiro;
- não preserva os campos após erro;
- o erro global não informa em qual integração houve falha;
- a busca por CNPJ parece outro formulário de cadastro;
- quando a lista está vazia, sobra um grande espaço sem orientação útil.

### 3.6. A tela de ações funciona, mas ainda é técnica

Ela pede ticker e mercado sem explicar exemplos, diferenças ou o que acontecerá. A atualização da cotação não exibe data, fonte, progresso adequado ou contexto.

### 3.7. Componentes grandes demais

`pages.ts` concentra várias páginas com templates inline extensos. Isso dificulta manutenção, testes, estilos específicos e evolução da interface.

## 4. Arquitetura obrigatória da solução

Separar as páginas em pastas próprias:

```text
frontend/src/app/features/
├── dashboard/
│   ├── dashboard.page.ts
│   ├── dashboard.page.html
│   ├── dashboard.page.scss
│   └── dashboard.page.spec.ts
├── carteiras/
│   ├── carteiras.page.ts
│   ├── carteiras.page.html
│   ├── carteiras.page.scss
│   └── carteiras.page.spec.ts
├── corretoras/
│   ├── corretoras.page.ts
│   ├── corretoras.page.html
│   ├── corretoras.page.scss
│   └── corretoras.page.spec.ts
└── acoes/
    ├── acoes.page.ts
    ├── acoes.page.html
    ├── acoes.page.scss
    └── acoes.page.spec.ts
```

Criar componentes reutilizáveis quando fizer sentido:

```text
frontend/src/app/shared/components/
├── page-header/
├── empty-state/
├── form-field/
├── status-badge/
├── skeleton/
└── confirm-dialog/
```

Não criar abstrações vazias ou componentes que apenas envolvem uma única tag.

## 5. Correção obrigatória da navegação do dashboard

### 5.1. Remover ID fixo do menu

Trocar:

```html
routerLink="/dashboard/1"
```

por:

```html
routerLink="/dashboard"
```

### 5.2. Rotas esperadas

```typescript
export const routes: Routes = [
  { path: 'dashboard', component: DashboardPage },
  { path: 'dashboard/:carteiraId', component: DashboardPage },
  { path: 'carteiras', component: CarteirasPage },
  { path: 'acoes', component: AcoesPage },
  { path: 'corretoras', component: CorretorasPage },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' }
];
```

### 5.3. Regra de seleção

Ao abrir `/dashboard`:

1. carregar `GET /api/v1/carteiras?page=0&size=100`;
2. se existir uma carteira salva como última seleção, selecioná-la somente se ainda existir;
3. caso contrário, selecionar a primeira carteira retornada;
4. atualizar a URL para `/dashboard/{id}` com `replaceUrl: true`;
5. somente depois carregar resumo, evolução e proventos;
6. se não houver carteira, exibir onboarding sem fazer requisições com ID inventado.

Ao abrir `/dashboard/:carteiraId`:

- validar se o ID existe na lista;
- se não existir, selecionar a primeira carteira válida e substituir a URL;
- nunca disparar as três requisições usando um ID inválido.

Criar um `PortfolioContextService` para manter a carteira selecionada:

```typescript
@Injectable({ providedIn: 'root' })
export class PortfolioContextService {
  private readonly storageKey = 'selectedPortfolioId';
  readonly selectedId = signal<number | null>(null);

  select(id: number): void {
    this.selectedId.set(id);
    localStorage.setItem(this.storageKey, String(id));
  }

  restore(): number | null {
    const value = Number(localStorage.getItem(this.storageKey));
    return Number.isInteger(value) && value > 0 ? value : null;
  }
}
```

## 6. Eliminar toasts duplicados

Implementar uma destas abordagens, preferencialmente a primeira.

### Abordagem recomendada: contexto HTTP silencioso

Criar token:

```typescript
export const SILENT_HTTP_ERROR = new HttpContextToken<boolean>(() => false);
```

No interceptor:

```typescript
if (!request.context.get(SILENT_HTTP_ERROR)) {
  notifications.show(apiErrorMessage(mapped.code), 'error');
}
```

Nos endpoints agrupados do dashboard:

```typescript
const options = {
  context: new HttpContext().set(SILENT_HTTP_ERROR, true)
};
```

O componente deve exibir uma única mensagem contextual, não três toasts.

Também implementar deduplicação no `NotificationService`: mensagens iguais não devem ser adicionadas novamente enquanto uma notificação idêntica estiver visível.

### Critério

Uma falha do dashboard pode gerar no máximo:

- um banner principal para falha do resumo; ou
- um aviso discreto para falhas opcionais.

Nunca três toasts idênticos.

## 7. Reconstrução da Visão geral

### 7.1. Cabeçalho

Mostrar:

- título “Visão geral”;
- descrição simples;
- seletor de carteira pelo nome;
- botão “Atualizar dados”;
- data e hora da última atualização;
- estado da integração, sem linguagem técnica desnecessária.

### 7.2. Quando não houver carteira

Exibir um onboarding central:

```text
Comece criando sua primeira carteira
Organize seus investimentos, adicione posições e acompanhe sua evolução.
[Criar minha primeira carteira]
```

Não mostrar “backend”, porta `8081`, ID ou erro `404` para o usuário final.

Detalhes técnicos podem aparecer apenas em uma seção expansível “Detalhes do erro” durante desenvolvimento.

### 7.3. Quando houver carteira sem posições

Exibir:

```text
Sua carteira ainda está vazia
Você já criou a carteira. Agora selecione uma ação, uma corretora e informe os dados da compra.
[Adicionar primeira posição]
```

### 7.4. Quando houver dados

Hierarquia mínima:

1. patrimônio atual;
2. total investido;
3. resultado acumulado em valor e percentual;
4. quantidade de ativos;
5. evolução patrimonial;
6. distribuição por ativo;
7. tabela de posições;
8. proventos;
9. indicadores externos.

O Status Invest destaca preço médio, patrimônio, rentabilidade, composição e proventos. Aplicar a mesma lógica informacional usando somente os campos que realmente existem no backend.

### 7.5. Falhas parciais

- falha em indicadores não deve esconder o patrimônio;
- falha em proventos não deve esconder posições;
- ausência de evolução deve exibir estado vazio dentro do card;
- `UNAVAILABLE` deve ser apresentado como “Dados ainda não disponíveis”;
- `STALE` deve ser apresentado como “Última atualização antiga”.

## 8. Reconstrução de Minhas carteiras

### 8.1. Cabeçalho e ação primária

Cabeçalho:

```text
Minhas carteiras
Crie carteiras e organize suas posições por objetivo.
[+ Nova carteira]
```

O formulário de criação/edição deve abrir em modal ou painel lateral, não ocupar permanentemente o topo da página.

Campos:

- `Nome da carteira` — obrigatório;
- `Descrição ou objetivo` — opcional;
- exemplos: “Longo prazo”, “Dividendos”, “Reserva em dólar”.

### 8.2. Lista de carteiras

Substituir a lista de botões por cards selecionáveis contendo:

- nome;
- descrição;
- quantidade de posições, se disponível;
- estado selecionado;
- botão de opções `⋮`;
- editar;
- excluir com confirmação.

Não deixar o botão excluir vermelho permanentemente em destaque. Exclusão é ação secundária/destrutiva e deve ficar em menu ou botão discreto.

### 8.3. Botão Abrir dashboard

Substituir o link simples por botão claro:

```html
<a class="dashboard-button" [routerLink]="['/dashboard', selected.id]">
  <span>Abrir visão geral</span>
  <span aria-hidden="true">→</span>
</a>
```

Requisitos:

- estilo de botão primário ou secundário forte;
- área mínima de clique de 44 px;
- ícone de seta;
- texto “Abrir visão geral” ou “Ver dashboard”;
- alinhado ao título da carteira;
- foco visível;
- não parecer link sublinhado comum.

### 8.4. Formulário para adicionar posição

Não solicitar IDs numéricos.

Carregar:

- `GET /api/v1/acoes?page=0&size=100`;
- `GET /api/v1/corretoras?page=0&size=100`.

Exibir selects:

```html
<label>
  Ação
  <select formControlName="acaoId">
    <option [ngValue]="null" disabled>Selecione uma ação</option>
    @for (acao of acoes(); track acao.id) {
      <option [ngValue]="acao.id">
        {{ acao.ticker }} — {{ acao.nomeEmpresa }}
      </option>
    }
  </select>
</label>

<label>
  Corretora
  <select formControlName="corretoraId">
    <option [ngValue]="null" disabled>Selecione uma corretora</option>
    @for (corretora of corretoras(); track corretora.id) {
      <option [ngValue]="corretora.id">
        {{ corretora.nomeFantasia || corretora.razaoSocial }}
      </option>
    }
  </select>
</label>
```

Usar valores iniciais nulos:

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

Depois de salvar:

```typescript
this.positionForm.reset({
  acaoId: null,
  corretoraId: null,
  quantidade: null,
  precoMedio: null,
  dataPrimeiraCompra: null
});
```

Labels e explicações:

| Campo | Label | Texto auxiliar |
|---|---|---|
| `acaoId` | Ação | Selecione o ativo cadastrado |
| `corretoraId` | Corretora | Instituição onde a compra foi realizada |
| `quantidade` | Quantidade comprada | Ex.: 100 |
| `precoMedio` | Preço médio por unidade | Ex.: R$ 28,50 |
| `dataPrimeiraCompra` | Data da primeira compra | Data inicial da posição |

### 8.5. Dependências ausentes

Se não houver ações:

```text
Você precisa cadastrar uma ação antes de adicionar uma posição.
[Cadastrar ação]
```

Se não houver corretoras:

```text
Você precisa cadastrar uma corretora antes de adicionar uma posição.
[Cadastrar corretora]
```

Não mostrar formulário impossível de preencher.

### 8.6. Tabela de posições

Colunas:

- ativo e empresa;
- corretora;
- quantidade;
- preço médio;
- cotação atual;
- valor investido;
- saldo atual;
- resultado em reais;
- rentabilidade percentual;
- ações.

Se o DTO não tiver o nome da corretora, manter a corretora selecionada no formulário e mostrar somente os campos disponíveis, sem inventar dados. Avaliar se é adequado enriquecer `AtivoCarteiraResponse` no backend com nome da corretora.

## 9. Reconstrução de Corretoras

### 9.1. Layout

Separar em duas áreas:

1. **Cadastrar corretora**;
2. **Corretoras cadastradas**.

Não colocar busca e cadastro na mesma linha sem identificação.

### 9.2. Formulário orientado

Usar labels persistentes:

```text
CNPJ da corretora
CEP da unidade
Número
Complemento (opcional)
```

Adicionar texto explicativo:

```text
Ao cadastrar, consultaremos os dados da empresa, o endereço pelo CEP e a situação da instituição no mercado financeiro.
```

Adicionar máscaras visuais, mas enviar somente números:

- CNPJ: `00.000.000/0000-00`;
- CEP: `00000-000`.

Normalizar antes do envio:

```typescript
const digitsOnly = (value: string) => value.replace(/\D/g, '');
```

Validar:

- CNPJ com 14 dígitos;
- CEP com 8 dígitos;
- número obrigatório;
- complemento opcional;
- mensagens abaixo do campo somente depois de `touched` ou submit.

### 9.3. Erros de cadastro

Não limpar o formulário quando ocorrer erro.

Apresentar banner dentro do formulário:

| Situação | Mensagem amigável |
|---|---|
| CNPJ inválido | Confira o CNPJ. Ele deve possuir 14 dígitos e ser válido. |
| CEP inválido | Não encontramos esse CEP. Confira os 8 dígitos. |
| CNPJ duplicado | Esta corretora já está cadastrada. |
| Empresa inativa | A empresa encontrada não está com situação ativa. |
| Não validada no mercado | Não foi possível confirmar esta instituição na fonte financeira. |
| Integração indisponível | O serviço de consulta está temporariamente indisponível. Tente novamente. |

Incluir detalhes técnicos somente em ambiente de desenvolvimento.

### 9.4. Diagnóstico funcional do backend

O cadastro executa três consultas obrigatórias:

1. `empresaAdapter.buscarPorCnpj(cnpj)`;
2. `enderecoAdapter.buscarPorCep(cep)`;
3. `instituicaoFinanceiraFacade.validarPorCnpj(cnpj)`.

O Codex deve testar cada integração isoladamente com XP, BTG, Genial ou Ágora e identificar qual etapa falha. Não contornar a validação atribuindo `validadaMercadoFinanceiro = true` sem confirmação real.

Verificar:

- configuração das URLs externas;
- normalização do CNPJ;
- resposta real da BrasilAPI;
- resposta do ViaCEP;
- fonte de instituições financeiras;
- diferença entre CNPJ da marca, banco, filial e corretora;
- códigos HTTP e `ErrorCode` retornados;
- mensagem que chega ao Angular.

Se a fonte CVM não oferecer busca confiável por CNPJ, documentar e implementar um adapter compatível com a fonte pública definida no projeto, sem inventar autorização.

### 9.5. Lista de corretoras

Exibir cards ou tabela com:

- nome fantasia;
- razão social;
- CNPJ formatado;
- cidade/UF;
- situação cadastral;
- badge “Validada” ou “Validação pendente”;
- fonte e data da validação;
- ação de visualizar detalhes.

Estado vazio deve incluir um pequeno fluxo em três passos:

```text
1. Informe o CNPJ
2. Confirme o endereço
3. Aguarde a validação automática
```

### 9.6. Busca

Criar campo de busca separado acima da lista:

```text
Buscar corretora cadastrada
Pesquise por nome ou CNPJ
```

Quando a busca não encontrar resultado:

- não disparar toast global genérico;
- mostrar “Nenhuma corretora encontrada para este CNPJ” junto ao campo;
- oferecer botão “Limpar busca”.

## 10. Melhorias da tela de Ações

### 10.1. Cadastro compreensível

Mostrar em card ou modal:

- label `Ticker`;
- texto auxiliar `Código de negociação, como PETR4 ou AAPL`;
- label `Mercado`;
- descrição `Brasil para ativos da B3; Estados Unidos para ações americanas`;
- exemplos clicáveis `PETR4`, `VALE3`, `ITUB4`, `AAPL`, `MSFT` apenas para preencher o campo, nunca para simular resultados;
- transformação automática do ticker para maiúsculas;
- botão “Buscar e cadastrar ação”.

### 10.2. Lista de ações

Exibir:

- ticker em destaque;
- nome da empresa;
- mercado e moeda;
- cotação atual;
- data/hora da cotação;
- estado da cotação;
- botão “Atualizar cotação” com spinner;
- link “Ver histórico”.

Adicionar busca local por ticker/nome e filtro por mercado.

### 10.3. Erros

- ticker duplicado: informar que a ação já existe e destacar o item existente;
- ticker não encontrado: explicar o formato esperado;
- fonte indisponível: manter o formulário preenchido;
- atualização em andamento: desabilitar apenas a linha correspondente.

## 11. Padrão visual obrigatório

Manter a identidade roxa, mas melhorar contraste e densidade.

### Regras

- largura máxima entre `1280px` e `1440px`;
- cards com cabeçalho, conteúdo e ação bem definidos;
- espaçamento baseado em escala de 4/8 px;
- títulos de página entre `28px` e `36px`, conforme viewport;
- corpo mínimo de `14px`;
- labels nunca substituídas apenas por placeholders;
- botões com altura mínima de `44px`;
- estados hover, focus, active e disabled;
- skeleton durante carregamento;
- tabelas responsivas com rolagem horizontal;
- formulários em uma ou duas colunas, nunca cinco campos espremidos em uma linha;
- ação primária roxa;
- ações secundárias neutras;
- exclusão discreta até confirmação;
- verde e ícone para lucro;
- vermelho e ícone para prejuízo;
- não depender exclusivamente de cor.

### Responsividade

- desktop: sidebar fixa e conteúdo em duas colunas quando útil;
- tablet: cards reorganizados e formulários em duas colunas;
- celular: menu recolhível, cards em uma coluna e botões com largura total quando necessário.

## 12. Acessibilidade e linguagem

- todos os campos devem ter `<label>`;
- `aria-describedby` para ajuda e erro;
- `aria-live="polite"` para feedback assíncrono;
- foco levado ao primeiro campo inválido após submit;
- modal deve prender foco e fechar com `Escape`;
- não usar “ID”, “backend”, “endpoint” ou “porta 8081” na mensagem principal do cliente;
- usar frases orientadas a ação;
- botões devem descrever o efeito: “Cadastrar corretora”, “Adicionar posição”, “Abrir visão geral”.

## 13. Tratamento de carregamento e erro

Cada página deve possuir estados explícitos:

```typescript
type ViewState = 'idle' | 'loading' | 'success' | 'empty' | 'error';
```

Requisitos:

- usar `finalize` em todas as requisições que alteram loading;
- incluir callback `error` nos `subscribe` que hoje possuem apenas `next`;
- impedir clique duplicado durante salvamento;
- preservar formulário após erro;
- recarregar somente o recurso alterado;
- não esconder dados válidos por falha de recurso opcional;
- mensagens técnicas apenas no console em desenvolvimento.

## 14. Testes obrigatórios

### Visão geral

- `/dashboard` seleciona a primeira carteira existente;
- `/dashboard/999999` redireciona para uma carteira válida;
- nenhuma carteira não dispara chamadas de resumo com ID `1`;
- erro em proventos mantém patrimônio visível;
- erro em indicadores mantém posições visíveis;
- três erros simultâneos não geram três toasts iguais;
- troca de carteira atualiza URL e dados.

### Carteiras

- formulário de posição começa vazio, sem `0`;
- ação e corretora aparecem pelo nome, não pelo ID;
- botão fica desabilitado até o formulário ser válido;
- ausência de ação mostra link de cadastro;
- ausência de corretora mostra link de cadastro;
- botão “Abrir visão geral” navega para o ID selecionado;
- edição preenche os valores existentes;
- reset devolve os campos para `null`.

### Corretoras

- CNPJ e CEP são enviados somente com números;
- CNPJ inválido mostra erro no campo;
- erro do backend preserva os valores;
- busca sem resultado não cria toast global;
- cadastro bem-sucedido limpa formulário e atualiza lista;
- CNPJ duplicado mostra mensagem específica;
- botão de submit impede requisição duplicada.

### Ações

- ticker é convertido para maiúsculas;
- mercado possui explicação;
- erro preserva formulário;
- atualização mostra loading apenas na ação clicada;
- busca e filtro funcionam.

### Build

Executar:

```bash
cd frontend
npm ci
npm run build
npm test -- --run
```

Backend:

```bash
./gradlew test
```

No Windows:

```powershell
.\gradlew.bat test
```

## 15. Critérios de aceite finais

- [ ] O menu “Visão geral” não contém `/dashboard/1` fixo.
- [ ] A visão geral abre uma carteira realmente existente.
- [ ] Não aparecem três toasts iguais para a mesma situação.
- [ ] Uma carteira inexistente não deixa a página vazia.
- [ ] Os formulários não mostram `0` como valor inicial.
- [ ] O usuário escolhe ação por ticker/nome.
- [ ] O usuário escolhe corretora pelo nome.
- [ ] Nenhum campo de interface pede ID técnico.
- [ ] “Abrir visão geral” é um botão claro e bem posicionado.
- [ ] Cadastro de corretora possui labels, máscaras, validação e mensagens específicas.
- [ ] O motivo real da falha de cadastro de corretora foi identificado e corrigido.
- [ ] A tela de ações explica ticker e mercado.
- [ ] Estados vazios possuem próxima ação.
- [ ] Dados opcionais indisponíveis não escondem dados válidos.
- [ ] Todas as telas funcionam em desktop e celular.
- [ ] Testes do frontend passam.
- [ ] Testes do backend passam.
- [ ] Build de produção passa sem warnings críticos.

## 16. Restrições

- Não usar dados financeiros falsos como se fossem reais.
- Não remover validações do backend apenas para fazer o formulário funcionar.
- Não cadastrar automaticamente instituições sem confirmação da fonte financeira.
- Não expor tokens ou URLs privadas no Angular.
- Não adicionar bibliotecas grandes sem necessidade.
- Não manter todas as páginas dentro de `pages.ts`.
- Não usar placeholders como único rótulo.
- Não usar IDs técnicos como campos para usuário final.
- Não finalizar a tarefa apenas porque o TypeScript compilou: testar os fluxos reais.

## 17. Entrega esperada do Codex

Ao finalizar, fornecer:

1. diagnóstico da causa de cada erro;
2. lista dos arquivos criados e alterados;
3. endpoints testados;
4. testes executados e resultados;
5. resultado do build;
6. instruções para iniciar backend e frontend;
7. limitações restantes, especialmente integrações externas;
8. capturas ou descrição objetiva das quatro telas finais.
