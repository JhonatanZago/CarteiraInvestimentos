package com.example.carteirainvestimento.mapper;

import com.example.carteirainvestimento.domain.Carteira;
import com.example.carteirainvestimento.dto.carteira.CarteiraResponse;

public final class CarteiraMapper {

    private CarteiraMapper() {
    }

    public static CarteiraResponse toResponse(Carteira carteira) {
        return new CarteiraResponse(carteira.getId(), carteira.getNome(), carteira.getDescricao(), carteira.getDataCriacao());
    }
}
