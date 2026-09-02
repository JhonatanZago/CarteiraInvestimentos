## 1. Proteção e baseline

- [x] 1.1 Criar cópia timestamped da pasta `frontend` e verificar que o backup contém os arquivos Angular atuais
- [x] 1.2 Executar TypeScript, testes e build baseline, registrando falhas preexistentes sem alterar lógica

## 2. Identidade e navegação

- [x] 2.1 Atualizar tokens globais e estilos base para a paleta azul-petróleo/verde, removendo referências roxas e verificando a busca visual
- [x] 2.2 Modernizar visualmente a navegação superior no SCSS encapsulado, preservando links, destinos e menu mobile; verificar em desktop e viewport menor que 720px
- [x] 2.3 Ajustar shell, conteúdo, toasts e foco visível sem manter regras de navegação duplicadas em `app.scss`; verificar compilação

## 3. Telas funcionais existentes

- [x] 3.1 Reorganizar somente o template e estilos do dashboard com cards, painéis, tabela, skeletons e estados vazios, verificando que todos os bindings e ações permanecem presentes
- [x] 3.2 Reorganizar visualmente carteiras e posições mantendo os mesmos campos, métodos CRUD e payloads; verificar cadastro, edição e exclusão
- [x] 3.3 Reorganizar visualmente ativos e histórico mantendo busca, cotação, histórico e estados atuais; verificar cadastro, busca e atualização
- [x] 3.4 Reorganizar visualmente corretoras mantendo máscaras, lookup, payload e tratamento de erro; verificar cadastro e busca

## 4. Responsividade e validação

- [x] 4.1 Aplicar layout responsivo, scroll de tabelas e formulários em uma coluna para desktop, tablet e celular; verificar foco e contraste
- [x] 4.2 Executar TypeScript, suíte de testes e build finais, corrigindo apenas erros de template/estilo
- [x] 4.3 Executar os fluxos manuais de carteira, ativo, corretora, posição, dashboard e histórico com backend ativo e registrar limitações reais (validado em instância isolada na porta 8082; provedores externos podem retornar erro para tickers não suportados)
- [x] 4.4 Confirmar que rotas, services, models, endpoints, payloads e backend não foram modificados e que não existem aliases, wrappers ou identidade roxa antiga
