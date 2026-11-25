package com.equipe1.aurora.infra.dto;

import com.google.gson.annotations.SerializedName;

// DTO para representar cada item retornado dentro do array "dados" do consultar.php
public class DispositivoDto {

    @SerializedName("id")
    private long id;

    @SerializedName("device_uid")
    private String deviceUid;

    @SerializedName("dispositivo_nome")
    private String dispositivoNome;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("altitude_m")
    private Double altitudeM;

    @SerializedName("velocidade_kmh")
    private Double velocidadeKmh;

    @SerializedName("satelites")
    private Integer satelites;

    @SerializedName("bateria")
    private Integer bateria;

    @SerializedName("sos")
    private boolean sos;

    @SerializedName("data_hora_gps")
    private String dataHoraGps;

    // Getters e Setters
    public long getId() { return id; }
    public String getDeviceUid() { return deviceUid; }
    public String getDispositivoNome() { return dispositivoNome; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Double getAltitudeM() { return altitudeM; }
    public Double getVelocidadeKmh() { return velocidadeKmh; }
    public Integer getSatelites() { return satelites; }
    public Integer getBateria() { return bateria; }
    public boolean isSos() { return sos; }
    public String getDataHoraGps() { return dataHoraGps; }
}