package com.equipe1.aurora.domain.model;

/**
 * Modelo com o resumo quantitativo das atividades registradas no histórico.
 */
public class ResumoEstatistico {

    private final int totalAlertasSos;
    private final int totalOcorrencias;
    private final double distanciaTotalKm;
    private final int totalPendentesSync; // Total de registos guardados no Room ainda não subidos via ponte API

    public ResumoEstatistico(int totalAlertasSos, int totalOcorrencias, double distanciaTotalKm, int totalPendentesSync) {
        this.totalAlertasSos = totalAlertasSos;
        this.totalOcorrencias = totalOcorrencias;
        this.distanciaTotalKm = distanciaTotalKm;
        this.totalPendentesSync = totalPendentesSync;
    }

    // Getters
    public int getTotalAlertasSos() { return totalAlertasSos; }
    public int getTotalOcorrencias() { return totalOcorrencias; }
    public double getDistanciaTotalKm() { return distanciaTotalKm; }
    public int getTotalPendentesSync() { return totalPendentesSync; }
}