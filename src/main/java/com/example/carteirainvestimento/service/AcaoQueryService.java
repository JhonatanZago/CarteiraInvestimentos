package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcaoQueryService {

    private final AcaoRepository repository;

    public AcaoQueryService(AcaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Acao> list(Pageable pageable) {
        return repository.findAll(pageable);
    }
    @Transactional(readOnly = true) public Acao findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Acao nao encontrada")); }
    @Transactional(readOnly = true) public Acao findByTicker(String ticker) { return repository.findByTicker(ticker.toUpperCase()).orElseThrow(() -> new ResourceNotFoundException("Acao nao encontrada")); }
}
