package com.equipe1.aurora.ui.dispositivo;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.equipe1.aurora.infra.ble.AuroraBleService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// =====================================================================
// AURORA GPS - VIEWMODEL COM GESTÃO COMPLETA (DispositivoViewModel.java)
// =====================================================================
// Controla a persistência (SharedPreferences), ativação/desativação,
// renomeação e remoção de múltiplos rastreadores.
// =====================================================================

public class DispositivoViewModel extends AndroidViewModel implements AuroraBleService.ServiceCallback {

    private static final String PREFS_DISPOSITIVOS = "aurora_prefs_dispositivos";
    private static final String KEY_MACS_PAREADOS = "set_macs_pareados";
    private static final String KEY_ULTIMO_MAC = "ultimo_mac_conectado";

    private AuroraBleService bleService;
    private boolean serviceBound = false;

    private final MutableLiveData<List<DispositivoItem>> listaDispositivosPareados = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<BluetoothDevice>> dispositivosEncontrados = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<double[]> eventoSosAcionado = new MutableLiveData<>(null);

    public DispositivoViewModel(@NonNull Application application) {
        super(application);
        carregarDispositivosSalvos();
    }

    // BLOCO 1: GETTERS LIVEDATA
    public LiveData<List<DispositivoItem>> getListaDispositivosPareados() { return listaDispositivosPareados; }
    public LiveData<List<BluetoothDevice>> getDispositivosEncontrados() { return dispositivosEncontrados; }
    public LiveData<double[]> getEventoSosAcionado() { return eventoSosAcionado; }

    // BLOCO 2: CARREGAMENTO DOS DISPOSITIVOS SALVOS NO SHAREDPREFERENCES
    public void carregarDispositivosSalvos() {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_DISPOSITIVOS, Context.MODE_PRIVATE);
        Set<String> setMacs = prefs.getStringSet(KEY_MACS_PAREADOS, new HashSet<>());

        List<DispositivoItem> itens = new ArrayList<>();
        for (String mac : setMacs) {
            String nome = prefs.getString("nome_" + mac, "Aurora GPS (" + mac + ")");
            boolean ativo = prefs.getBoolean("ativo_" + mac, true);

            DispositivoItem item = new DispositivoItem(mac, nome);
            item.setAtivo(ativo);
            itens.add(item);
        }

        listaDispositivosPareados.setValue(itens);
    }

    // BLOCO 3: OPERAÇÕES DE SALVAR, RENOMEAR, ATIVAR/DESATIVAR E REMOVER
    public void salvarNovoDispositivo(String mac, String nomeCustomizado) {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_DISPOSITIVOS, Context.MODE_PRIVATE);
        Set<String> setMacs = new HashSet<>(prefs.getStringSet(KEY_MACS_PAREADOS, new HashSet<>()));

        setMacs.add(mac);
        prefs.edit()
                .putStringSet(KEY_MACS_PAREADOS, setMacs)
                .putString("nome_" + mac, nomeCustomizado)
                .putBoolean("ativo_" + mac, true)
                .putString(KEY_ULTIMO_MAC, mac)
                .apply();

        carregarDispositivosSalvos();
    }

    public void renomearDispositivo(String mac, String novoNome) {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_DISPOSITIVOS, Context.MODE_PRIVATE);
        prefs.edit().putString("nome_" + mac, novoNome).apply();

        carregarDispositivosSalvos();
    }

    public void alternarEstadoAtivoDispositivo(String mac, boolean ativo) {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_DISPOSITIVOS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("ativo_" + mac, ativo).apply();

        List<DispositivoItem> lista = listaDispositivosPareados.getValue();
        if (lista != null) {
            for (DispositivoItem item : lista) {
                if (item.getMacAddress().equals(mac)) {
                    item.setAtivo(ativo);
                    break;
                }
            }
            listaDispositivosPareados.setValue(lista);
        }
    }

    public void removerDispositivo(String mac) {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_DISPOSITIVOS, Context.MODE_PRIVATE);
        Set<String> setMacs = new HashSet<>(prefs.getStringSet(KEY_MACS_PAREADOS, new HashSet<>()));

        setMacs.remove(mac);
        prefs.edit()
                .putStringSet(KEY_MACS_PAREADOS, setMacs)
                .remove("nome_" + mac)
                .remove("ativo_" + mac)
                .apply();

        carregarDispositivosSalvos();
    }

    // BLOCO 4: INTEGRAÇÃO E CONEXÃO BLE
    public void iniciarVarreduraParaSelecao() {
        dispositivosEncontrados.setValue(new ArrayList<>());
        if (serviceBound && bleService != null) {
            bleService.iniciarVarreduraParaSelecao();
        } else {
            iniciarEConectarServico();
        }
    }

    public void conectarDispositivoEspecifico(String macAddress) {
        BluetoothDevice device = buscarDevicePorMacNaLista(macAddress);
        if (bleService != null && device != null) {
            bleService.conectarDispositivoEspecifico(device);
        }
    }

    private BluetoothDevice buscarDevicePorMacNaLista(String mac) {
        List<BluetoothDevice> lista = dispositivosEncontrados.getValue();
        if (lista != null) {
            for (BluetoothDevice d : lista) {
                if (d.getAddress().equals(mac)) return d;
            }
        }
        return null;
    }

    public void iniciarEConectarServico() {
        Context context = getApplication().getApplicationContext();
        Intent intent = new Intent(context, AuroraBleService.class);
        try {
            ContextCompat.startForegroundService(context, intent);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AuroraBleService.LocalBinder binder = (AuroraBleService.LocalBinder) service;
            bleService = binder.getService();
            bleService.setServiceCallback(DispositivoViewModel.this);
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            bleService = null;
        }
    };

    // BLOCO 5: REPASSE DE EVENTOS BLE
    @Override
    public void onStatusConexaoAlterado(boolean conectado) {
        List<DispositivoItem> lista = listaDispositivosPareados.getValue();
        if (lista != null && !lista.isEmpty()) {
            lista.get(0).setConectado(conectado);
            listaDispositivosPareados.postValue(lista);
        }
    }

    @Override
    public void onDadosGpsRecebidos(double lat, double lng, int bateria) {
        List<DispositivoItem> lista = listaDispositivosPareados.getValue();
        if (lista != null && !lista.isEmpty()) {
            DispositivoItem item = lista.get(0);
            item.setCoordenadas(lat, lng);
            item.setNivelBateria(bateria);
            item.setStatusGps("Sinal OK");
            listaDispositivosPareados.postValue(lista);
        }
    }

    @Override
    public void onSosFisicoAcionado(double lat, double lng) {
        eventoSosAcionado.postValue(new double[]{lat, lng});
    }

    @Override
    public void onNomeDispositivoLido(String nome, String macAddress) {}

    @Override
    public void onDispositivoEncontrado(BluetoothDevice device) {
        List<BluetoothDevice> lista = dispositivosEncontrados.getValue();
        if (lista == null) lista = new ArrayList<>();

        for (BluetoothDevice d : lista) {
            if (d.getAddress().equals(device.getAddress())) return;
        }

        List<BluetoothDevice> novaLista = new ArrayList<>(lista);
        novaLista.add(device);
        dispositivosEncontrados.postValue(novaLista);
    }

    public void resetarEventoSos() {
        eventoSosAcionado.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (serviceBound) {
            try { getApplication().getApplicationContext().unbindService(serviceConnection); } catch (Exception ignored) {}
            serviceBound = false;
        }
    }
}