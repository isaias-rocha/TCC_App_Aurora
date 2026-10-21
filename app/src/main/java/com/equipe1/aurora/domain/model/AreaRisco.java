package com.equipe1.aurora.domain.model;

import com.google.gson.annotations.SerializedName;

public class AreaRisco {
    @SerializedName("id_area_risco")
    private Long idAreaRisco;

    @SerializedName("nome_area")
    private String nomeArea;

    @SerializedName("descricao_area")
    private String descricaoArea;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("raio_metros")
    private double raioMetros;

    @SerializedName("nivel_risco")
    private String nivelRisco;

    @SerializedName("tipo_crime")
    private String tipoCrime;

    @SerializedName("bairro")
    private String bairro;

    // Getters
    public Long getIdAreaRisco() {
        return idAreaRisco;
    }

    public String getNomeArea() {
        return nomeArea;
    }

    public String getDescricaoArea() {
        return descricaoArea;
    }

    public Double getLatitude() {
        return latitude != null ? latitude : 0.0;
    }

    public Double getLongitude() {
        return longitude != null ? longitude : 0.0;
    }

    public double getRaioMetros() {
        return raioMetros;
    }

    public String getNivelRisco() {
        return nivelRisco;
    }

    public String getTipoCrime() {
        return tipoCrime;
    }

    public String getBairro() {
        return bairro;
    }
}