package com.equipe1.aurora.infra.dto;

import com.google.gson.annotations.SerializedName;

// DTO que espelha o JSON retornado pela API pública da ViaCEP
public class ViaCepResponseDto {

    @SerializedName("cep")
    private String cep;

    @SerializedName("logradouro")
    private String logradouro;

    @SerializedName("bairro")
    private String bairro;

    // Na ViaCEP, o nome da cidade vem no campo "localidade"
    @SerializedName("localidade")
    private String localidade;

    // Na ViaCEP, a sigla do estado vem no campo "uf"
    @SerializedName("uf")
    private String uf;

    // A ViaCEP retorna HTTP 200 com {"erro": true} quando o CEP não existe na base
    @SerializedName("erro")
    private boolean erro;

    public String getCep() { return cep; }
    public String getLogradouro() { return logradouro; }
    public String getBairro() { return bairro; }
    public String getLocalidade() { return localidade; }
    public String getUf() { return uf; }
    public boolean isErro() { return erro; }
}