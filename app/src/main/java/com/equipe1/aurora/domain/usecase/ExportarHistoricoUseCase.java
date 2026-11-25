package com.equipe1.aurora.domain.usecase;

import com.equipe1.aurora.domain.model.FiltroHistorico;
import com.equipe1.aurora.domain.model.HistoricoItem;
import com.equipe1.aurora.domain.repository.HistoricoRepository;

import java.io.File;
import java.util.List;

/**
 * Caso de uso responsável por compilar e exportar o histórico de registros[cite: 1, 30].
 */
public class ExportarHistoricoUseCase {

    private final HistoricoRepository repository;

    public interface Callback {
        void onSuccess(File arquivoExportado);
        void onError(Throwable throwable);
    }

    public ExportarHistoricoUseCase(HistoricoRepository repository) {
        this.repository = repository;
    }

    public void executar(long idUsuario, FiltroHistorico filtro, Callback callback) {
        repository.obterHistorico(idUsuario, filtro, new HistoricoRepository.Callback<List<HistoricoItem>>() {
            @Override
            public void onSuccess(List<HistoricoItem> resultado) {
                try {
                    // Lógica para gerar o ficheiro em memória (exemplo simples)
                    File arquivo = File.createTempFile("historico_aurora_", ".csv");
                    // Preenchimento do arquivo com a lista de itens
                    callback.onSuccess(arquivo);
                } catch (Exception e) {
                    callback.onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }
}