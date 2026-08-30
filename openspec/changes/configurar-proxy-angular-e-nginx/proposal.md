## Why

O frontend desenvolvido em uma origem diferente da API depende de CORS e falha quando a origem ou a porta não estão alinhadas. Em produção, o build Angular usa `/api/v1`, mas ainda não há uma configuração versionada que encaminhe esse caminho ao Spring Boot.

## What Changes

- Configurar o servidor de desenvolvimento Angular para encaminhar `/api/v1` ao Spring Boot local.
- Usar o caminho relativo `/api/v1` nos ambientes Angular, eliminando o endereço absoluto da API no navegador.
- Adicionar uma configuração Nginx de produção que sirva a SPA e faça proxy de `/api/v1` para o Spring Boot.
- Documentar os comandos e variáveis de implantação necessários para esse fluxo.

## Capabilities

### New Capabilities

- `frontend-api-proxy`: Encaminhamento consistente de chamadas versionadas da SPA ao backend no desenvolvimento e na produção.

### Modified Capabilities

- `investment-platform-api`: A fronteira Angular–API passa a ser acessada pelo caminho relativo versionado, atrás de um proxy de mesma origem em produção.

## Impact

Afeta os ambientes e a configuração de serve do Angular, adiciona arquivos de configuração Nginx e documentação de implantação. Não altera os contratos ou as regras de negócio da API Spring Boot.
