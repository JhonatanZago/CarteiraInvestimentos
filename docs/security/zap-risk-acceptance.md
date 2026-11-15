# Politica de aceitacao de risco — OWASP ZAP

Data da triagem: 2026-11-07  
Escopo: Baseline Scan local, somente `http://host.docker.internal:8081/swagger-ui/index.html` e recursos locais descobertos.  
Scanner: `ghcr.io/zaproxy/zaproxy:2.17.0`.

Este documento e uma proposta de tratamento. Nenhum risco residual abaixo esta aprovado. O Security Gate continua bloqueante para `zap:alerts` e os alertas permanecem nos relatorios HTML, JSON e XML.

Responsavel pela decisao: proprietario do projeto, ainda nao designado nesta execucao.  
Data de revisao proposta: 2027-02-07, ou antes se houver mudanca de Spring Security, Swagger UI, CSP, recursos cross-origin ou exposicao publica da documentacao.

## Resumo

O scan cobriu 13 URLs locais, com 63 PASS, 0 FAIL e 6 entradas WARN em 5 regras ZAP. O alerta de biblioteca vulneravel/DOMPurify foi corrigido anteriormente pela atualizacao para Swagger UI 5.34.1 / DOMPurify 3.4.17 e nao aparece neste relatorio.

## Correcao aplicada nesta rodada

### ZAP-10055-SCRIPT — CSP: script-src unsafe-inline

O alerta foi confirmado como falso positivo operacional para o uso atual: o `index.html` oficial do Swagger UI 5.34.1 possui tres tags `script` externas e nenhum bloco de script inline. `script-src 'unsafe-inline'` foi removido da politica global. O Swagger, seus bundles e a rota protegida responderam normalmente no teste HTTP local; o alerta nao aparece no novo JSON/XML/HTML.

Status: **CORRIGIDO**. A funcionalidade visual completa em navegador permanece limitada porque o host de automacao de navegador nao estava disponivel nesta execucao.

## Itens residuais propostos

### ZAP-10055-STYLE — CSP: style-src unsafe-inline

- ID ZAP: `10055`.
- Severidade/confiança: Medium / High.
- Recurso: `/swagger-ui/index.html`.
- Evidencia sanitizada: `style-src 'self' 'unsafe-inline'`.
- Ocorrencias: 1 entrada no JSON; o console reportou 2 observacoes do recurso.
- Impacto: permite estilos inline e reduz a protecao CSP contra injecao de estilo.
- Probabilidade: baixa no escopo atual.
- Recomendacao oficial: configurar CSP adequadamente sem `unsafe-inline`.
- Controles compensatorios: `style-src` limitado a self, sem `unsafe-eval`, documentacao local, headers de isolamento e ausencia de dados financeiros na pagina.
- Risco residual: baixo a medio e restrito ao Swagger UI.
- Tratamento proposto: nonce/hash ou separacao de politica; nao alterar enquanto a compatibilidade nao for comprovada.
- Condicao de reavaliacao: mudanca do Swagger UI, CSP ou publicacao da documentacao.
- Status: **PROPOSTO** — nao aprovado.

### ZAP-90004-COEP — Cross-Origin-Embedder-Policy Header Missing or Invalid

- ID ZAP: `90004`.
- Severidade/confiança: Low / Medium.
- Recurso: `/swagger-ui/index.html`.
- Evidencia sanitizada: `Cross-Origin-Embedder-Policy: unsafe-none`.
- Ocorrencias: 1 entrada no JSON.
- Impacto: a pagina nao exige isolamento cross-origin completo; `require-corp` pode impedir recursos que nao enviem CORP/CORS apropriado.
- Probabilidade: baixa para o uso atual, que nao requer SharedArrayBuffer ou isolamento cross-origin.
- Recomendacao oficial: usar `require-corp` em documentos quando aplicavel.
- Controles compensatorios: alvo somente local, `Cross-Origin-Opener-Policy: same-origin`, `Cross-Origin-Resource-Policy: same-site`, CSP restritiva e nenhum recurso externo autorizado no DAST.
- Risco residual: baixo; disponibilidade/compatibilidade foi priorizada ate teste especifico de `require-corp`.
- Tratamento proposto: validar em ambiente isolado uma politica por rota antes de qualquer alteracao.
- Condicao de reavaliacao: uso de recursos cross-origin, publicacao do Swagger ou necessidade de isolamento forte.
- Status: **PROPOSTO** — nao aprovado.

### ZAP-90004-CORP — Cross-Origin-Resource-Policy Header Missing or Invalid

