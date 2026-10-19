package com.equipe1.aurora.ui.dispositivo;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.equipe1.aurora.domain.model.DispositivoEmbarcado;
import com.equipe1.aurora.infra.ble.AuroraBleService;
import com.equipe1.aurora.infra.dto.ConsultaResponseDto;
import com.equipe1.aurora.infra.dto.DispositivoDto;
import com.equipe1.aurora.infra.dto.mapper.DispositivoMapper;
import com.equipe1.aurora.infra.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * =====================================================================
 * AURORA GPS - VIEWMODEL DA TELA DE DISPOSITIVOS
 * =====================================================================
 * Ponte reativa responsável por manter os dados da interface ativos durante
 * rotações do ecrã, controlar a comunicação com o serviço BLE e integrar
 * a consulta ao servidor local (consultar.php) via Retrofit.
 * =====================================================================
 */
public class DispositivoViewModel extends AndroidViewModel implements AuroraBleService.ServiceCallback {

    private static final String TAG = "DispositivoViewModel";

    // Instância do serviço de ligação BLE e indicador de vinculação
    private AuroraBleService bleService;
    private boolean serviceBound = false;

    // -----------------------------------------------------------------
    // BLOCO 1: LIVEDATAS DE CONEXÃO E TELEMETRIA BLE (TEMPO REAL)
    // -----------------------------------------------------------------
    private final MutableLiveData<Boolean> estaConectado = new MutableLiveData<>(false);
    private final MutableLiveData<String> nomeDispositivo = new MutableLiveData<>("Localizador Aurora");
    private final MutableLiveData<Integer> nivelBateria = new MutableLiveData<>(100);
    private final MutableLiveData<String> statusGps = new MutableLiveData<>("Aguardando sinal...");
    private final MutableLiveData<double[]> ultimasCoordenadas = new MutableLiveData<>(null);
    private final MutableLiveData<double[]> eventoSosAcionado = new MutableLiveData<>(null);
    private final MutableLiveData<List<BluetoothDevice>> dispositivosEncontrados = new MutableLiveData<>(new ArrayList<>());

    // -----------------------------------------------------------------
    // BLOCO 2: LIVEDATAS DA API REST (HISTÓRICO MYSQL)
    // -----------------------------------------------------------------
    private final MutableLiveData<List<DispositivoEmbarcado>> historicoBancoLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> carregandoApiLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> mensagemErroApiLiveData = new MutableLiveData<>(null);

    // -----------------------------------------------------------------
    // BLOCO 3: CONSTRUTOR DA CLASSE
    // -----------------------------------------------------------------
    public DispositivoViewModel(@NonNull Application application) {
        super(application);
    }

    // -----------------------------------------------------------------
    // BLOCO 4: EXPOSIÇÃO DE GETTERS IMUTÁVEIS (LIVEDATA) PARA O FRAGMENT
    // -----------------------------------------------------------------
    public LiveData<Boolean> getEstaConectado() { return estaConectado; }
    public LiveData<String> getNomeDispositivo() { return nomeDispositivo; }
    public LiveData<Integer> getNivelBateria() { return nivelBateria; }
    public LiveData<String> getStatusGps() { return statusGps; }
    public LiveData<double[]> getUltimasCoordenadas() { return ultimasCoordenadas; }
    public LiveData<double[]> getEventoSosAcionado() { return eventoSosAcionado; }
    public LiveData<List<BluetoothDevice>> getDispositivosEncontrados() { return dispositivosEncontrados; }

    public LiveData<List<DispositivoEmbarcado>> getHistoricoBancoLiveData() { return historicoBancoLiveData; }
    public LiveData<Boolean> getCarregandoApiLiveData() { return carregandoApiLiveData; }
    public LiveData<String> getMensagemErroApiLiveData() { return mensagemErroApiLiveData; }

