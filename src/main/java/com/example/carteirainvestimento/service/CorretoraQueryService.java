package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorretoraQueryService {

    private final CorretoraRepository repository;

    public CorretoraQueryService(CorretoraRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Corretora> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Corretora findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Corretora nao encontrada"));
    }

    @Transactional(readOnly = true)
    public Corretora findByCnpj(String cnpj) {
        return repository.findByCnpj(cnpj).orElseThrow(() -> new ResourceNotFoundException("Corretora nao encontrada"));
    }
}
