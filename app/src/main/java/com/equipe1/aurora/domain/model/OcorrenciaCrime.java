package com.equipe1.aurora.domain.model;

import com.google.gson.annotations.SerializedName;

public class OcorrenciaCrime {

    @SerializedName("tipo_crime") // Nome da chave vinda da API JSON
    private String tipoCrime;

    @SerializedName("descricao")
    private String descricao;

    @SerializedName("latitude")
    private double latitude; // Gson converte string ou number JSON para double

    @SerializedName("longitude")
    private double longitude;

    // Construtor vazio (necessário para o Gson)
    public OcorrenciaCrime() {}

    // Getters e Setters retornando double nativo
    public String getTipoCrime() {
        return tipoCrime;
    }

    public String getDescricao() {
        return descricao;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}