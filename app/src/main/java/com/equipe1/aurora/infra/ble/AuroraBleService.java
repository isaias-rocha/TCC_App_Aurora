package com.equipe1.aurora.infra.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.equipe1.aurora.R;
import com.equipe1.aurora.ui.main.MainActivity;

/**
 * SERVIÇO DE PRIMEIRO PLANO (FOREGROUND SERVICE)
 * Mantém a conexão BLE ativa 24 horas por dia em segundo plano,
 * mesmo com o aplicativo fechado ou com a tela do smartphone desligada.
 */
public class AuroraBleService extends Service implements AuroraBleManager.BleCallback {

    private static final String TAG = "AuroraBleService";

    // Canal e ID para a notificação contínua de monitoramento em segundo plano
    private static final String CHANNEL_ID = "AuroraBleServiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    // NOVO: Canal e ID exclusivos de alta prioridade para o Alerta de Emergência SOS
    private static final String CHANNEL_SOS_ID = "AuroraSosAlertChannel";
    private static final int NOTIFICATION_SOS_ID = 1002;

    // Mecanismo Debounce: Evita disparos duplicados se o alerta vier via BLE e Wi-Fi simultaneamente
    private long ultimoTimestampSos = 0;
    private static final long INTERVALO_MINIMO_SOS_MS = 3000; // 03 segundos de trava

    private final IBinder binder = new LocalBinder();
    private AuroraBleManager bleManager;
    private ServiceCallback serviceCallback;

    public interface ServiceCallback {
        void onStatusConexaoAlterado(boolean conectado);
        void onDadosGpsRecebidos(double lat, double lng, int bateria);
        void onSosFisicoAcionado(double lat, double lng);
        void onNomeDispositivoLido(String nome);
        void onDispositivoEncontrado(BluetoothDevice device);
    }

    /**
     * Binder local que permite aos ViewModels comunicarem-se com este Serviço
     */
    public class LocalBinder extends Binder {
        public AuroraBleService getService() {
            return AuroraBleService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Serviço BLE de monitoramento inicializado.");
        bleManager = new AuroraBleManager(this, this);
        criarCanalNotificacao();
    }

    /**
     * Chamado ao iniciar o serviço em primeiro plano
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = criarNotificacao("Conectando ao Dispositivo Aurora...");

        // Suporte para declaração de Foreground Service no Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE | ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                );
            } catch (Exception e) {
                Log.e(TAG, "Falha ao atribuir tipo de Foreground Service, iniciando no modo genérico.", e);
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        bleManager.iniciarEscaneamentoEPareamento();
        return START_STICKY; // Garante que o SO recrie o serviço caso seja finalizado por falta de memória
    }

    public void setServiceCallback(ServiceCallback callback) {
        this.serviceCallback = callback;
    }

    public void conectarBle() {
        if (bleManager != null) {
            bleManager.iniciarEscaneamentoEPareamento();
        }
    }

    public void conectarDispositivoEspecifico(BluetoothDevice device) {
        if (bleManager != null) {
            bleManager.conectarDispositivoEspecifico(device);
        }
    }

    public void desconectarBle() {
        if (bleManager != null) {
            bleManager.desconectar();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // --- REPASSE DE CALLBACKS DO BLE MANAGER PARA O VIEWMODEL ---

    @Override
    public void onStatusConexaoAlterado(boolean conectado) {
        atualizarNotificacao(conectado ? "Localizador Aurora Conectado (Proteção Ativa)" : "Dispositivo Desconectado");
        if (serviceCallback != null) {
            serviceCallback.onStatusConexaoAlterado(conectado);
        }
    }

    @Override
    public void onDadosGpsRecebidos(double lat, double lng, int bateria) {
        if (serviceCallback != null) {
            serviceCallback.onDadosGpsRecebidos(lat, lng, bateria);
        }
    }

    /**
     * Filtra e processa alertas de emergência recebidos do hardware
     */
    @Override
    public void onSosFisicoAcionado(double lat, double lng) {
        long agora = System.currentTimeMillis();

        // Trava temporal de 3 segundos contra duplicidade (debounce)
        if (agora - ultimoTimestampSos < INTERVALO_MINIMO_SOS_MS) {
            Log.d(TAG, "Sinal de SOS ignorado pelo filtro debounce (Redundância ativada).");
            return;
        }

        ultimoTimestampSos = agora;
        Log.w(TAG, "🚨 SOS FÍSICO DE EMERGÊNCIA PROCESSADO! Lat: " + lat + " | Lng: " + lng);

        // NOVO: Dispara a notificação visual e sonora de emergência (Heads-up) no Android
        dispararNotificacaoSos(lat, lng);

        if (serviceCallback != null) {
            serviceCallback.onSosFisicoAcionado(lat, lng);
        }
    }

    @Override
    public void onNomeDispositivoLido(String nome) {
        if (serviceCallback != null) {
            serviceCallback.onNomeDispositivoLido(nome);
        }
    }

    @Override
    public void onDispositivoEncontrado(BluetoothDevice device) {
        if (serviceCallback != null) {
            serviceCallback.onDispositivoEncontrado(device);
        }
    }

    // --- GERENCIAMENTO DAS NOTIFICAÇÕES PERSISTENTES E ALERTAS DO SISTEMA ---

    /**
     * Cria os canais de notificação no Android 8.0+ (API 26+)
     */
    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                // 1. Canal do Foreground Service de monitoramento contínuo (Baixa Importância)
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Monitoramento Dispositivo Aurora",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Mantém a comunicação constante com o botão de emergência físico.");
                manager.createNotificationChannel(channel);

                // 2. NOVO: Canal de Emergência SOS (Alta Importância, som e vibração de alerta)
                NotificationChannel sosChannel = new NotificationChannel(
                        CHANNEL_SOS_ID,
                        "Alertas de Emergência SOS",
                        NotificationManager.IMPORTANCE_HIGH
                );
                sosChannel.setDescription("Notificações urgentes acionadas pelo botão de pânico físico.");
                sosChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
                sosChannel.enableVibration(true);
                manager.createNotificationChannel(sosChannel);
            }
        }
    }

    /**
     * Notificação fixa mantida pelo Foreground Service
     */
    private Notification criarNotificacao(String conteudo) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sistema de Segurança Aurora")
                .setContentText(conteudo)
                .setSmallIcon(R.drawable.ic_hardware)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Impede remoção arrastando para o lado
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /**
     * Dispara a notificação de emergência no estilo Heads-up (popup no topo da tela)
     * quando o botão físico de SOS é pressionado no ESP32
     */
    private void dispararNotificacaoSos(double lat, double lng) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("latitude: ", lat);
        notificationIntent.putExtra("longitude: ", lng);
        notificationIntent.putExtra("veio_do_sos", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String textoCoordenadas = String.format("Lat: %.6f | Lng: %.6f", lat, lng);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_SOS_ID)
                .setContentTitle("🚨 ALERTA DE SOS FISICO ATIVADO!" + "\n")
                .setContentText("Localização: " + textoCoordenadas)
                .setSmallIcon(R.drawable.ic_hardware)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true) // Desaparece ao ser tocada pelo usuário
                .setVibrate(new long[]{0, 500, 200, 500})
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            try {
                manager.notify(NOTIFICATION_SOS_ID, notification);
            } catch (SecurityException e) {
                Log.e(TAG, "Permissão de notificação negada no Android 13+.", e);
            }
        }
    }

    private void atualizarNotificacao(String conteudo) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, criarNotificacao(conteudo));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bleManager != null) {
            bleManager.desconectar();
        }
    }
}