package com.example.carteirainvestimento.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "corretora", uniqueConstraints = @UniqueConstraint(name = "uk_corretora_cnpj", columnNames = "cnpj"))
@Getter
@Setter
@NoArgsConstructor
public class Corretora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(nullable = false)
    private String razaoSocial;

    private String nomeFantasia;
    private String email;
    private String telefone;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String situacaoCadastral;

    @Column(nullable = false)
    private boolean validadaMercadoFinanceiro;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(length = 32)
    private StatusValidacaoCorretora statusValidacao = StatusValidacaoCorretora.AGUARDANDO_VALIDACAO;

    private String motivoValidacao;

    private OffsetDateTime dataValidacaoMercado;
    private String fonteValidacaoMercado;

    @Column(nullable = false)
    private OffsetDateTime dataCadastro;
}
