## 1. Proxy de desenvolvimento Angular

- [x] 1.1 Adicionar a configuração de proxy para encaminhar `/api/v1` ao Spring Boot local e verificar que o arquivo é reconhecido pelo comando de serve.
- [x] 1.2 Alterar o ambiente de desenvolvimento Angular para usar a base relativa `/api/v1` e verificar com teste de serviço HTTP que a URL solicitada é relativa.
- [x] 1.3 Configurar o target de serve para usar o proxy e verificar, com Angular e Spring Boot ativos, que uma chamada à API via `http://localhost:4200/api/v1/**` retorna a resposta do backend.

## 2. Proxy de produção

- [x] 2.1 Adicionar uma configuração Nginx versionada que sirva o build Angular, faça fallback de rotas da SPA e encaminhe `/api/v1/` para o Spring Boot com cabeçalhos encaminhados; verificar a sintaxe da configuração quando Nginx estiver disponível.
- [x] 2.2 Documentar o fluxo de build, a publicação dos arquivos estáticos, o upstream Spring Boot e as variáveis de origem permitida; verificar que a documentação não contém credenciais.

## 3. Verificação de integração

- [x] 3.1 Executar os testes e o build Angular e verificar que não há URL absoluta de backend nos arquivos de ambiente do navegador.
- [x] 3.2 Executar o smoke test de desenvolvimento com os dois serviços e verificar que cadastro ou consulta via proxy não apresenta erro de CORS, conexão ou rota.
