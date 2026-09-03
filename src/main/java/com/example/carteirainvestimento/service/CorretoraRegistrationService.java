package com.example.carteirainvestimento.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.domain.StatusValidacaoCorretora;
import com.example.carteirainvestimento.dto.corretora.CorretoraCreateRequest;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.integration.adapter.EmpresaAdapter;
import com.example.carteirainvestimento.integration.adapter.EnderecoAdapter;
import com.example.carteirainvestimento.integration.dto.EmpresaConsulta;
import com.example.carteirainvestimento.integration.dto.EnderecoConsulta;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import com.example.carteirainvestimento.validation.CnpjValidator;
import com.example.carteirainvestimento.validation.DocumentoNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorretoraRegistrationService {

    private final CorretoraRepository repository;
    private final EmpresaAdapter empresaAdapter;
    private final EnderecoAdapter enderecoAdapter;

    public CorretoraRegistrationService(CorretoraRepository repository, EmpresaAdapter empresaAdapter,
                                        EnderecoAdapter enderecoAdapter) {
        this.repository = repository;
        this.empresaAdapter = empresaAdapter;
        this.enderecoAdapter = enderecoAdapter;
    }

    @Transactional
    public Corretora registrar(CorretoraCreateRequest request) {
        String cnpj = DocumentoNormalizer.cnpj(request.cnpj());
        if (!CnpjValidator.isValid(cnpj)) {
            throw new BusinessRuleException("CNPJ invalido");
        }
        if (repository.existsByCnpj(cnpj)) {
            throw new DuplicateResourceException("CNPJ ja cadastrado");
        }

        EmpresaConsulta empresa;
        StatusValidacaoCorretora status = StatusValidacaoCorretora.VALIDADA;
        String motivo = null;
        try {
            empresa = empresaAdapter.buscarPorCnpj(cnpj);
            if (!"ATIVA".equalsIgnoreCase(empresa.situacaoCadastral())) {
                throw new BusinessRuleException("Empresa nao esta ativa");
            }
        } catch (ExternalIntegrationException externalFailure) {
            // O cadastro continua pendente quando os provedores externos estão indisponíveis.
            status = StatusValidacaoCorretora.FONTE_INDISPONIVEL;
            motivo = externalFailure.getMessage();
            empresa = new EmpresaConsulta(cnpj, "Empresa não identificada", null, null, null, "PENDENTE");
        }

        EnderecoConsulta endereco;
        try {
            endereco = enderecoAdapter.buscarPorCep(DocumentoNormalizer.cep(request.cep()));
        } catch (ExternalIntegrationException | ResourceNotFoundException ignored) {
            endereco = new EnderecoConsulta(DocumentoNormalizer.cep(request.cep()), null, null, null, null);
        }

        Corretora corretora = new Corretora();
        corretora.setCnpj(cnpj);
        corretora.setRazaoSocial(empresa.razaoSocial());
        corretora.setNomeFantasia(empresa.nomeFantasia());
        corretora.setEmail(empresa.email());
        corretora.setTelefone(empresa.telefone());
        corretora.setCep(endereco.cep());
        corretora.setLogradouro(endereco.logradouro());
        corretora.setNumero(request.numero());
        corretora.setComplemento(request.complemento());
        corretora.setBairro(endereco.bairro());
        corretora.setCidade(endereco.cidade());
        corretora.setUf(endereco.uf());
        corretora.setSituacaoCadastral(empresa.situacaoCadastral());
        corretora.setValidadaMercadoFinanceiro(status == StatusValidacaoCorretora.VALIDADA);
        corretora.setStatusValidacao(status);
        corretora.setMotivoValidacao(motivo);
        corretora.setDataValidacaoMercado(status == StatusValidacaoCorretora.VALIDADA
                ? OffsetDateTime.now(ZoneOffset.UTC) : null);
        corretora.setFonteValidacaoMercado("BRASIL_API");
        corretora.setDataCadastro(OffsetDateTime.now(ZoneOffset.UTC));
        return repository.save(corretora);
    }

    @Transactional
    public Corretora revalidar(Long id) {
        Corretora corretora = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Corretora nao encontrada"));
        EmpresaConsulta empresa = empresaAdapter.buscarPorCnpj(corretora.getCnpj());
        if (!"ATIVA".equalsIgnoreCase(empresa.situacaoCadastral())) {
            corretora.setStatusValidacao(StatusValidacaoCorretora.NAO_AUTORIZADA);
            corretora.setValidadaMercadoFinanceiro(false);
            corretora.setMotivoValidacao("Empresa inativa: " + empresa.situacaoCadastral());
        } else {
            corretora.setStatusValidacao(StatusValidacaoCorretora.VALIDADA);
            corretora.setValidadaMercadoFinanceiro(true);
            corretora.setMotivoValidacao(null);
        }
        corretora.setRazaoSocial(empresa.razaoSocial());
        corretora.setNomeFantasia(empresa.nomeFantasia());
        corretora.setSituacaoCadastral(empresa.situacaoCadastral());
        corretora.setDataValidacaoMercado(OffsetDateTime.now(ZoneOffset.UTC));
        corretora.setFonteValidacaoMercado("BRASIL_API");
        return repository.save(corretora);
    }
}
