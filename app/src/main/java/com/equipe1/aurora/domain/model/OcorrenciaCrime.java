package com.equipe1.aurora.domain.model;

import com.google.gson.annotations.SerializedName;

public class OcorrenciaCrime {

    @SerializedName("tipoCrime")
    private String tipoCrime;

    @SerializedName("descricao")
    private String descricao;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    // Construtor vazio (necessário para o Gson)
    public OcorrenciaCrime() {}

    // Construtor usado para enviar uma nova ocorrência
    public OcorrenciaCrime(String tipoCrime, String descricao, double latitude, double longitude) {
        this.tipoCrime = tipoCrime;
        this.descricao = descricao;
        this.latitude = latitude;
        this.longitude = longitude;
    }

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