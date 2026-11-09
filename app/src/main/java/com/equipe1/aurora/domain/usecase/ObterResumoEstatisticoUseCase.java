package com.equipe1.aurora.domain.usecase;

import com.equipe1.aurora.domain.model.ResumoEstatistico;
import com.equipe1.aurora.domain.repository.HistoricoRepository;

/**
 * Caso de uso responsável por obter os totais e estatísticas das atividades do usuário.
 */
public class ObterResumoEstatisticoUseCase {

    private final HistoricoRepository repository;

    public interface Callback {
        void onSuccess(ResumoEstatistico resumo);
        void onError(Throwable throwable);
    }

    public ObterResumoEstatisticoUseCase(HistoricoRepository repository) {
        this.repository = repository;
    }

    public void executar(long idUsuario, Callback callback) {
        repository.obterResumoEstatistico(idUsuario, new HistoricoRepository.Callback<ResumoEstatistico>() {
            @Override
            public void onSuccess(ResumoEstatistico resultado) {
                callback.onSuccess(resultado);
            }

            @Override
            public void onError(Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }
}