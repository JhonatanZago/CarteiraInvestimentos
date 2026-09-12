Implemente no projeto CarteiraInvestimentos a tela de autenticação baseada exatamente na segunda imagem de referência anexada: login centralizado em um card escuro translúcido, fundo financeiro desfocado, identidade visual verde premium e suporte ao tema.

Esta não é apenas uma tarefa visual. O sistema de autenticação precisa funcionar integralmente no backend Spring Boot Java 21 e no frontend Angular standalone.

## Regra principal

Antes de implementar, analise completamente o projeto existente:

* estrutura do backend;
* dependências do `build.gradle`;
* entidades atuais;
* controllers/resources;
* serviços;
* tratamento de exceções;
* configuração de CORS;
* rotas do Angular;
* interceptors existentes;
* armazenamento atual;
* estrutura das carteiras;
* vínculo entre carteiras, posições, ações, vendas e outras operações.

Não remova nem reescreva funcionalidades que já estão funcionando.

Não crie uma camada arquitetural chamada `provider`. Preserve o padrão existente com services, adapters, facades, repositories, mappers, DTOs, domains, resources e exceptions.

## 1. Funcionalidades obrigatórias

Implementar:

* cadastro de usuário;
* login com e-mail e senha;
* logout;
* recuperação da sessão após atualizar a página;
* opção “Manter conectado”;
* consulta do usuário autenticado;
* proteção das rotas privadas;
* proteção dos endpoints do backend;
* redirecionamento automático para login;
* redirecionamento após autenticação;
* senha criptografada;
* tratamento de token expirado;
* refresh de autenticação seguro;
* mensagens de erro amigáveis;
* botão para mostrar e ocultar senha;
* loading durante login;
* prevenção de múltiplos envios;
* recuperação de senha;
* tema claro e escuro na tela de autenticação.

## 2. Modelo de usuário

Crie ou adapte uma entidade equivalente a:

```java
Usuario {
    Long id;
    String nome;
    String email;
    String senha;
    PerfilUsuario perfil;
    Boolean ativo;
    LocalDateTime criadoEm;
    LocalDateTime atualizadoEm;
}
```

Utilize enum:

```java
public enum PerfilUsuario {
    USER,
    ADMIN
}
```

Regras:

* e-mail obrigatório;
* normalizar e-mail com `trim().toLowerCase()`;
* e-mail único;
* nome obrigatório;
* senha nunca deve ser devolvida por endpoints;
* senha armazenada somente com BCrypt;
* usuário novo recebe `USER`;
* usuário inativo não pode entrar;
* perfil não pode ser escolhido livremente no cadastro público.

Não exponha a entidade diretamente. Utilize DTOs.

## 3. DTOs

Crie DTOs equivalentes a:

```java
CadastroUsuarioRequest
LoginRequest
LoginResponse
UsuarioLogadoResponse
RefreshTokenResponse
SolicitarRecuperacaoSenhaRequest
RedefinirSenhaRequest
```

Sugestão:

```java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String senha,
    boolean manterConectado
) {}
```

A resposta não pode conter senha, hash, refresh token ou informações internas.

## 4. Endpoints

Implemente ou adapte:

```text
POST /api/v1/auth/cadastro
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
POST /api/v1/auth/esqueci-minha-senha
POST /api/v1/auth/redefinir-senha
```

Contratos esperados:

### Cadastro

```json
{
  "nome": "Jhonatan Zago",
  "email": "jhonatan@email.com",
  "senha": "SenhaForte@123",
  "confirmacaoSenha": "SenhaForte@123"
}
```

### Login

```json
{
  "email": "jhonatan@email.com",
  "senha": "SenhaForte@123",
  "manterConectado": true
}
```

### Usuário autenticado

```json
{
  "id": 1,
  "nome": "Jhonatan Zago",
  "email": "jhonatan@email.com",
  "perfil": "USER"
}
```

Use os status HTTP corretamente:

* `200`: login realizado;
* `201`: cadastro realizado;
* `204`: logout;
* `400`: dados inválidos;
* `401`: credenciais inválidas ou sessão expirada;
* `403`: usuário sem autorização;
* `409`: e-mail já cadastrado.

