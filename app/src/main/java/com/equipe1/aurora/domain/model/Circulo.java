package com.equipe1.aurora.domain.model;

import com.google.gson.annotations.SerializedName;

public class Circulo {
    @SerializedName("idCirculo")
    private Long idCirculo;

    @SerializedName("codigoConvite")
    private String codigoConvite;

    @SerializedName("nome")
    private String nome;

    public Long getIdCirculo() { return idCirculo; }
    public String getCodigoConvite() { return codigoConvite; }
    public String getNome() { return nome; }
}