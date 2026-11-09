package com.equipe1.aurora.domain.usecase;

import com.equipe1.aurora.domain.model.FiltroHistorico;
import com.equipe1.aurora.domain.model.HistoricoItem;
import com.equipe1.aurora.domain.repository.HistoricoRepository;

import java.util.List;

/**
 * Caso de uso responsável por buscar e filtrar os registros do Histórico.
 */
public class ObterHistoricoUseCase {

    private final HistoricoRepository repository;

    public interface Callback {
        void onSuccess(List<HistoricoItem> itens);
        void onError(Throwable throwable);
    }

    public ObterHistoricoUseCase(HistoricoRepository repository) {
        this.repository = repository;
    }

    public void executar(long idUsuario, FiltroHistorico filtro, Callback callback) {
        repository.obterHistorico(idUsuario, filtro, new HistoricoRepository.Callback<List<HistoricoItem>>() {
            @Override
            public void onSuccess(List<HistoricoItem> resultado) {
                // Aqui podem ser aplicadas regras de negócio adicionais antes de enviar à UI
                callback.onSuccess(resultado);
            }

            @Override
            public void onError(Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }
}