# Cobertura inicial do backend

## Execução

Ambiente utilizado:

- Java 21.0.12.1 (Eclipse Adoptium).
- Gradle Wrapper 9.5.1.
- Cache Gradle: `.gradle-user-home-validation`.

Comandos executados:

```powershell
.\gradlew.bat --no-daemon test jacocoTestReport --console=plain
.\gradlew.bat --offline --no-daemon jacocoTestCoverageVerification --console=plain
```

A primeira tentativa offline do `test` ficou `BLOCKED` porque o cache não
possuía `org.jacoco:org.jacoco.agent:0.8.14`. A resolução oficial pelo
Maven Central configurado permitiu executar a medição; nenhum artefato foi
copiado manualmente.

## Resultado dos testes

- 174 testes executados.
- 0 falhas.
- 1 teste ignorado.
- `test` e `jacocoTestReport`: PASS.

## Cobertura medida

Valores extraídos de `build/reports/jacoco/test/jacocoTestReport.xml`:

| Métrica | Cobertura |
|---|---:|
| Instruções | 71,49% (8.363 / 11.698) |
| Linhas | 72,69% (1.166 / 1.604) |
| Métodos | 77,49% (389 / 502) |
| Classes | 92,41% (134 / 145) |
| Ramos | 48,90% (511 / 1.045) |

O relatório HTML está em:

`build/reports/jacoco/test/html/index.html`

Também foram gerados os relatórios XML e CSV no diretório
`build/reports/jacoco/test/`.

## Portão de qualidade

Foram configurados os limites acadêmicos iniciais, sem reduzi-los:

- Linhas: 80%.
- Ramos: 55%.

`jacocoTestCoverageVerification`: **FAIL**, pois a medição atual ficou em
72,69% de linhas e 48,90% de ramos. O relatório continua sendo gerado antes
da verificação.

A meta acadêmica final de 80% de linhas e 80% de ramos ainda não foi atingida.

## Prioridades para os próximos ciclos TDD

1. **MovimentacaoFinanceiraService** — linha 10,6% e ramo 0%.
   - Positivo: compra/venda com câmbio disponível e valores válidos.
   - Negativo: câmbio indisponível, quantidade superior ao saldo e moeda
     incompatível.
   - Fronteiras: quantidade zero, venda integral e taxa cambial exatamente
     no limite aceito.
   - Prioridade: concentra regras financeiras e decisões pouco cobertas.

2. **UsuarioAdministracaoService** — linha 85,5% e ramo 45,8%.
   - Positivo: ADMIN cria usuário USER, altera papel e troca senha.
   - Negativo: USER recebe 403, e-mail duplicado e último ADMIN não pode ser
     desativado/rebaixado.
   - Fronteiras: ID inexistente, senha divergente e exatamente um ADMIN ativo.
   - Prioridade: regras RBAC críticas ainda têm ramos não exercitados.

3. **AuthTokenService** — linha 94,9% e ramo 77,8%.
   - Positivo: JWT HS256 válido com emissor, authorities e expiração.
   - Negativo: segredo ausente/curto, placeholder, emissor inválido, token
     adulterado ou expirado.
   - Fronteiras: segredo com 31/32 caracteres e expiração no limite.
   - Prioridade: autenticação é uma barreira transversal de segurança.

## Próximos passos

- Implementar um ciclo TDD por comportamento, começando por
  `MovimentacaoFinanceiraService`.
- Cobrir ramos negativos de administração e autorização.
- Manter os testes JWT e ownership como regressão obrigatória.
- Reexecutar `jacocoTestReport` e `jacocoTestCoverageVerification` após cada
  ciclo.
- Atingir primeiro 80% de linhas e 55% de ramos; depois evoluir para 80% de
  ramos sem exclusões artificiais.
