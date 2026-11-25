package com.equipe1.aurora.domain.repository;

import com.equipe1.aurora.domain.model.FiltroHistorico;
import com.equipe1.aurora.domain.model.HistoricoItem;
import com.equipe1.aurora.domain.model.ResumoEstatistico;

import java.util.List;

/**
 * Contrato de repositório para acesso aos dados do Histórico.
 * Implementado na camada de infraestrutura/repositório.
 */
public interface HistoricoRepository {

    // Interfaces para retornos assíncronos (Callbacks)
    interface Callback<T> {
        void onSuccess(T resultado);
        void onError(Throwable throwable);
    }

    /**
     * Obtém os itens do histórico aplicando filtros (combina dados locais do Room com remotos do MySQL).
     */
    void obterHistorico(long idUsuario, FiltroHistorico filtro, Callback<List<HistoricoItem>> callback);

    /**
     * Obtém o resumo estatístico das atividades do utilizador.
     */
    void obterResumoEstatistico(long idUsuario, Callback<ResumoEstatistico> callback);

    /**
     * Força a sincronização pendente do Room para a ponte MySQL
     */
    void sincronizarDadosPendentes(long idUsuario, Callback<Boolean> callback);
}