package com.equipe1.aurora.ui.dispositivo;

// =====================================================================
// AURORA GPS - ESTRUTURA DO DISPOSITIVO (DispositivoItem.java)
// =====================================================================
// Armazena todas as propriedades de telemetria, estado de expansão
// e ativação de cada rastreador pareado na lista.
// =====================================================================

public class DispositivoItem {
    private String macAddress;
    private String nome;
    private boolean conectado;
    private boolean ativo; // Controle de visibilidade para Círculos / SOS
    private boolean expandido; // Controle do estado do painel "Mais detalhes"
    private int nivelBateria;
    private String statusGps;
    private double latitude;
    private double longitude;
    private double altitude;
    private double velocidade;
    private int satelites;

    public DispositivoItem(String macAddress, String nome) {
        this.macAddress = macAddress;
        this.nome = nome;
        this.conectado = false;
        this.ativo = true;
        this.expandido = false;
        this.nivelBateria = 100;
        this.statusGps = "Aguardando...";
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.altitude = 0.0;
        this.velocidade = 0.0;
        this.satelites = 0;
    }

    // BLOCO DE GETTERS E SETTERS
    public String getMacAddress() { return macAddress; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public boolean isConectado() { return conectado; }
    public void setConectado(boolean conectado) { this.conectado = conectado; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public boolean isExpandido() { return expandido; }
    public void setExpandido(boolean expandido) { this.expandido = expandido; }

    public int getNivelBateria() { return nivelBateria; }
    public void setNivelBateria(int nivelBateria) { this.nivelBateria = nivelBateria; }

    public String getStatusGps() { return statusGps; }
    public void setStatusGps(String statusGps) { this.statusGps = statusGps; }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public void setCoordenadas(double lat, double lng) {
        this.latitude = lat;
        this.longitude = lng;
    }

    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    public double getVelocidade() { return velocidade; }
    public void setVelocidade(double velocidade) { this.velocidade = velocidade; }

    public int getSatelites() { return satelites; }
    public void setSatelites(int satelites) { this.satelites = satelites; }
}