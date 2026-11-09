package com.equipe1.aurora.domain.model;

import java.util.Date;

/**
 * Modelo de domínio que representa um item unificado no Histórico do Aurora.
 * Consolida registros de Rotas, Alertas SOS (App/Dispositivo) e Ocorrências.
 */
public class HistoricoItem {

    public enum TipoHistorico {
        ROTA,
        ALERTA_SOS,
        OCORRENCIA_DENUNCIA
    }

    private final String id;
    private final TipoHistorico tipo;
    private final String titulo;
    private final String descricao;
    private final Date dataHora;
    private final String origem; // "APP" ou "DISPOSITIVO" (alinhado com o ENUM 'origem' da tabela alerta_sos)
    private final double latitude;
    private final double longitude;
    private final boolean sincronizado; // Indica se já subiu para a ponte MySQL ou se está retido no Room
    private final Integer bateria; // Nível de bateria no momento do registro (opcional)

    public HistoricoItem(
            String id,
            TipoHistorico tipo,
            String titulo,
            String descricao,
            Date dataHora,
            String origem,
            double latitude,
            double longitude,
            boolean sincronizado,
            Integer bateria
    ) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.descricao = descricao;
        this.dataHora = dataHora;
        this.origem = origem;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sincronizado = sincronizado;
        this.bateria = bateria;
    }

    // Getters
    public String getId() { return id; }
    public TipoHistorico getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public Date getDataHora() { return dataHora; }
    public String getOrigem() { return origem; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isSincronizado() { return sincronizado; }
    public Integer getBateria() { return bateria; }
}