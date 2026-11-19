# Roteiro de apresentacao - Aula 15

Duracao sugerida: 5 a 8 minutos.

## 1. Abertura - 30 segundos

Apresente o objetivo: mostrar como o projeto usa testes automatizados, TDD e
JaCoCo para reduzir regressao e produzir evidencias verificaveis.

## 2. TDD - 1 minuto

Explique o ciclo:

1. RED: escrever um teste que falha por um comportamento ausente ou incorreto.
2. GREEN: implementar a menor correcao para passar.
3. REFACTOR: reorganizar sem alterar o comportamento.

Destaque que um teste que passa desde o inicio e Green-first, nao um ciclo RED
comprovado.

## 3. Exemplo financeiro - 1 minuto

Mostre `MovimentacaoFinanceiraService`:

- Uma taxa cambial negativa era aceita indevidamente.
- O teste falhou primeiro.
- A validacao foi adicionada.
- A regra foi extraida para `validarCambioInformado`.

Esse e o primeiro ciclo RED -> GREEN -> REFACTOR comprovado.

## 4. Exemplo de relogio - 1 minuto

Mostre `ExchangeRateService`:

- O teste usa um `Clock` fixo.
- A implementacao comparava com o relogio global e tratava a cotacao como
  futura.
- A correcao usa o `Clock` injetado.

Esse e o segundo ciclo RED -> GREEN comprovado. Nao apresente um terceiro ciclo
como se existisse.

## 5. Executar os testes - 45 segundos

Use o terminal:

```text
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Explique que a suite atual tem 241 testes, nenhum erro ou falha e um teste
condicional ignorado por exigir PostgreSQL externo.

## 6. Abrir JaCoCo - 45 segundos

Abra:

```text
build/reports/jacoco/test/html/index.html
```

Mostre o resumo do bundle e, depois, uma classe com decisoes. O XML em
`build/reports/jacoco/test/jacocoTestReport.xml` serve para integracao e
automacao; o HTML facilita a explicacao visual.

## 7. Interpretar os numeros - 1 minuto

Resultado atual:

- Linhas: 89,56%.
- Ramos: 70,98%, ou 746 de 1.051.
- Portao minimo: 80% de linhas e 55% de ramos, aprovado.
- Meta academica adicional: 80% de ramos, ainda pendente.

Explique que linhas cobertas nao significam que todos os caminhos de decisao
foram testados.

## 8. Encerramento - 30 segundos

Conclua que o projeto possui suite automatizada, dois ciclos TDD RED legitimos,
relatorios reproduziveis e portao minimo aprovado. As pendencias sao atingir
80% de ramos, habilitar o smoke test em PostgreSQL controlado e apresentar os
materiais/evidencias externas exigidos pela disciplina.
