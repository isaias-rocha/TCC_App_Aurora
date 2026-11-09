package com.equipe1.aurora.ui.sos;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.equipe1.aurora.R;
import com.equipe1.aurora.infra.local.AppDataBase;
import com.equipe1.aurora.infra.local.entity.AlertaSosEntity;
import com.equipe1.aurora.ui.main.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// =====================================================================
// AURORA GPS - VIEWMODEL DA TELA SOS (SosViewModel.java)
// =====================================================================
// Executa a gravação assíncrona do SOS no Room com origem "APP", salva
// com status pendente de sincronização e dispara notificação Heads-up.
// =====================================================================

public class SosViewModel extends AndroidViewModel {

    // // BLOCO 1: CONSTANTES DE NOTIFICAÇÃO E BANCO LOCAL
    private static final String CHANNEL_SOS_ID = "AuroraSosAlertChannel";
    private static final int NOTIFICATION_SOS_APP_ID = 2001;

    private final AppDataBase db;
    private final ExecutorService executorService;

    // // BLOCO 2: LIVEDATAS DE ESTADO REATIVO
    private final MutableLiveData<Boolean> _alertaEnviadoComSucesso = new MutableLiveData<>();
    public final LiveData<Boolean> alertaEnviadoComSucesso = _alertaEnviadoComSucesso;
    private final MutableLiveData<String> _mensagemErro = new MutableLiveData<>();
    public final LiveData<String> mensagemErro = _mensagemErro;

    // // BLOCO 3: CONSTRUTOR E INICIALIZAÇÃO
    public SosViewModel(@NonNull Application application) {
        super(application);
        this.db = AppDataBase.getInstance(application);
        this.executorService = Executors.newSingleThreadExecutor();
        criarCanalNotificacaoSos();
    }

    // // BLOCO 4: PROCESSAMENTO DO SOS COM ORIGEM "APP" E GRAVAÇÃO OFFLINE (ROOM)
    public void enviarAlertaSosApp(double latitude, double longitude) {
        executorService.execute(() -> {
            try {
                // Instancia o alerta definindo a origem como 'APP' e pendente de sincronização
                AlertaSosEntity alerta = new AlertaSosEntity(
                        System.currentTimeMillis(),
                        latitude,
                        longitude,
                        "APP",         // Origem explícita: Aplicativo
                        "ATIVO",       // Status inicial
                        null,          // DispositivoId é nulo, pois foi gerado pelo Smartphone
                        false          // sincronizado = false (Salvo localmente para envio posterior ao MySQL)
                );

                // Grava no Room Database (Persistência Offline)
                db.alertaSosDao().inserir(alerta);

                // Dispara a notificação visual do sistema Android para o usuário
                dispararNotificacaoSosApp(latitude, longitude);

                // Notifica a View que a gravação local foi concluída
                _alertaEnviadoComSucesso.postValue(true);

            } catch (Exception e) {
                _mensagemErro.postValue("Erro ao registrar o alerta SOS localmente.");
                _alertaEnviadoComSucesso.postValue(false);
            }
        });
    }

    // // BLOCO 5: CRIAÇÃO DO CANAL DE NOTIFICAÇÃO DE ALTA PRIORIDADE (ANDROID 8.0+)
    private void criarCanalNotificacaoSos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel sosChannel = new NotificationChannel(
                        CHANNEL_SOS_ID,
                        "Alertas de Emergência SOS",
                        NotificationManager.IMPORTANCE_HIGH
                );
                sosChannel.setDescription("Notificações urgentes acionadas pelo aplicativo.");
                sosChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
                sosChannel.enableVibration(true);
                manager.createNotificationChannel(sosChannel);
            }
        }
    }

    // // BLOCO 6: DISPARO DA NOTIFICAÇÃO HEADS-UP DO SOS COM ORIGEM "APP"
    private void dispararNotificacaoSosApp(double latitude, double longitude) {
        Context context = getApplication().getApplicationContext();

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.putExtra("latitude", latitude);
        notificationIntent.putExtra("longitude", longitude);
        notificationIntent.putExtra("origem", "APP");
        notificationIntent.putExtra("veio_do_sos", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String horaAtual = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        String detalheTexto = String.format(
                "Origem: APP\n" +
                        "Horário: %s\n" +
                        "Lat: %.7f\n" +
                        "Lon: %.7f\n" +
                        "Status: Salvo localmente \n(Notificando contatos de emergência e círculos)",
                horaAtual, latitude, longitude
        );

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_SOS_ID)
                .setContentTitle("🚨 ALERTA SOS ATIVADO!")
                .setContentText("Origem: APP")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(detalheTexto))
                .setSmallIcon(R.drawable.ic_alerta)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            try {
                manager.notify(NOTIFICATION_SOS_APP_ID, notification);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    // // BLOCO 7: DESALOCAÇÃO DO EXECUTOR
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}