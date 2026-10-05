package com.equipe1.aurora.ui.config;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.equipe1.aurora.databinding.ActivityConfiguracoesBinding;
import com.equipe1.aurora.ui.planos.PlanosAssinaturaActivity;

// Certifique-se de importar a sua AcessibilidadeActivity real abaixo:
import com.equipe1.aurora.ui.acessibilidade.AcessibilidadeActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Activity responsável por gerenciar a tela principal de configurações do aplicativo Aurora.
 */
public class ConfigActivity extends AppCompatActivity {

    // Constantes de Persistência (SharedPreferences)
    private static final String PREFS_NAME = "AuroraConfigPrefs";
    private static final String KEY_IDIOMA = "key_idioma";
    private static final String KEY_NOTIFICACOES = "key_notificacoes";
    private static final String KEY_LOCALIZACAO = "key_localizacao";

    // View Binding
    private ActivityConfiguracoesBinding binding;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Habilita a renderização sem bordas (Edge-to-Edge)
        EdgeToEdge.enable(this);
        binding = ActivityConfiguracoesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializa o gerenciador de preferências locais
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Mapeamento e inicialização de estados e eventos
        carregarEstadosIniciais();
        configurarListeners();
        configurarWindowInsets();
    }

    /**
     * Ajusta os insets do sistema (status bar e navigation bar) preservando o padding original do layout.
     */
    private void configurarWindowInsets() {
        int initialPaddingLeft = binding.layoutConfig.getPaddingLeft();
        int initialPaddingTop = binding.layoutConfig.getPaddingTop();
        int initialPaddingRight = binding.layoutConfig.getPaddingRight();
        int initialPaddingBottom = binding.layoutConfig.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutConfig, (v, insets) -> {
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
     * Carrega as configurações dos switches previamente salvos.
     */
    private void carregarEstadosIniciais() {
        binding.switchNotificacoes.setChecked(preferences.getBoolean(KEY_NOTIFICACOES, true));
        binding.switchLocalizacao.setChecked(preferences.getBoolean(KEY_LOCALIZACAO, true));
    }

    /**
     * Define as ações executadas ao clicar nos itens da interface.
     */
    private void configurarListeners() {
        // Voltar
        binding.btnVoltar.setOnClickListener(v -> finish());

        // Switches de Notificação e Localização
        binding.switchNotificacoes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_NOTIFICACOES, isChecked).apply();
            String status = isChecked ? "Notificações ativadas" : "Notificações desativadas";
            buttonView.announceForAccessibility(status);
        });

        binding.switchLocalizacao.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_LOCALIZACAO, isChecked).apply();
            String status = isChecked ? "Compartilhamento de localização ativado" : "Compartilhamento de localização desativado";
            buttonView.announceForAccessibility(status);
        });

        // 1. PLANO DE ASSINATURA
        binding.menuPlanoAssinatura.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigActivity.this, PlanosAssinaturaActivity.class);
            startActivity(intent);
        });

        // 2. ACESSIBILIDADE (CORRIGIDO)
        binding.menuAcessibilidade.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigActivity.this, AcessibilidadeActivity.class);
            startActivity(intent);
        });

        // 3. IDIOMA
        binding.menuIdioma.setOnClickListener(v -> abrirDialogoIdioma());

        // 4. SOBRE O APLICATIVO
        binding.menuSobre.setOnClickListener(v -> abrirDialogoSobre());

        // 5. SAIR DA CONTA
        binding.tvSairConta.setOnClickListener(v -> realizarLogout());
    }

    /* ============================================================================================
       LÓGICA: SELEÇÃO DE IDIOMA
       ============================================================================================ */

    private void abrirDialogoIdioma() {
        // Mapeamento dos 5 idiomas suportados nas suas pastas res/values-xx-rXX
        String[] idiomas = {
                "Português (Brasil)",
                "English (US)",
                "Español (España)",
                "Deutsch (Deutschland)",
                "Русский (Россия)"
        };

        String[] tagsLinguagem = {
                "pt-BR",
                "en-US",
                "es-ES",
                "de-DE",
                "ru-RU"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Selecionar Idioma")
                .setItems(idiomas, (dialog, which) -> {
                    String tagSelecionada = tagsLinguagem[which];

                    preferences.edit().putString(KEY_IDIOMA, tagSelecionada).apply();

                    // Aplica o novo idioma globalmente no app via AppCompatDelegate
                    LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(tagSelecionada);
                    AppCompatDelegate.setApplicationLocales(appLocale);

                    Toast.makeText(this, "Idioma alterado para: " + idiomas[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /* ============================================================================================
       LÓGICA: SOBRE O APLICATIVO
       ============================================================================================ */

    private void abrirDialogoSobre() {
        String versaoApp = obterVersaoDoApp();

        String mensagemSobre = "Projeto Aurora\n" +
                "Versão: " + versaoApp + "\n\n" +
                "Desenvolvido pela Equipe 1 com foco em acessibilidade, segurança e melhor experiência do usuário.";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sobre o Aurora")
                .setMessage(mensagemSobre)
                .setPositiveButton("Fechar", null)
                .setNeutralButton("Termos & Privacidade", (dialog, which) -> {
                    Toast.makeText(this, "Abrindo Termos de Uso...", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String obterVersaoDoApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageInfo pInfo = getPackageManager().getPackageInfo(
                        getPackageName(),
                        PackageManager.PackageInfoFlags.of(0)
                );
                return pInfo.versionName;
            } else {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                return pInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }

    /* ============================================================================================
       LÓGICA: SAIR DA CONTA (LOGOUT)
       ============================================================================================ */

    private void realizarLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sair da Conta")
                .setMessage("Tem certeza de que deseja sair?")
                .setPositiveButton("Sair", (dialog, which) -> {
                    Toast.makeText(this, "Saindo da conta...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}