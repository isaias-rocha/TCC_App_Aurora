package com.equipe1.aurora.ui.sos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.equipe1.aurora.databinding.DialogSeletorEmergenciaBinding;
import com.equipe1.aurora.databinding.FragmentSosBinding;

/**
 * Fragment responsável pelo painel de emergência SOS e acionamento rápido de auxílio.
 */
public class SosFragment extends Fragment {

    private FragmentSosBinding binding;
    private SosViewModel sosViewModel;

    // Controle do temporizador de toque contínuo
    private CountDownTimer countDownTimer;
    private boolean isHolding = false;
    private static final long TEMPO_PRESSIONADO_MS = 2000; // 2 segundos

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSosBinding.inflate(inflater, container, false);
        sosViewModel = new ViewModelProvider(this).get(SosViewModel.class);

        // Inicializa as escutas de eventos da interface
        configurarBotaoSos();
        configurarSeletorEmergencia();

        return binding.getRoot();
    }

    /**
     * Configura o evento de toque contínuo (hold) de 2 segundos no botão SOS.
     */
    private void configurarBotaoSos() {
        binding.btnSos.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Usuário iniciou o toque no botão
                    iniciarContagemRegressiva();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Usuário soltou o botão antes do tempo concluir
                    cancelarContagemRegressiva();
                    return true;
            }
            return false;
        });
    }

    /**
     * Inicia a contagem regressiva visual, tátil e auditiva para disparo do alerta.
     */
    private void iniciarContagemRegressiva() {
        isHolding = true;
        vibrar(100); // Feedback tátil do toque inicial

        countDownTimer = new CountDownTimer(TEMPO_PRESSIONADO_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int segundosRestantes = (int) Math.ceil(millisUntilFinished / 1000.0);

                // Atualização visual do botão
                binding.btnSos.setText(String.valueOf(segundosRestantes));

                // Acessibilidade para leitores de tela (TalkBack)
                String anuncio = "Segure por mais " + segundosRestantes + " segundos para disparar o SOS";
                binding.btnSos.setContentDescription(anuncio);
                binding.btnSos.announceForAccessibility(segundosRestantes + " segundos");
            }

            @Override
            public void onFinish() {
                if (isHolding) {
                    dispararAlertaSos();
                }
            }
        }.start();
    }

    /**
     * Interrompe a contagem regressiva e restabelece os estados visuais padrões.
     */
    private void cancelarContagemRegressiva() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (isHolding) {
            isHolding = false;

            // Restaura texto e descrição de acessibilidade
            binding.btnSos.setText("SOS");
            binding.btnSos.setContentDescription("Acionar emergência SOS. Mantenha pressionado para enviar o alerta com sua localização.");
            binding.btnSos.announceForAccessibility("Envio de emergência cancelado.");
        }
    }

    /**
     * Dispara a ação de emergência após a contagem de 2 segundos ser finalizada.
     */
    private void dispararAlertaSos() {
        isHolding = false;
        vibrar(500); // Feedback tátil longo confirmando a ação

        binding.btnSos.setText("SOS");
        binding.btnSos.setContentDescription("Alerta SOS enviado com sucesso.");
        binding.btnSos.announceForAccessibility("Emergência acionada! Sua localização foi enviada.");

        // Executa a regra de negócio no ViewModel
        sosViewModel.enviarAlertaSos();

        Toast.makeText(requireContext(), "Localização enviada aos contatos", Toast.LENGTH_LONG).show();
    }

    /**
     * Executa resposta tátil por vibração considerando a preferência do usuário em Acessibilidade.
     */
    private void vibrar(long milissegundos) {
        if (getContext() == null) return;

        // Consulta as configurações de acessibilidade salvas
        SharedPreferences prefs = requireContext().getSharedPreferences("AuroraAccessibilityPrefs", Context.MODE_PRIVATE);
        boolean feedbackTatilAtivo = prefs.getBoolean("key_feedback_tatil", true);

        // Se o feedback tátil estiver desativado pelo usuário, cancela a vibração
        if (!feedbackTatilAtivo) return;

        // Dispara a vibração no hardware
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot
                    (milissegundos, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    /**
     * Configura a exibição do diálogo modal com os contatos da Polícia, SAMU e Bombeiros.
     */
    private void configurarSeletorEmergencia() {
        binding.btnLigarEmergencia.setOnClickListener(v -> {
            DialogSeletorEmergenciaBinding dialogBinding = DialogSeletorEmergenciaBinding.inflate(getLayoutInflater());

            AlertDialog.Builder construtorAlerta = new AlertDialog.Builder(requireContext());
            construtorAlerta.setView(dialogBinding.getRoot());

            AlertDialog dialogo = construtorAlerta.create();

            if (dialogo.getWindow() != null) {
                dialogo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            dialogBinding.btnVoltar.setOnClickListener(view -> dialogo.dismiss());

            dialogBinding.btnPolicia.setOnClickListener(view -> {
                abrirDiscador("190");
                dialogo.dismiss();
            });

            dialogBinding.btnSamu.setOnClickListener(view -> {
                abrirDiscador("192");
                dialogo.dismiss();
            });

            dialogBinding.btnBombeiros.setOnClickListener(view -> {
                abrirDiscador("193");
                dialogo.dismiss();
            });

            dialogo.show();
        });
    }

    /**
     * Redireciona o usuário para a aplicação nativa de telefone com o número preenchido.
     */
    private void abrirDiscador(String numeroTelefone) {
        Intent intentDiscar = new Intent(Intent.ACTION_DIAL);
        intentDiscar.setData(Uri.parse("tel:" + numeroTelefone));
        startActivity(intentDiscar);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Garante o encerramento do timer caso a tela seja fechada durante o processo
        cancelarContagemRegressiva();
        binding = null;
    }
}