- ID ZAP: `90004`.
- Severidade/confiança: Low / Medium.
- Recursos: favicons, `index.css`, `index.html` e `swagger-initializer.js` sob `/swagger-ui/`.
- Evidencia sanitizada: `Cross-Origin-Resource-Policy: same-site`.
- Ocorrencias: 5.
- Impacto: `same-site` e menos restritivo que `same-origin`; alterar para `same-origin` pode afetar recursos servidos por origens autorizadas.
- Probabilidade: baixa no escopo local.
- Recomendacao oficial: preferir `same-origin` para paginas; usar `cross-origin` somente quando compartilhamento for necessario.
- Controles compensatorios: recursos servidos pelo proprio backend, CSP `default-src 'self'`, COOP `same-origin` e ausencia de terceiros no alvo.
- Risco residual: baixo.
- Tratamento proposto: testar `same-origin` somente na rota Swagger e validar Angular/recursos estaticos antes de aplicar.
- Condicao de reavaliacao: mudanca de origem, CDN, Angular ou Swagger.
- Status: **PROPOSTO** — nao aprovado.

### ZAP-10096 — Timestamp Disclosure - Unix

- ID ZAP: `10096`.
- Severidade/confiança: Low / Low.
- Recurso: `/swagger-ui/swagger-ui-standalone-preset.js`.
- Evidencia sanitizada: constantes Unix estaticas `1518500249`, `1732584193`, `1750603025`, `1859775393`, `1894007588`.
- Ocorrencias: 5.
- Impacto: potencial revelacao de metadados da biblioteca; nao ha evidencia de timestamp de negocio, segredo ou dado financeiro.
- Probabilidade: baixa.
- Recomendacao oficial: confirmar manualmente que os timestamps nao sao sensiveis nem agregaveis para um padrao exploravel.
- Controles compensatorios: artefato oficial versionado, sem dados da aplicacao no bundle, acesso local/protegido ao Swagger.
- Risco residual: baixo.
- Tratamento proposto: aceitar tecnicamente somente apos revisao do responsavel; nao editar biblioteca de terceiro.
- Condicao de reavaliacao: mudanca do webjar ou evidencia de metadado da aplicacao.
- Status: **PROPOSTO** — nao aprovado.

### ZAP-10109 — Modern Web Application

- ID ZAP: `10109`.
- Severidade/confiança: Informational / Medium.
- Recurso: `/swagger-ui/index.html`.
- Evidencia sanitizada: carregamento dos bundles JavaScript do Swagger UI.
- Ocorrencias: 1.
- Impacto: nenhum; e uma classificacao de tecnologia e recomendacao de spider.
- Probabilidade: nao aplicavel.
- Recomendacao oficial: nenhuma correcao; usar Client Spider se for necessaria cobertura adicional.
- Controles compensatorios: Baseline limitado e escopo local explicitamente documentado.
- Risco residual: informativo.
- Tratamento proposto: registrar e nao alterar codigo.
- Condicao de reavaliacao: mudanca do metodo DAST ou necessidade de cobertura autenticada.
- Status: **PROPOSTO** — nao aprovado; item informativo.

### ZAP-10049 — Non-Storable Content

- ID ZAP: `10049`.
- Severidade/confiança: Informational / Medium.
- Recursos: `sitemap.xml`, favicons e `index.css` do Swagger UI.
- Evidencia sanitizada: `Cache-Control: no-cache, no-store, max-age=0, must-revalidate`, `Pragma: no-cache`, `Expires: 0`.
- Ocorrencias: 4.
- Impacto: perda de cacheabilidade e possivel impacto de desempenho; nao indica armazenamento indevido de dados.
- Probabilidade: baixa.
- Recomendacao oficial: remover `no-store` somente para respostas publicas nao sensiveis que atendam aos requisitos de cache.
- Controles compensatorios: `no-store` protege respostas autenticadas e dados financeiros; o comportamento global vem do mecanismo de seguranca e nao foi relaxado.
- Risco residual: informativo/baixo, com impacto de desempenho limitado ao escopo local.
- Tratamento proposto: nao alterar globalmente; avaliar politica de cache por rota em mudanca estrutural autorizada.
- Condicao de reavaliacao: necessidade mensuravel de cache, publicacao do Swagger ou separacao formal de recursos publicos.
- Status: **PROPOSTO** — nao aprovado.

## Politica de excecao do gate

Nao foi implementado bypass. Qualquer futura excecao deve exigir identificador ZAP exato, rota/padrao estrito, justificativa, controles compensatorios, responsavel, aprovacao explicita e data de expiracao/revisao. A excecao nao pode abranger todos os WARNs nem alertas High/Critical por categoria. Enquanto nao houver aprovacao formal, `zap:alerts` permanece bloqueante.

## Riscos fora do ZAP

O token BRAPI antigo permanece exposto no historico Git, sem revogacao confirmada. O valor nao foi exibido, testado, reutilizado ou revogado automaticamente.