## 5. Segurança

Utilize Spring Security compatível com a versão atual do Spring Boot.

Não utilize configurações obsoletas como `WebSecurityConfigurerAdapter`.

Utilize:

* `SecurityFilterChain`;
* `PasswordEncoder`;
* BCrypt;
* autenticação stateless;
* access token de curta duração;
* refresh token com expiração;
* assinatura JWT forte;
* segredo configurado por variável de ambiente;
* validação de expiração e assinatura;
* logout com invalidação do refresh token;
* respostas JSON para `401` e `403`.

Nunca:

* salvar senha em texto puro;
* colocar segredo JWT diretamente no código;
* registrar senha ou token em logs;
* devolver refresh token no JSON;
* aceitar algoritmo JWT vindo do cliente;
* colocar token em parâmetros da URL;
* expor detalhes que permitam descobrir se um e-mail existe.

O refresh token deve ser armazenado em cookie:

* `HttpOnly`;
* `SameSite=Lax` ou configuração segura equivalente;
* `Path=/api/v1/auth`;
* `Secure=true` em produção;
* tempo diferente conforme “Manter conectado”.

No ambiente local HTTP, utilize configuração específica de desenvolvimento para permitir o cookie sem enfraquecer produção.

O access token pode ser mantido pelo Angular em memória e renovado pelo refresh token. Se a arquitetura existente exigir armazenamento no navegador, documente o risco e evite `localStorage` como primeira escolha.

A opção “Manter conectado” deve controlar a duração do refresh token:

* desmarcada: sessão curta;
* marcada: sessão persistente por período configurável.

## 6. Autorização dos endpoints

Deixe públicos somente:

```text
/api/v1/auth/login
/api/v1/auth/cadastro
/api/v1/auth/refresh
/api/v1/auth/esqueci-minha-senha
/api/v1/auth/redefinir-senha
```

Também preserve somente os endpoints públicos realmente necessários, como:

* documentação da API, se habilitada;
* console H2 apenas no perfil de desenvolvimento;
* recursos públicos;
* tratamento de preflight `OPTIONS`.

Todos os endpoints de carteiras, ações, corretoras, posições, vendas, histórico, proventos e dashboard devem exigir autenticação.

## 7. Isolamento dos dados por usuário

Isto é obrigatório.

Não basta esconder as telas. Um usuário não pode acessar dados de outro alterando o ID na URL.

Analise os relacionamentos atuais e associe os dados ao proprietário autenticado.

No mínimo:

* cada carteira deve pertencer a um usuário;
* consultas devem filtrar pelo usuário autenticado;
* criação deve associar automaticamente o usuário autenticado;
* atualização e exclusão devem validar propriedade;
* posições e operações devem ser acessíveis por meio de carteiras pertencentes ao usuário;
* IDs de outro usuário devem retornar `404` ou `403`, conforme padrão adotado;
* nenhuma listagem pode retornar dados globais de todos os usuários.

Não aceite `usuarioId` enviado pelo frontend para definir o proprietário. Obtenha o usuário pelo contexto de autenticação.

Preserve IDs, posições e relacionamentos existentes. Crie uma migração ou estratégia segura para os registros legados, sem apagar dados.

## 8. CORS

O Angular utiliza `http://localhost:4200` e o backend `http://localhost:8081`.

Configure CORS especificamente para a origem do frontend e permita credenciais quando necessário.

Não utilize simultaneamente:

```text
allowCredentials(true)
allowedOrigins("*")
```

Permitir:

* métodos utilizados pela aplicação;
* cabeçalhos necessários;
* cookie de refresh;
* header `Authorization`.

Não desabilite toda a segurança para resolver CORS.

## 9. Frontend Angular

Crie uma feature de autenticação organizada, seguindo o padrão standalone do projeto:

```text
core/auth/
features/auth/login/
features/auth/register/
features/auth/forgot-password/
features/auth/reset-password/
```

Adapte os nomes à estrutura real.

Criar:

