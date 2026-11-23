package com.equipe1.aurora.domain.model;

import com.google.gson.annotations.SerializedName;

public class AlertaSos {
    @SerializedName("idAlerta")
    private Long idAlerta;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("origem")
    private String origem;

    @SerializedName("status")
    private String status;

    @SerializedName("usuario")
    private UsuarioDto usuario;

    public Long getIdAlerta() { return idAlerta; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getOrigem() { return origem; }
    public String getStatus() { return status; }
    public UsuarioDto getUsuario() { return usuario; }
}