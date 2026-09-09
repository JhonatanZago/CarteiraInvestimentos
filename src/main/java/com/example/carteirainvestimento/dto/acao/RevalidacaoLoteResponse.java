package com.example.carteirainvestimento.dto.acao;

import java.util.List;

public record RevalidacaoLoteResponse(int processados, int atualizados, int semLogo, List<String> erros,
                                      List<RevalidacaoDetalhe> detalhes) {
    public RevalidacaoLoteResponse(int processados, int atualizados, int semLogo, List<String> erros) {
        this(processados, atualizados, semLogo, erros, List.of());
    }
}