* `AuthService`;
* modelos de autenticação;
* interceptor funcional;
* guard para rotas privadas;
* guard para impedir usuário autenticado de voltar ao login;
* gerenciamento do estado autenticado;
* carregamento inicial da sessão;
* logout no menu;
* tratamento central de `401`.

Não espalhe regras de autenticação por vários componentes.

## 10. Tela de login escolhida

Reproduza fielmente a segunda imagem:

* fundo ocupando toda a tela;
* fundo financeiro escuro e desfocado;
* gráfico e elementos de investimento discretos;
* card centralizado;
* card com transparência controlada;
* borda verde/cinza discreta;
* título `CARTEIRA`;
* subtítulo `Controle seus investimentos`;
* texto `Entre para acompanhar sua evolução`;
* inputs de e-mail e senha;
* ícones nos inputs;
* botão para mostrar ou ocultar senha;
* checkbox `Manter conectado`;
* link `Esqueci minha senha`;
* botão principal verde `Entrar`;
* divisor com `ou`;
* botão secundário `Criar nova conta`;
* alternador de tema no canto superior direito.

O fundo deve ser criado com HTML/CSS e elementos visuais leves já disponíveis no projeto. Não utilize uma captura estática do dashboard com dados pessoais.

Pode utilizar gradientes, gráficos abstratos e elementos decorativos, mas eles devem ter:

```css
pointer-events: none;
```

O card deve continuar legível e com contraste adequado.

## 11. Formulário de login

Use Reactive Forms.

Validações:

* e-mail obrigatório;
* formato de e-mail válido;
* senha obrigatória;
* mensagens abaixo dos campos;
* campos marcados após interação ou envio;
* tecla Enter envia o formulário;
* botão fica desabilitado quando inválido ou carregando;
* spinner durante requisição;
* impedir clique duplicado;
* preservar o e-mail após erro;
* nunca limpar a senha antes de apresentar o resultado;
* erro genérico para credenciais inválidas.

Mensagens:

```text
Informe seu e-mail.
Digite um e-mail válido.
Informe sua senha.
E-mail ou senha inválidos.
Sua sessão expirou. Entre novamente.
Não foi possível conectar ao servidor.
```

Não mostrar stack trace ou resposta bruta do backend.

## 12. Cadastro

O botão “Criar nova conta” deve abrir uma rota real, por exemplo:

```text
/cadastro
```

Campos:

* nome;
* e-mail;
* senha;
* confirmação de senha;
* aceite dos termos, caso existam termos reais.

Validações:

* nome obrigatório;
* e-mail válido;
* senhas iguais;
* requisitos de senha exibidos ao usuário;
* indicador simples de segurança da senha;
* mensagens do backend tratadas;
* após sucesso, autenticar ou direcionar para login com mensagem de confirmação.

Não adicionar checkbox de termos se não existir uma página ou conteúdo correspondente.

## 13. Recuperação de senha

O link “Esqueci minha senha” deve funcionar.

Fluxo:

1. usuário informa o e-mail;
2. backend sempre retorna mensagem genérica;
3. gerar token aleatório de uso único;
4. armazenar somente hash do token;
5. definir expiração;
6. invalidar após uso;
7. permitir cadastrar nova senha;
8. invalidar sessões antigas após alteração.

Mensagem:

```text
Se o e-mail estiver cadastrado, você receberá as instruções para redefinir sua senha.
```

Crie uma interface de adapter para envio de e-mail, seguindo a arquitetura do projeto.

Se ainda não existir serviço de e-mail:

* implemente a estrutura sem inventar envio em produção;
* no perfil de desenvolvimento, permita testar o fluxo com segurança;
* documente como configurar o adapter real;
* não exponha token em resposta de produção;
* não deixe botão sem ação.

## 14. Rotas

Estrutura esperada:

```text
/login
/cadastro
/esqueci-minha-senha
/redefinir-senha
```

As rotas internas atuais devem usar o guard.

Comportamento:

* usuário não autenticado acessando rota privada → `/login`;
* preservar a URL original;
* após login → voltar à URL original;
* sem URL anterior → dashboard;
* usuário autenticado acessando `/login` → dashboard;
* logout → `/login`;
* refresh inválido → limpar sessão e ir para `/login`.

