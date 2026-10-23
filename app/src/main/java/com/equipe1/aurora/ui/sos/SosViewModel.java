package com.equipe1.aurora.ui.sos;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.equipe1.aurora.infra.local.AppDataBase;
import com.equipe1.aurora.infra.local.entity.AlertaSosEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel responsável pela regra de negócio e gerenciamento dos alertas de emergência SOS.
 */
public class SosViewModel extends AndroidViewModel {

    // Instância do banco de dados local (Room)
    private final AppDataBase db;

    // Thread em segundo plano para não congelar a interface gráfica (UI Thread)
    private final ExecutorService executorService;

    // Estado observável para notificar a View sobre o sucesso do disparo do alerta
    private final MutableLiveData<Boolean> _alertaEnviadoComSucesso = new MutableLiveData<>();
    public final LiveData<Boolean> alertaEnviadoComSucesso = _alertaEnviadoComSucesso;

    // Estado observável para envio de mensagens de erro à View
    private final MutableLiveData<String> _mensagemErro = new MutableLiveData<>();
    public final LiveData<String> mensagemErro = _mensagemErro;

    public SosViewModel(@NonNull Application application) {
        super(application);
        // Inicializa o Room Database e o Executor de tarefas assíncronas
        this.db = AppDataBase.getInstance(application);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Executa a regra de negócio para registrar o alerta de emergência na base local (Room)
     * com origem 'APP' [RF026 / Tabela Alerta_SOS][cite: 28, 74].
     */
    public void enviarAlertaSos(double latitude, double longitude) {
        executorService.execute(() -> {
            try {
                // Cria a entidade do alerta SOS com origem 'APP' e status 'ATIVO'
                AlertaSosEntity alerta = new AlertaSosEntity(
                        System.currentTimeMillis(), // Timestamp do momento do disparo
                        latitude,
                        longitude,
                        "APP",                // Origem do disparo (Aplicativo móvel)
                        "ATIVO",                    // Status inicial
                        null,                       // DispositivoId (null pois foi via tela do App)
                        false                       // Sincronizado = false (pendente de envio para o MySQL via API)
                );

                // Inserção assíncrona na tabela local do Room
                db.alertaSosDao().inserir(alerta);

                // Notifica a View que o alerta foi registrado com sucesso
                _alertaEnviadoComSucesso.postValue(true);

            } catch (Exception e) {
                // Notifica mensagem de erro em caso de falha na gravação local
                _mensagemErro.postValue("Erro ao registrar o alerta SOS na base de dados.");
                _alertaEnviadoComSucesso.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Encerra a thread de segundo plano para evitar vazamentos de memória
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}