    // -----------------------------------------------------------------
    // BLOCO 5: MÉTODOS DE REQUISIÇÃO À API REST (consultar.php)
    // -----------------------------------------------------------------
    /**
     * Executa a busca assíncrona dos últimos registos de localização
     * guardados na base de dados local MySQL através do Retrofit.
     */
    public void carregarDadosDoBancoServidor() {
        carregandoApiLiveData.setValue(true);

        ApiClient.getApiService().consultarDispositivos(50, "desc").enqueue(new Callback<ConsultaResponseDto>() {
            @Override
            public void onResponse(Call<ConsultaResponseDto> call, Response<ConsultaResponseDto> response) {
                carregandoApiLiveData.setValue(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSucesso()) {
                    List<DispositivoDto> listaDto = response.body().getDados();
                    List<DispositivoEmbarcado> listaDominio = DispositivoMapper.paraListaDominio(listaDto);

                    historicoBancoLiveData.setValue(listaDominio);
                } else {
                    mensagemErroApiLiveData.setValue("Erro ao consultar servidor local. Código: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ConsultaResponseDto> call, Throwable t) {
                carregandoApiLiveData.setValue(false);
                Log.e(TAG, "Falha na requisição HTTP: " + t.getMessage(), t);
                mensagemErroApiLiveData.setValue("Falha de ligação com o servidor local: " + t.getMessage());
            }
        });
    }

    // -----------------------------------------------------------------
    // BLOCO 6: INICIALIZAÇÃO E GESTÃO DO SERVIÇO DE SEGUNDO PLANO (BLE)
    // -----------------------------------------------------------------
    /**
     * Limpa a lista interna de dispositivos BLE encontrados durante a varredura.
     */
    public void limparListaDispositivos() {
        dispositivosEncontrados.setValue(new ArrayList<>());
    }

    /**
     * Inicia o Foreground Service e estabelece a vinculação segura (bind) com a aplicação.
     */
    public void iniciarEConectarServico() {
        if (serviceBound && bleService != null) {
            bleService.conectarBle();
            return;
        }

        Context context = getApplication().getApplicationContext();
        Intent intent = new Intent(context, AuroraBleService.class);

        try {
            ContextCompat.startForegroundService(context, intent);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void iniciarConexaoBle() {
        if (serviceBound && bleService != null) {
            bleService.conectarBle();
        } else {
            iniciarEConectarServico();
        }
    }

    public void conectarDispositivoSelecionado(BluetoothDevice device) {
        if (bleService != null) {
            bleService.conectarDispositivoEspecifico(device);
        }
    }

    public void setNomeDispositivoCustomizado(String nome) {
        nomeDispositivo.postValue(nome);
    }

    // -----------------------------------------------------------------
    // BLOCO 7: CONNECTION INTERFACE PARA INTER-PROCESS COMMUNICATION (IPC)
    // -----------------------------------------------------------------
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AuroraBleService.LocalBinder binder = (AuroraBleService.LocalBinder) service;
            bleService = binder.getService();
            bleService.setServiceCallback(DispositivoViewModel.this);
            serviceBound = true;
            bleService.conectarBle();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            bleService = null;
        }
    };

    // -----------------------------------------------------------------
    // BLOCO 8: CALLBACKS DE EVENTOS EM TEMPO REAL RECEBIDOS DO SERVIÇO
    // -----------------------------------------------------------------
    @Override
    public void onStatusConexaoAlterado(boolean conectado) {
        estaConectado.postValue(conectado);
        statusGps.postValue(conectado ? "Sinal Ativo" : "Desconectado");
    }

    @Override
    public void onDadosGpsRecebidos(double lat, double lng, int bateria) {
        ultimasCoordenadas.postValue(new double[]{lat, lng});
        nivelBateria.postValue(bateria);
        statusGps.postValue("Sinal OK");
    }

    @Override
    public void onSosFisicoAcionado(double lat, double lng) {
        eventoSosAcionado.postValue(new double[]{lat, lng});
    }

    @Override
    public void onNomeDispositivoLido(String nome) {
        nomeDispositivo.postValue(nome);
    }

    @Override
    public void onDispositivoEncontrado(BluetoothDevice device) {
        List<BluetoothDevice> listaAtual = dispositivosEncontrados.getValue();
        if (listaAtual == null) {
            listaAtual = new ArrayList<>();
        }

        if (device != null) {
            for (BluetoothDevice d : listaAtual) {
                if (d.getAddress().equals(device.getAddress())) {
                    return; // Evita duplicados verificando o MAC Address
                }
            }

            List<BluetoothDevice> novaLista = new ArrayList<>(listaAtual);
            novaLista.add(device);
            dispositivosEncontrados.postValue(novaLista);
        }
    }

    public void resetarEventoSos() {
        eventoSosAcionado.setValue(null);
    }

    // -----------------------------------------------------------------
    // BLOCO 9: LIMPEZA E DESVINCULAÇÃO DO SERVIÇO (DESTRUIÇÃO DO VIEWMODEL)
    // -----------------------------------------------------------------
    @Override
    protected void onCleared() {
        super.onCleared();
        if (serviceBound) {
            try {
                getApplication().getApplicationContext().unbindService(serviceConnection);
            } catch (Exception ignored) {}
            serviceBound = false;
        }
    }
}