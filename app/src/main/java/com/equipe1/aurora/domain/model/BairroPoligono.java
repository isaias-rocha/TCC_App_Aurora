package com.equipe1.aurora.domain.model;

import com.google.gson.annotations.SerializedName;

public class BairroPoligono {

    @SerializedName("idBairro")
    private Long idBairro;

    @SerializedName("nomeBairro")
    private String nomeBairro;

    @SerializedName("nivelRisco")
    private String nivelRisco;

    @SerializedName("coordenadasVertices")
    private String coordenadasVertices;

    public Long getIdBairro() { return idBairro; }
    public String getNomeBairro() { return nomeBairro; }
    public String getNivelRisco() { return nivelRisco; }
    public String getCoordenadasVertices() { return coordenadasVertices; }
}