    package com.equipe1.aurora.ui.config;

    import android.content.Context;
    import android.content.SharedPreferences;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.VibrationEffect;
    import android.os.Vibrator;
    import android.view.View;
    import android.widget.ImageButton;

    import androidx.activity.EdgeToEdge;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;

    import com.equipe1.aurora.R;
    import com.google.android.material.switchmaterial.SwitchMaterial;

    public class AccessibilityActivity extends AppCompatActivity {

        // Constantes para persistência local via SharedPreferences
        private static final String PREFS_NAME = "AuroraAccessibilityPrefs";
        public static final String KEY_ALTO_CONTRASTE = "key_alto_contraste";
        public static final String KEY_REDUZIR_ANIMACOES = "key_reduzir_animacoes";
        public static final String KEY_FEEDBACK_TATIL = "key_feedback_tatil";
        public static final String KEY_LEITOR_TELA = "key_leitor_tela";

        // Componentes visuais
        private ImageButton btnVoltar;
        private View rowAltoContraste, rowReduzirAnimacoes, rowFeedbackTatil, rowLeitorTela;
        private SwitchMaterial switchAltoContraste, switchReduzirAnimacoes, switchFeedbackTatil, switchLeitorTela;
        private SharedPreferences preferences;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Ativa exibição sem bordas (Edge-to-Edge) para Android 10+
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_accessibility);

            // Inicializa o gerenciador de preferências
            preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            // Mapeia views e configura o estado dos componentes
            initComponents();
            carregarEstadosIniciais();
            configurarListeners();

            // Ajusta os espaçamentos internos (padding) para evitar sobreposição com as barras de sistema (Status/Navigation)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.accessibility), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }


         // Vincula as variáveis Java aos elementos definidos no XML.

        private void initComponents() {
            btnVoltar = findViewById(R.id.btn_voltar_acessibilidade);

            // Linhas inteiras clicáveis (ConstraintLayouts contêineres)
            rowAltoContraste = findViewById(R.id.row_alto_contraste);
            rowReduzirAnimacoes = findViewById(R.id.row_reduzir_animacoes);
            rowFeedbackTatil = findViewById(R.id.row_feedback_tatil);
            rowLeitorTela = findViewById(R.id.row_leitor_tela);

            // Chaves de seleção
            switchAltoContraste = findViewById(R.id.switch_alto_contraste);
            switchReduzirAnimacoes = findViewById(R.id.switch_reduzir_animacoes);
            switchFeedbackTatil = findViewById(R.id.switch_feedback_tatil);
            switchLeitorTela = findViewById(R.id.switch_leitor_tela);
        }

         // Recupera os valores previamente salvos no SharedPreferences para restaurar o estado da tela.

        private void carregarEstadosIniciais() {
            switchAltoContraste.setChecked(preferences.getBoolean(KEY_ALTO_CONTRASTE, false));
            switchReduzirAnimacoes.setChecked(preferences.getBoolean(KEY_REDUZIR_ANIMACOES, false));
            switchFeedbackTatil.setChecked(preferences.getBoolean(KEY_FEEDBACK_TATIL, true));
            switchLeitorTela.setChecked(preferences.getBoolean(KEY_LEITOR_TELA, true));
        }


        //Configura os eventos de clique estendendo a área de toque para o contêiner completo de cada opção.

        private void configurarListeners() {
            // Encerra a Activity ao clicar no botão voltar
            btnVoltar.setOnClickListener(v -> finish());

            // Alternância: Modo de Alto Contraste
            rowAltoContraste.setOnClickListener(v -> {
                boolean novoEstado = !switchAltoContraste.isChecked();
                switchAltoContraste.setChecked(novoEstado);
                salvarPreferencia(KEY_ALTO_CONTRASTE, novoEstado);

                anunciarParaTalkBack(v, "Modo de alto contraste " + (novoEstado ? "ativado" : "desativado"));

                // Recarrega a tela para recriar o tema com alto contraste
                recreate();
            });

            // Alternância: Reduzir Animações
            rowReduzirAnimacoes.setOnClickListener(v -> {
                boolean novoEstado = !switchReduzirAnimacoes.isChecked();
                switchReduzirAnimacoes.setChecked(novoEstado);
                salvarPreferencia(KEY_REDUZIR_ANIMACOES, novoEstado);

                anunciarParaTalkBack(v, "Redução de animações " + (novoEstado ? "ativada" : "desativada"));
            });

            // Alternância: Feedback Tátil (Vibração)
            rowFeedbackTatil.setOnClickListener(v -> {
                boolean novoEstado = !switchFeedbackTatil.isChecked();
                switchFeedbackTatil.setChecked(novoEstado);
                salvarPreferencia(KEY_FEEDBACK_TATIL, novoEstado);

                anunciarParaTalkBack(v, "Vibração " + (novoEstado ? "ativada" : "desativada"));

                // Executa vibração imediata de teste caso ativado
                if (novoEstado) {
                    executarVibracaoTeste();
                }
            });

            // Alternância: Suporte a Leitor de Tela (TalkBack)
            rowLeitorTela.setOnClickListener(v -> {
                boolean novoEstado = !switchLeitorTela.isChecked();
                switchLeitorTela.setChecked(novoEstado);
                salvarPreferencia(KEY_LEITOR_TELA, novoEstado);

                anunciarParaTalkBack(v, "Suporte a TalkBack " + (novoEstado ? "ativado" : "desativado"));
            });
        }


         // Escreve e persiste a preferência em background.

        private void salvarPreferencia(String chave, boolean valor) {
            preferences.edit().putBoolean(chave, valor).apply();
        }


         // Força uma notificação por voz para leitores de tela como o TalkBack.

        private void anunciarParaTalkBack(View view, String mensagem) {
            view.announceForAccessibility(mensagem);
        }


         // Emite um pulso tátil direto para confirmação física de ativação do recurso.
        private void executarVibracaoTeste() {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        }
    }