package com.equipe1.aurora.domain.model;

import com.google.gson.annotations.SerializedName;

public class MembroMapaDTO {
    @SerializedName("idUsuario")
    private Long idUsuario;

    @SerializedName("nome")
    private String nome;

    @SerializedName("fotoUrl")
    private String fotoUrl;

    @SerializedName("nomeCirculo")
    private String nomeCirculo;

    @SerializedName("funcao")
    private String funcao;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("ultimaAtualizacao")
    private String ultimaAtualizacao;

    public Long getIdUsuario() { return idUsuario; }
    public String getNome() { return nome; }
    public String getFotoUrl() { return fotoUrl; }
    public String getNomeCirculo() { return nomeCirculo; }
    public String getFuncao() { return funcao; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getUltimaAtualizacao() { return ultimaAtualizacao; }
}