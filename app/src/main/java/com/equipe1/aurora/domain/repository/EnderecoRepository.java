package com.equipe1.aurora.domain.repository;

import com.equipe1.aurora.domain.model.Endereco;

// Contrato da camada de domínio para busca de endereço por CEP
public interface EnderecoRepository {

    // Callback assíncrono para retornar o resultado da consulta à camada superior
    interface ConsultaCepCallback {
        void onSucesso(Endereco endereco);
        void onErro(String mensagemErro);
    }

    void consultarCep(String cep, ConsultaCepCallback callback);
}