Evite loop infinito entre interceptor, refresh e guard.

O interceptor não deve tentar atualizar token quando a própria requisição de refresh falhar.

## 15. Usuário no menu

Após o login, exiba no menu ou cabeçalho:

* nome do usuário;
* e-mail ou avatar com iniciais;
* opção de sair.

O logout deve:

* chamar o backend;
* invalidar refresh token;
* limpar estado do frontend;
* redirecionar para login;
* impedir retorno às páginas privadas pelo histórico do navegador.

## 16. Tema e responsividade

A tela deve funcionar no tema escuro e claro.

No celular:

* ocultar ou simplificar os elementos financeiros do fundo;
* card ocupar quase toda a largura;
* manter margens de 16px;
* evitar rolagem horizontal;
* manter botão e campos acessíveis;
* permitir abertura correta do teclado;
* conteúdo não pode ser cortado em telas baixas.

Testar pelo menos:

* 1920×1080;
* 1366×768;
* 768×1024;
* 390×844.

## 17. Migração e dados de desenvolvimento

Não apague o banco ou os dados existentes.

Se estiver usando H2 com `create-drop`, avalie e ajuste sem causar perda acidental.

Use migration se o projeto já tiver Flyway ou Liquibase. Não adicione ambos.

Crie usuário de desenvolvimento somente no perfil `dev`, caso seja necessário para testes.

Credenciais de desenvolvimento devem ser documentadas e nunca ativadas em produção.

Não deixe senha padrão dentro do código principal.

## 18. Testes obrigatórios do backend

Criar testes para:

* cadastro válido;
* e-mail duplicado;
* senha criptografada;
* login válido;
* senha incorreta;
* usuário inexistente;
* usuário inativo;
* acesso sem token;
* acesso com token inválido;
* token expirado;
* refresh válido;
* refresh expirado;
* logout;
* endpoint `/me`;
* usuário tentando acessar carteira de outro usuário;
* recuperação de senha;
* token de recuperação expirado;
* token de recuperação usado duas vezes.

## 19. Testes obrigatórios do frontend

Testar:

* validação dos formulários;
* botão mostrar senha;
* loading;
* resposta `401`;
* indisponibilidade do backend;
* guard;
* interceptor;
* restauração da sessão;
* logout;
* “Manter conectado”;
* redirecionamento após login;
* prevenção de loop de refresh;
* responsividade básica.

## 20. Verificação final

Execute:

```text
./gradlew test
./gradlew build
```

No frontend:

```text
npm test
npm run build
```

Depois execute backend e frontend juntos e teste manualmente:

1. criar conta;
2. realizar login;
3. atualizar a página;
4. abrir rota privada diretamente;
5. sair;
6. tentar voltar pelo navegador;
7. usar senha incorreta;
8. simular token expirado;
9. entrar com “Manter conectado”;
10. testar recuperação de senha;
11. confirmar isolamento entre dois usuários;
12. testar temas;
13. testar desktop e celular.

Não considere concluído apenas porque compilou.

## 21. Restrições finais

* Não alterar o visual já aprovado das páginas internas.
* Não quebrar os endpoints existentes.
* Não desativar segurança globalmente.
* Não utilizar autenticação simulada.
* Não utilizar apenas dados no `localStorage`.
* Não aceitar qualquer senha.
* Não deixar rotas privadas acessíveis diretamente.
* Não retornar senha ou refresh token.
* Não inventar integração de e-mail em produção.
* Não apagar dados existentes.
* Não gerar uma nova aplicação separada.
* Não implementar uma camada genérica chamada `provider`.
* Não encerrar outras aplicações ou portas sem necessidade.

Ao finalizar, informe:

1. arquitetura adotada;
2. arquivos criados e alterados;
3. endpoints;
4. estratégia de access e refresh token;
5. como os dados foram vinculados ao usuário;
6. configuração necessária no `.env`;
7. usuário de desenvolvimento, se criado;
8. testes executados;
9. resultado dos builds;
10. limitações restantes;
11. instruções exatas para executar e testar o login;
12. captura da tela final comparada à referência.
