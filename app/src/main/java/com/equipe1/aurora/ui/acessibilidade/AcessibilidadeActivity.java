package com.equipe1.aurora.ui.acessibilidade;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.equipe1.aurora.databinding.ActivityAccessibilidadeBinding;
import com.google.android.material.slider.Slider;

/**
 * Activity responsável por gerenciar e aplicar os ajustes de acessibilidade e feedback visual/tátil.
 */
public class AcessibilidadeActivity extends AppCompatActivity {

    // CHAVES DE ARMAZENAMENTO (SharedPreferences)
    private static final String PREFS_NAME = "AuroraAccessibilityPrefs";
    public static final String KEY_ALTO_CONTRASTE = "key_alto_contraste";
    public static final String KEY_REDUZIR_ANIMACOES = "key_reduzir_animacoes";
    public static final String KEY_TAMANHO_FONTE = "key_tamanho_fonte"; // Armazena o valor (0.0f a 3.0f)
    public static final String KEY_FEEDBACK_TATIL = "key_feedback_tatil";
    public static final String KEY_LEITOR_TELA = "key_leitor_tela";

    // View Binding
    private ActivityAccessibilidadeBinding binding;
    private SharedPreferences preferences;

    /**
     * Sobrescreve o contexto para aplicar o fator de escala da fonte configurado.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        float valorSlider = prefs.getFloat(KEY_TAMANHO_FONTE, 1.0f); // Padrão: nível 1 (1.0x)

        float fontScale = obterEscalaFonte(valorSlider);

        Configuration overrideConfig = new Configuration(newBase.getResources().getConfiguration());
        overrideConfig.fontScale = fontScale;

        Context context = newBase.createConfigurationContext(overrideConfig);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ativa exibição tela cheia (Edge-to-Edge)
        EdgeToEdge.enable(this);
        binding = ActivityAccessibilidadeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializa SharedPreferences
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // MÉTODOS DE INICIALIZAÇÃO DA TELA
        carregarEstadosIniciais();
        configurarListeners();
        configurarWindowInsets();
    }

    /**
     * Ajusta o padding dinâmico sem sobrescrever o padding original do layout XML.
     */
    private void configurarWindowInsets() {
        int initialPaddingLeft = binding.accessibility.getPaddingLeft();
        int initialPaddingTop = binding.accessibility.getPaddingTop();
        int initialPaddingRight = binding.accessibility.getPaddingRight();
        int initialPaddingBottom = binding.accessibility.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(binding.accessibility, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialPaddingLeft + systemBars.left,
                    initialPaddingTop + systemBars.top,
                    initialPaddingRight + systemBars.right,
                    initialPaddingBottom + systemBars.bottom
            );
            return insets;
        });
    }

    /**
     * Carrega as configurações previamente salvas no dispositivo.
     */
    private void carregarEstadosIniciais() {
        binding.switchAltoContraste.setChecked(preferences.getBoolean(KEY_ALTO_CONTRASTE, false));
        binding.switchReduzirAnimacoes.setChecked(preferences.getBoolean(KEY_REDUZIR_ANIMACOES, false));
        binding.sliderTamanhoFonte.setValue(preferences.getFloat(KEY_TAMANHO_FONTE, 1.0f));
        binding.switchFeedbackTatil.setChecked(preferences.getBoolean(KEY_FEEDBACK_TATIL, true));

        // Se o TalkBack do sistema estiver ativo, forçamos o valor para true
        boolean talkBackSistemaAtivo = verificarTalkBackSistema();
        boolean leitorPref = preferences.getBoolean(KEY_LEITOR_TELA, talkBackSistemaAtivo);
        binding.switchLeitorTela.setChecked(leitorPref);
    }

    /**
     * Define a lógica e o comportamento disparado ao interagir com cada opção.
     */
    private void configurarListeners() {
        // Voltar
        binding.btnVoltarAcessibilidade.setOnClickListener(v -> finish());

        // 1. Modo de Alto Contraste
        binding.rowAltoContraste.setOnClickListener(v -> {
            boolean novoEstado = !binding.switchAltoContraste.isChecked();
            binding.switchAltoContraste.setChecked(novoEstado);
            salvarPreferencia(KEY_ALTO_CONTRASTE, novoEstado);

            anunciarParaTalkBack(v, "Modo de alto contraste " + (novoEstado ? "ativado" : "desativado"));
            recreate();
        });

        // 2. Reduzir Animações
        binding.rowReduzirAnimacoes.setOnClickListener(v -> {
            boolean novoEstado = !binding.switchReduzirAnimacoes.isChecked();
            binding.switchReduzirAnimacoes.setChecked(novoEstado);
            salvarPreferencia(KEY_REDUZIR_ANIMACOES, novoEstado);

            anunciarParaTalkBack(v, "Redução de animações " + (novoEstado ? "ativada" : "desativada"));
        });

        // 3. Tamanho da Fonte (Slider)
        binding.sliderTamanhoFonte.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                // Nenhuma ação necessária ao iniciar o toque
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                float value = slider.getValue();
                preferences.edit().putFloat(KEY_TAMANHO_FONTE, value).apply();

                String[] niveis = {"Pequeno", "Padrão", "Grande", "Muito Grande"};
                int index = Math.min((int) value, niveis.length - 1);

                anunciarParaTalkBack(slider, "Tamanho da fonte alterado para " + niveis[index]);
                recreate();
            }
        });

        // 4. Feedback Tátil (Vibração)
        binding.rowFeedbackTatil.setOnClickListener(v -> {
            boolean novoEstado = !binding.switchFeedbackTatil.isChecked();
            binding.switchFeedbackTatil.setChecked(novoEstado);
            salvarPreferencia(KEY_FEEDBACK_TATIL, novoEstado);

            anunciarParaTalkBack(v, "Vibração " + (novoEstado ? "ativada" : "desativada"));

            if (novoEstado) {
                executarVibracaoTeste();
            }
        });

        // 5. Suporte a Leitor de Tela (TalkBack)
        binding.rowLeitorTela.setOnClickListener(v -> {
            boolean novoEstado = !binding.switchLeitorTela.isChecked();
            binding.switchLeitorTela.setChecked(novoEstado);
            salvarPreferencia(KEY_LEITOR_TELA, novoEstado);

            anunciarParaTalkBack(v, "Suporte a TalkBack " + (novoEstado ? "ativado" : "desativado"));
        });
    }

    /**
     * Mapeia os índices do Slider (0, 1, 2, 3) para fatores reais de escala de fonte (fontScale).
     */
    private static float obterEscalaFonte(float valorSlider) {
        int index = Math.round(valorSlider);
        switch (index) {
            case 0:
                return 0.85f; // Pequeno
            case 2:
                return 1.15f; // Grande
            case 3:
                return 1.30f; // Muito Grande
            case 1:
            default:
                return 1.00f; // Padrão
        }
    }

    /**
     * Grava uma preferência booleana no arquivo SharedPreferences.
     */
    private void salvarPreferencia(String chave, boolean valor) {
        preferences.edit().putBoolean(chave, valor).apply();
    }

    /**
     * Envia uma mensagem em áudio para o leitor de tela (TalkBack).
     */
    private void anunciarParaTalkBack(View view, String mensagem) {
        if (view != null) {
            view.announceForAccessibility(mensagem);
        }
    }

    /**
     * Verifica se algum serviço de acessibilidade/TalkBack está ativo no sistema Android.
     */
    private boolean verificarTalkBackSistema() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        return am != null && am.isEnabled() && am.isTouchExplorationEnabled();
    }

    /**
     * Executa um pulso de vibração curto (100ms) compatível com versões legadas e Android 12+ (API 31+).
     */
    private void executarVibracaoTeste() {
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
            } else {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}