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
import android.os.VibratorManager;
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
 * Fragment responsável pelo painel de emergência SOS, contagem regressiva e ligação rápida para órgãos públicos.
 */
public class SosFragment extends Fragment {

    // ViewBinding para acesso seguro aos componentes do layout fragment_sos.xml
    private FragmentSosBinding binding;

    // ViewModel associado ao ciclo de vida do Fragment
    private SosViewModel sosViewModel;

    // Controle da contagem regressiva de 2 segundos ao manter o botão pressionado
    private CountDownTimer countDownTimer;
    private boolean isHolding = false;
    private static final long TEMPO_PRESSIONADO_MS = 2000; // 2 segundos

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla a interface utilizando ViewBinding
        binding = FragmentSosBinding.inflate(inflater, container, false);

        // Inicializa o ViewModel
        sosViewModel = new ViewModelProvider(this).get(SosViewModel.class);

        // Configura as escutas de eventos dos componentes visuais
        configurarBotaoVoltar();
        configurarBotaoSos();
        configurarSeletorEmergencia();
        observarViewModel();

        return binding.getRoot();
    }

    /**
     * Configura a ação de clique do botão voltar da barra superior.
     */
    private void configurarBotaoVoltar() {
        if (binding == null) return;
        binding.btnVoltar.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );
    }

    /**
     * Observa as alterações nos LiveDatas expostos pelo ViewModel.
     */
    private void observarViewModel() {
        sosViewModel.alertaEnviadoComSucesso.observe(getViewLifecycleOwner(), sucesso -> {
            if (Boolean.TRUE.equals(sucesso)) {
                Toast.makeText(requireContext(), "Emergência acionada e salva localmente!", Toast.LENGTH_LONG).show();
            }
        });

        sosViewModel.mensagemErro.observe(getViewLifecycleOwner(), erro -> {
            if (erro != null) {
                Toast.makeText(requireContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Configura o evento de pressionar continuamente (hold de 2 segundos) o botão SOS.
     */
    private void configurarBotaoSos() {
        if (binding == null) return;

        binding.btnSos.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Inicia a contagem quando o usuário toca no botão
                    iniciarContagemRegressiva();
                    return true;

                case MotionEvent.ACTION_UP:
                    v.performClick(); // Garante a acessibilidade do Android ao soltar o toque
                case MotionEvent.ACTION_CANCEL:
                    // Cancela a contagem se o usuário soltar antes dos 2 segundos
                    cancelarContagemRegressiva();
                    return true;
            }
            return false;
        });
    }

    /**
     * Inicia a contagem regressiva de 2, 1 segundos antes do disparo efetivo do alerta.
     */
    private void iniciarContagemRegressiva() {
        isHolding = true;
        vibrar(100); // Feedback tátil inicial do toque

        countDownTimer = new CountDownTimer(TEMPO_PRESSIONADO_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Proteção para evitar acesso ao binding caso a View seja destruída
                if (binding == null) return;

                int segundosRestantes = (int) Math.ceil(millisUntilFinished / 1000.0);

                // Atualização visual do texto do botão
                binding.btnSos.setText(String.valueOf(segundosRestantes));

                // Acessibilidade para leitores de tela (TalkBack)
                String anuncio = "Segure por mais " + segundosRestantes + " segundos para disparar o SOS";
                binding.btnSos.setContentDescription(anuncio);
                binding.btnSos.announceForAccessibility(segundosRestantes + " segundos");
            }

            @Override
            public void onFinish() {
                if (isHolding && binding != null) {
                    dispararAlertaSos();
                }
            }
        }.start();
    }

    /**
     * Cancela o temporizador e restaura a interface gráfica ao estado padrão.
     */
    private void cancelarContagemRegressiva() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (isHolding) {
            isHolding = false;

            if (binding != null) {
                // Restaura o texto original do botão e descrições de acessibilidade
                binding.btnSos.setText("SOS");
                binding.btnSos.setContentDescription("Acionar emergência SOS. Mantenha pressionado para enviar o alerta com sua localização.");
                binding.btnSos.announceForAccessibility("Envio de emergência cancelado.");
            }
        }
    }

    /**
     * Dispara o alerta SOS no sistema e envia para gravação no Room Database via ViewModel.
     */
    private void dispararAlertaSos() {
        isHolding = false;
        vibrar(500); // Feedback tátil longo confirmando o acionamento

        if (binding != null) {
            binding.btnSos.setText("SOS");
            binding.btnSos.setContentDescription("Alerta SOS enviado com sucesso.");
            binding.btnSos.announceForAccessibility("Emergência acionada! Sua localização foi enviada.");
        }

        // Exemplo de coordenadas atuais capturadas pelo GPS (pode ser integrado ao FusedLocationProvider)
        double latitudeAtual = -23.550520;
        double longitudeAtual = -46.633308;

        // Dispara a lógica de envio e gravação no Room via ViewModel
        sosViewModel.enviarAlertaSos(latitudeAtual, longitudeAtual);
    }

    /**
     * Executa resposta tátil por vibração no dispositivo, respeitando as preferências do usuário.
     */
    private void vibrar(long milissegundos) {
        if (getContext() == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("AuroraAccessibilityPrefs", Context.MODE_PRIVATE);
        boolean feedbackTatilAtivo = prefs.getBoolean("key_feedback_tatil", true);

        if (!feedbackTatilAtivo) return;

        // Suporte moderno a vibração conforme a versão do Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager != null) {
                vibratorManager.getDefaultVibrator().vibrate(VibrationEffect.createOneShot(milissegundos, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(milissegundos, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    /**
     * Configura a exibição da janela modal de serviços de emergência (Polícia, SAMU e Bombeiros).
     */
    private void configurarSeletorEmergencia() {
        if (binding == null) return;

        binding.btnLigarEmergencia.setOnClickListener(v -> {
            // Infla o layout do diálogo utilizando ViewBinding
            DialogSeletorEmergenciaBinding dialogBinding = DialogSeletorEmergenciaBinding.inflate(getLayoutInflater());

            AlertDialog.Builder construtorAlerta = new AlertDialog.Builder(requireContext());
            construtorAlerta.setView(dialogBinding.getRoot());

            AlertDialog dialogo = construtorAlerta.create();

            // Aplica fundo transparente para garantir o visual com cantos arredondados do layout
            if (dialogo.getWindow() != null) {
                dialogo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // Ação do botão voltar no diálogo
            dialogBinding.btnVoltar.setOnClickListener(view -> dialogo.dismiss());

            // Ações dos botões de chamadas telefônicas
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
     * Redireciona o usuário para o discador telefônico padrão do Android.
     */
    private void abrirDiscador(String numeroTelefone) {
        Intent intentDiscar = new Intent(Intent.ACTION_DIAL);
        intentDiscar.setData(Uri.parse("tel:" + numeroTelefone));
        startActivity(intentDiscar);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Garante o encerramento do timer e limpa o binding para evitar vazamento de memória (Memory Leak)
        cancelarContagemRegressiva();
        binding = null;
    }
}