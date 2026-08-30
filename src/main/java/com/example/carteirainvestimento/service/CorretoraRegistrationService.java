package com.example.carteirainvestimento.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.dto.corretora.CorretoraCreateRequest;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.integration.adapter.EmpresaAdapter;
import com.example.carteirainvestimento.integration.adapter.EnderecoAdapter;
import com.example.carteirainvestimento.integration.dto.EmpresaConsulta;
import com.example.carteirainvestimento.integration.dto.EnderecoConsulta;
import com.example.carteirainvestimento.integration.dto.InstituicaoFinanceiraConsulta;
import com.example.carteirainvestimento.integration.facade.InstituicaoFinanceiraFacade;
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
    private final InstituicaoFinanceiraFacade instituicaoFinanceiraFacade;

    public CorretoraRegistrationService(CorretoraRepository repository, EmpresaAdapter empresaAdapter,
                                        EnderecoAdapter enderecoAdapter,
                                        InstituicaoFinanceiraFacade instituicaoFinanceiraFacade) {
        this.repository = repository;
        this.empresaAdapter = empresaAdapter;
        this.enderecoAdapter = enderecoAdapter;
        this.instituicaoFinanceiraFacade = instituicaoFinanceiraFacade;
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

        EmpresaConsulta empresa = empresaAdapter.buscarPorCnpj(cnpj);
        if (!"ATIVA".equalsIgnoreCase(empresa.situacaoCadastral())) {
            throw new BusinessRuleException("Empresa nao esta ativa");
        }
        EnderecoConsulta endereco = enderecoAdapter.buscarPorCep(DocumentoNormalizer.cep(request.cep()));
        InstituicaoFinanceiraConsulta instituicao = instituicaoFinanceiraFacade.validarPorCnpj(cnpj);

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
        corretora.setValidadaMercadoFinanceiro(true);
        corretora.setDataValidacaoMercado(instituicao.dataHoraValidacao());
        corretora.setFonteValidacaoMercado(instituicao.fonte());
        corretora.setDataCadastro(OffsetDateTime.now(ZoneOffset.UTC));
        return repository.save(corretora);
    }
}
