package com.equipe1.aurora.infra.repository;

import com.equipe1.aurora.domain.model.FiltroHistorico;
import com.equipe1.aurora.domain.model.HistoricoItem;
import com.equipe1.aurora.domain.model.ResumoEstatistico;
import com.equipe1.aurora.domain.repository.HistoricoRepository;
import com.equipe1.aurora.infra.dto.ConsultaResponseDto;
import com.equipe1.aurora.infra.dto.mapper.HistoricoMapper;
import com.equipe1.aurora.infra.local.dao.AlertaSosDao;
import com.equipe1.aurora.infra.local.entity.AlertaSosEntity;
import com.equipe1.aurora.infra.network.api.ApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Implementação do repositório de histórico unificando Room e Retrofit
 * com as assinaturas reais da ApiService
 */
public class HistoricoRepositoryImpl implements HistoricoRepository {

    private final AlertaSosDao alertaSosDao;
    private final ApiService apiService;
    private final ExecutorService executorService;

    public HistoricoRepositoryImpl(AlertaSosDao alertaSosDao, ApiService apiService) {
        this.alertaSosDao = alertaSosDao;
        this.apiService = apiService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void obterHistorico(long idUsuario, FiltroHistorico filtro, Callback<List<HistoricoItem>> callback) {
        executorService.execute(() -> {
            try {
                List<HistoricoItem> resultadoCombinado = new ArrayList<>();

                // 1. Obtém dados offline pendentes armazenados no Room local
                List<AlertaSosEntity> alertasLocais = alertaSosDao.obterNaoSincronizados();
                resultadoCombinado.addAll(HistoricoMapper.paraListaDominioDasEntidades(alertasLocais));

                // 2. Consulta o servidor MySQL usando o endpoint consultar.php com X-API-Key
                Call<ConsultaResponseDto> call = apiService.consultarDispositivos(100, "DESC");
                Response<ConsultaResponseDto> response = call.execute();

                if (response.isSuccessful() && response.body() != null && response.body().isSucesso()) {
                    List<HistoricoItem> remotos = HistoricoMapper.paraListaDominioDosDtos(response.body().getDados());
                    resultadoCombinado.addAll(remotos);
                }

                callback.onSuccess(resultadoCombinado);

            } catch (Exception e) {
                // Em caso de falta de conexão, devolve os dados locais guardados no Room
                try {
                    List<AlertaSosEntity> alertasLocais = alertaSosDao.obterNaoSincronizados();
                    callback.onSuccess(HistoricoMapper.paraListaDominioDasEntidades(alertasLocais));
                } catch (Exception ex) {
                    callback.onError(ex);
                }
            }
        });
    }

    @Override
    public void obterResumoEstatistico(long idUsuario, Callback<ResumoEstatistico> callback) {
        executorService.execute(() -> {
            try {
                List<AlertaSosEntity> pendentes = alertaSosDao.obterNaoSincronizados();
                int totalPendentes = pendentes.size();

                ResumoEstatistico resumo = new ResumoEstatistico(
                        totalPendentes,
                        0,
                        0.0,
                        totalPendentes
                );
                callback.onSuccess(resumo);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    @Override
    public void sincronizarDadosPendentes(long idUsuario, Callback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                List<AlertaSosEntity> pendentes = alertaSosDao.obterNaoSincronizados();

                for (AlertaSosEntity alerta : pendentes) {
                    Call<Void> call = apiService.sincronizarAlertaOffline(alerta);
                    Response<Void> response = call.execute();

                    if (response.isSuccessful()) {
                        alertaSosDao.marcarComoSincronizado(alerta.getId()); // Atualiza no Room após envio para o MySQL
                    }
                }
                callback.onSuccess(true);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
}