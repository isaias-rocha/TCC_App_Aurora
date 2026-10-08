package com.equipe1.aurora.domain.model;

public class Membro {
    public final String nome;
    public final String status;   // ex.: "Em casa · há 2 min"
    public final int bateria;     // 0 - 100
    public final double lat, lon; // última localização (RF018)

    public Membro(String nome, String status, int bateria, double lat, double lon) {
        this.nome = nome;
        this.status = status;
        this.bateria = bateria;
        this.lat = lat;
        this.lon = lon;
    }

    public String iniciais() {
        String[] p = nome.trim().split("\\s+");
        String a = p[0].substring(0, 1);
        String b = p.length > 1 ? p[p.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase();
    }
}