package com.equipe1.aurora.domain.model;

import java.io.Serializable;

/**
 * =====================================================================
 * DISPOSITIVO EMBARCADO
 * =====================================================================
 * Mapeia os dados do dispositivo e o estado da sua
 * última leitura de telemetria/SOS retornada pelo servidor local.
 * Implementa Serializable para permitir o envio do objeto via Intent/Bundle.
 * =====================================================================
 */
public class DispositivoEmbarcado implements Serializable {

    // -----------------------------------------------------------------
    // BLOCO 1: ATRIBUTOS DE IDENTIFICAÇÃO E HARDWARE
    // -----------------------------------------------------------------
    private String deviceUid;        // Identificador único (MAC Address do ESP32)
    private String nome;             // Nome amigável do dispositivo
    private String modelo;           // Modelo do hardware
    private String firmware;         // Versão do firmware compilado

    // -----------------------------------------------------------------
    // BLOCO 2: ATRIBUTOS DE POSICIONAMENTO E TELEMETRIA GPS
    // -----------------------------------------------------------------
    private double latitude;         // Latitude em graus decimais
    private double longitude;        // Longitude em graus decimais
    private Double altitudeM;        // Altitude em metros (pode ser nulo)
    private Double velocidadeKmh;    // Velocidade em km/h (pode ser nulo)
    private Integer satelites;       // Quantidade de satélites conectados (pode ser nulo)

    // -----------------------------------------------------------------
    // BLOCO 3: ATRIBUTOS DE ESTADO DA BATERIA E EMERGÊNCIA
    // -----------------------------------------------------------------
    private int bateria;             // Porcentagem de bateria no momento do registro (0-100%)
    private boolean sos;             // Indica se a leitura foi disparada por acionamento do botão SOS

    // -----------------------------------------------------------------
    // BLOCO 4: ATRIBUTOS TEMPORAIS DE REGISTRO
    // -----------------------------------------------------------------
    private String dataHoraGps;      // Timestamp gerado pelo GPS (Formato: YYYY-MM-DD HH:MM:SS)
    private String recebidoEm;       // Timestamp da recepção no servidor local

    // -----------------------------------------------------------------
    // BLOCO 5: CONSTRUTORES (CONSTRUTOR VAZIO E COMPLETO)
    // -----------------------------------------------------------------
    /**
     * Construtor padrão (necessário para bibliotecas de serialização/JSON)
     */
    public DispositivoEmbarcado() {
    }

    /**
     * Construtor completo com os principais campos de exibição do aplicativo
     */
    public DispositivoEmbarcado(String deviceUid, String nome, double latitude, double longitude,
                                int bateria, boolean sos, String dataHoraGps) {
        this.deviceUid = deviceUid;
        this.nome = nome;
        this.latitude = latitude;
        this.longitude = longitude;
        this.bateria = bateria;
        this.sos = sos;
        this.dataHoraGps = dataHoraGps;
    }

    // -----------------------------------------------------------------
    // BLOCO 6: MÉTODOS GETTERS E SETTERS
    // -----------------------------------------------------------------
    public String getDeviceUid() {
        return deviceUid;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getFirmware() {
        return firmware;
    }

    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getAltitudeM() {
        return altitudeM;
    }

    public void setAltitudeM(Double altitudeM) {
        this.altitudeM = altitudeM;
    }

    public Double getVelocidadeKmh() {
        return velocidadeKmh;
    }

    public void setVelocidadeKmh(Double velocidadeKmh) {
        this.velocidadeKmh = velocidadeKmh;
    }

    public Integer getSatelites() {
        return satelites;
    }

    public void setSatelites(Integer satelites) {
        this.satelites = satelites;
    }

    public int getBateria() {
        return bateria;
    }

    public void setBateria(int bateria) {
        this.bateria = bateria;
    }

    public boolean isSos() {
        return sos;
    }

    public void setSos(boolean sos) {
        this.sos = sos;
    }

    public String getDataHoraGps() {
        return dataHoraGps;
    }

    public void setDataHoraGps(String dataHoraGps) {
        this.dataHoraGps = dataHoraGps;
    }

    public String getRecebidoEm() {
        return recebidoEm;
    }

    public void setRecebidoEm(String recebidoEm) {
        this.recebidoEm = recebidoEm;
    }

    // -----------------------------------------------------------------
    // BLOCO 7: MÉTODOS AUXILIARES E FORMATADORES
    // -----------------------------------------------------------------
    /**
     * Retorna a coordenada formatada para exibição simplificada
     */
    public String getCoordenadasFormatadas() {
        return String.format("Lat: %.6f | Lon: %.6f", latitude, longitude);
    }

    @Override
    public String toString() {
        return "DispositivoEmbarcado{" +
                "deviceUid='" + deviceUid + '\'' +
                ", nome='" + nome + '\'' +
                ", bateria=" + bateria + "%" +
                ", sos=" + sos +
                ", lat=" + latitude +
                ", lng=" + longitude +
                ", dataGps='" + dataHoraGps + '\'' +
                '}';
    }
}