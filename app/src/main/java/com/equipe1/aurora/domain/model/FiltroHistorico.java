package com.equipe1.aurora.domain.model;

import java.util.Date;

/**
 * Objeto de valor que representa os critérios de filtro aplicados na consulta do Histórico.
 */
public class FiltroHistorico {

    private final HistoricoItem.TipoHistorico tipo;
    private final Date dataInicio;
    private final Date dataFim;
    private final Boolean apenasNaoSincronizados; // Se true, exibe apenas o que está pendente de subir para o MySQL

    public FiltroHistorico(HistoricoItem.TipoHistorico tipo, Date dataInicio, Date dataFim, Boolean apenasNaoSincronizados) {
        this.tipo = tipo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.apenasNaoSincronizados = apenasNaoSincronizados;
    }

    // Getters
    public HistoricoItem.TipoHistorico getTipo() { return tipo; }
    public Date getDataInicio() { return dataInicio; }
    public Date getDataFim() { return dataFim; }
    public Boolean getApenasNaoSincronizados() { return apenasNaoSincronizados; }
}