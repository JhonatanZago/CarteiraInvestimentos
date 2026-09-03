# Implantação com Angular, Nginx e Spring Boot

## Docker com dados persistentes

Para iniciar o backend e o PostgreSQL em containers:

```powershell
docker compose up --build -d
```

O banco utiliza o volume nomeado `carteira-investimentos-postgres-data`, montado em
`/var/lib/postgresql/data`. Os dados permanecem após `docker compose down` e reinícios
dos containers. Para removê-los deliberadamente, execute `docker compose down --volumes`.

O backend ficará disponível em `http://localhost:8081` (ou na porta definida por `APP_PORT`).

## Desenvolvimento local

Inicie o backend na porta `8081`:

```powershell
.\gradlew.bat bootRun
```

Em outro terminal, inicie o Angular:

```powershell
cd frontend
npm start
```

Abra `http://localhost:4200`. O arquivo `frontend/proxy.conf.json` encaminha as requisições de `/api/v1` para `http://localhost:8081`, portanto o navegador não precisa conhecer a porta do backend.

## Produção

Gere o build do Angular:

```bash
cd frontend
npm run build
```

Publique o conteúdo de `frontend/dist/frontend/browser` em `/usr/share/nginx/html` e instale `nginx/carteira-investimentos.conf` como um server block do Nginx. A configuração:

- serve os arquivos estáticos do Angular;
- retorna `index.html` para rotas da SPA;
- encaminha `/api/v1/` ao Spring Boot em `127.0.0.1:8081`.

O Spring Boot deve permanecer acessível ao Nginx na porta `8081`. Caso o backend esteja em outro host ou porta, atualize somente o valor de `proxy_pass` antes de publicar.

Em uma implantação de mesma origem, não é necessário liberar o domínio público pelo CORS para chamadas da SPA. Se houver outro cliente web em uma origem diferente, defina `CORS_ALLOWED_ORIGINS` com as origens autorizadas, separadas por vírgula.

## Verificação

```bash
npm test
npm run build
curl -i http://localhost/api/v1/mercado/indicadores
```

Com Nginx instalado, valide a configuração antes de recarregá-lo:

```bash
nginx -t
```
