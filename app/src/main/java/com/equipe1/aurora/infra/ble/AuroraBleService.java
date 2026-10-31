package com.equipe1.aurora.infra.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.equipe1.aurora.R;
import com.equipe1.aurora.ui.main.MainActivity;

// =====================================================================
// AURORA GPS - SERVIÇO BLE E NOTIFICAÇÕES (AuroraBleService.java)
// =====================================================================
// Mantém o monitoramento de emergência em primeiro plano e dispara
// alertas contendo o nome customizado do dispositivo.
// =====================================================================

public class AuroraBleService extends Service implements AuroraBleManager.BleCallback {

    private static final String TAG = "AuroraBleService";
    private static final String PREFS_DISPOSITIVOS = "aurora_prefs_dispositivos";

    private static final String CHANNEL_ID = "AuroraBleServiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    private static final String CHANNEL_SOS_ID = "AuroraSosAlertChannel";
    private static final int NOTIFICATION_SOS_ID = 1002;

    private long ultimoTimestampSos = 0;
    private static final long INTERVALO_MINIMO_SOS_MS = 3000;

    private final IBinder binder = new LocalBinder();
    private AuroraBleManager bleManager;
    private ServiceCallback serviceCallback;

    public interface ServiceCallback {
        void onStatusConexaoAlterado(boolean conectado);
        void onDadosGpsRecebidos(double lat, double lng, int bateria);
        void onSosFisicoAcionado(double lat, double lng);
        void onNomeDispositivoLido(String nome, String macAddress);
        void onDispositivoEncontrado(BluetoothDevice device);
    }

    public class LocalBinder extends Binder {
        public AuroraBleService getService() {
            return AuroraBleService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bleManager = new AuroraBleManager(this, this);
        criarCanalNotificacao();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = criarNotificacao("Monitorando dispositivo em segundo plano...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE | ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } catch (Exception e) {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        return START_STICKY;
    }

    public void setServiceCallback(ServiceCallback callback) {
        this.serviceCallback = callback;
    }

    public void iniciarVarreduraParaSelecao() {
        if (bleManager != null) {
            bleManager.iniciarEscaneamentoParaLista();
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

    // BLOCO 1: REPASSE DE CALLBACKS
    @Override
    public void onStatusConexaoAlterado(boolean conectado) {
        atualizarNotificacao(conectado ? "Proteção Ativa - Dispositivo Conectado" : "Dispositivo Desconectado");
        if (serviceCallback != null) serviceCallback.onStatusConexaoAlterado(conectado);
    }

    @Override
    public void onDadosGpsRecebidos(double lat, double lng, int bateria) {
        if (serviceCallback != null) serviceCallback.onDadosGpsRecebidos(lat, lng, bateria);
    }

    @Override
    public void onSosFisicoAcionadoComDetalhes(double lat, double lng, int bateria, double velocidade, double altitude, String horario, String macAddress) {
        long agora = System.currentTimeMillis();
        if (agora - ultimoTimestampSos < INTERVALO_MINIMO_SOS_MS) return;

        ultimoTimestampSos = agora;

        // Resgata o nome customizado das SharedPreferences com base no MAC Address
        SharedPreferences prefs = getSharedPreferences(PREFS_DISPOSITIVOS, Context.MODE_PRIVATE);
        String nomeSalvo = prefs.getString("nome_" + macAddress, "Aurora GPS (" + macAddress + ")");

        dispararNotificacaoSosCompleta(nomeSalvo, lat, lng, bateria, velocidade, altitude, horario);

        if (serviceCallback != null) serviceCallback.onSosFisicoAcionado(lat, lng);
    }

    @Override
    public void onNomeDispositivoLido(String nome, String macAddress) {
        if (serviceCallback != null) serviceCallback.onNomeDispositivoLido(nome, macAddress);
    }

    @Override
    public void onDispositivoEncontrado(BluetoothDevice device) {
        if (serviceCallback != null) serviceCallback.onDispositivoEncontrado(device);
    }

    // BLOCO 2: DISPARO DE NOTIFICAÇÃO COM O NOME DO RASTREADOR
    private void dispararNotificacaoSosCompleta(String nomeDispositivo, double lat, double lng, int bateria, double velocidade, double altitude, String horario) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("latitude", lat);
        notificationIntent.putExtra("longitude", lng);
        notificationIntent.putExtra("veio_do_sos", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String detalheTexto = String.format(
                "Dispositivo: %s\n" +
                        "Horário: %s | \n" +
                        "Bateria: %d%%\n" +
                        "Lat: %.7f | Lon: %.7f\n" +
                        "Altitude: %.1f m \n" +
                        "Velocidade: %.1f km/h",
                nomeDispositivo, horario, bateria, lat, lng, altitude, velocidade
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_SOS_ID)
                .setContentTitle("🚨 SOS DISPOSITIVO ATIVADO")

                .setContentText("Dispositivo: " + nomeDispositivo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(detalheTexto))
                .setSmallIcon(R.drawable.ic_hardware)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 300, 500})
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            try {
                manager.notify(NOTIFICATION_SOS_ID, notification);
            } catch (SecurityException e) {
                Log.e(TAG, "Permissão de notificação negada.", e);
            }
        }
    }

    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Monitoramento Dispositivo", NotificationManager.IMPORTANCE_LOW);
                manager.createNotificationChannel(channel);

                NotificationChannel sosChannel = new NotificationChannel(CHANNEL_SOS_ID, "Alertas SOS", NotificationManager.IMPORTANCE_HIGH);
                sosChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
                sosChannel.enableVibration(true);
                manager.createNotificationChannel(sosChannel);
            }
        }
    }

    private Notification criarNotificacao(String conteudo) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sistema Aurora")
                .setContentText(conteudo)
                .setSmallIcon(R.drawable.ic_hardware)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void atualizarNotificacao(String conteudo) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, criarNotificacao(conteudo));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bleManager != null) bleManager.desconectar();
    }
}