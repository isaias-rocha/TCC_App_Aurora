// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Motor global de acessibilidade visual, tátil e sonora do aplicativo Aurora.
// Aplica dinamicamente em todas as Activities e Fragments:
// 1. Cores (ThemeOverlay + varredura de Cards, Bordas, Botões, Campos, etc.);
// 2. Tamanho da Fonte proporcional em todos os TextViews em tempo real;
// 3. Feedback de Vibração respeitando a preferência salva;
// 4. Otimização de TalkBack (descrições automáticas, área 48dp e anúncios).
// =========================================================================

package com.equipe1.aurora.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import com.equipe1.aurora.R;
import com.equipe1.aurora.domain.model.ConfiguracaoAcessibilidade;
import com.equipe1.aurora.domain.model.CorPrincipal;
import com.equipe1.aurora.infra.repository.AcessibilidadeRepositoryImpl;
import com.equipe1.aurora.infra.storage.AcessibilidadePreferencesStorage;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

public final class TemaDinamicoHelper {

    // // Chave única para armazenar o tamanho original (px) de cada TextView sem inflar múltiplas vezes
    private static final int KEY_ORIGINAL_TEXT_SIZE = 0x7F0A9901;

    private TemaDinamicoHelper() {}

    // -------------------------------------------------------------------------
    // SEÇÃO 1: APLICAÇÃO DE TEMA E ESCALA ANTES DO SETCONTENTVIEW()
    // -------------------------------------------------------------------------
    public static void aplicarTemaNaActivity(Activity activity, ConfiguracaoAcessibilidade config) {
        if (activity == null || config == null) return;

        // // Aplica Modo Claro ou Modo Escuro globalmente
        AppCompatDelegate.setDefaultNightMode(
                config.isModoEscuro() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        // // Aplica o ThemeOverlay correspondente à cor escolhida e alto contraste
        aplicarOverlayCor(activity, config.getCorPrincipal(), config.isAltoContraste());

        // // Ajusta a configuração base de escala de fonte dos recursos da Activity
        Configuration configuration = new Configuration(activity.getResources().getConfiguration());
        configuration.fontScale = config.getFatorEscalaFonte();
        activity.getResources().updateConfiguration(configuration, activity.getResources().getDisplayMetrics());
    }

    public static void aplicarOverlayCor(Activity activity, CorPrincipal cor, boolean altoContraste) {
        int overlayResId = obterStyleOverlayCor(cor);
        activity.getTheme().applyStyle(overlayResId, true);

        if (altoContraste) {
            activity.getTheme().applyStyle(R.style.ThemeOverlay_Aurora_AltoContraste, true);
        }
    }

    // -------------------------------------------------------------------------
    // SEÇÃO 2: VARREDURA COMPLETA DO ESQUELETO DA TELA (CORES, FONTE E TALKBACK)
    // -------------------------------------------------------------------------
    public static void aplicarConfiguracoesNaTela(Activity activity, View rootView, ConfiguracaoAcessibilidade config) {
        if (activity == null || rootView == null || config == null) return;

        // // Reaplica o overlay caso a cor tenha mudado em tempo real
        aplicarOverlayCor(activity, config.getCorPrincipal(), config.isAltoContraste());

        // // Resolve a cor efetiva (considera values/values-night e adaptação de daltonismo)
        int corPrimaria;
        if (config.isDaltonismoEfetivo()) {
            corPrimaria = Color.parseColor(config.getHexCorEfetiva());
        } else {
            corPrimaria = resolverAtributoCor(activity, androidx.appcompat.R.attr.colorPrimary);
        }

        int corTextoContraste = resolverAtributoCor(activity, com.google.android.material.R.attr.colorOnPrimary);
        int corBordaSuave = Color.argb(85, Color.red(corPrimaria), Color.green(corPrimaria), Color.blue(corPrimaria));

        percorrerViewsRecursivamente(
                rootView,
                corPrimaria,
                corTextoContraste,
                corBordaSuave,
                config
        );
    }

    private static void percorrerViewsRecursivamente(
            View view,
            int corPrimaria,
            int corTextoContraste,
            int corBordaSuave,
            ConfiguracaoAcessibilidade config
    ) {
        boolean ignorarCor = "ignorar_tema_dinamico".equals(view.getTag());
        float density = view.getResources().getDisplayMetrics().density;

        // // 1. ATUALIZAÇÃO DE CORES NOS COMPONENTES DA TELA
        if (!ignorarCor) {
            aplicarCorNoComponente(view, corPrimaria, corTextoContraste, corBordaSuave, config.isAltoContraste(), density);
        }

        // // 2. ATUALIZAÇÃO DO TAMANHO DA FONTE EM TODOS OS TEXTVIEWS
        if (view instanceof TextView) {
            aplicarEscalaFonteNoTexto((TextView) view, config.getFatorEscalaFonte());
        }

        // // 3. OTIMIZAÇÃO DE TALKBACK (ÁREA DE TOQUE E DESCRIÇÃO AUTOMÁTICA)
        if (config.isTalkbackOtimizado()) {
            otimizarComponenteParaTalkBack(view, density);
        }

        // // Continua a varredura nos filhos caso a View seja um container
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                percorrerViewsRecursivamente(
                        group.getChildAt(i),
                        corPrimaria,
                        corTextoContraste,
                        corBordaSuave,
                        config
                );
            }
        }
    }

    // -------------------------------------------------------------------------
    // SEÇÃO 3: APLICAÇÃO DE CORES POR TIPO DE COMPONENTE
    // -------------------------------------------------------------------------
    private static void aplicarCorNoComponente(
            View view,
            int corPrimaria,
            int corTextoContraste,
            int corBordaSuave,
            boolean altoContraste,
            float density
    ) {
        // // Cards Material 3
        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            if (altoContraste) {
                card.setStrokeWidth((int) (2 * density));
                card.setStrokeColor(corPrimaria);
            } else if (card.getStrokeWidth() > 0) {
                card.setStrokeColor(corBordaSuave);
            }
        }
        // // Botões Material 3 (Preenchidos e Outlined)
        else if (view instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) view;
            if (button.getStrokeWidth() > 0) {
                button.setStrokeColor(ColorStateList.valueOf(altoContraste ? corPrimaria : corBordaSuave));
            } else {
                button.setBackgroundTintList(ColorStateList.valueOf(corPrimaria));
                button.setTextColor(corTextoContraste);
                button.setIconTint(ColorStateList.valueOf(corTextoContraste));
            }
        }
        // // Botão Flutuante (FAB - ex.: Botão SOS)
        else if (view instanceof FloatingActionButton) {
            FloatingActionButton fab = (FloatingActionButton) view;
            fab.setBackgroundTintList(ColorStateList.valueOf(corPrimaria));
            fab.setImageTintList(ColorStateList.valueOf(corTextoContraste));
        }
        // // Campos de Texto (TextInputLayout)
        else if (view instanceof TextInputLayout) {
            TextInputLayout inputLayout = (TextInputLayout) view;
            inputLayout.setBoxStrokeColor(corPrimaria);
            inputLayout.setHintTextColor(ColorStateList.valueOf(corPrimaria));
        }
        // // Switches
        else if (view instanceof SwitchMaterial) {
            SwitchMaterial sw = (SwitchMaterial) view;
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
            };
            sw.setThumbTintList(new ColorStateList(states, new int[]{corPrimaria, Color.parseColor("#9CA3AF")}));
            sw.setTrackTintList(new ColorStateList(states, new int[]{corBordaSuave, Color.parseColor("#374151")}));
        }
        // // CheckBoxes e RadioButtons
        else if (view instanceof CheckBox) {
            ((CheckBox) view).setButtonTintList(ColorStateList.valueOf(corPrimaria));
        } else if (view instanceof RadioButton) {
            ((RadioButton) view).setButtonTintList(ColorStateList.valueOf(corPrimaria));
        }
        // // Sliders e Barras de Progresso
        else if (view instanceof Slider) {
            Slider slider = (Slider) view;
            ColorStateList tint = ColorStateList.valueOf(corPrimaria);
            slider.setThumbTintList(tint);
            slider.setTrackActiveTintList(tint);
        } else if (view instanceof ProgressBar) {
            ((ProgressBar) view).setProgressTintList(ColorStateList.valueOf(corPrimaria));
        }
        // // Barra de Navegação Inferior (BottomNavigationView)
        else if (view instanceof BottomNavigationView) {
            BottomNavigationView nav = (BottomNavigationView) view;
            nav.setItemActiveIndicatorColor(ColorStateList.valueOf(corBordaSuave));
            int[][] navStates = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
            };
            ColorStateList navColors = new ColorStateList(navStates, new int[]{corPrimaria, Color.parseColor("#64748B")});
            nav.setItemIconTintList(navColors);
            nav.setItemTextColor(navColors);
        }
    }

    // -------------------------------------------------------------------------
    // SEÇÃO 4: ESCALA DINÂMICA DE TAMANHO DA FONTE EM TEMPO REAL
    // -------------------------------------------------------------------------
    private static void aplicarEscalaFonteNoTexto(TextView textView, float fatorEscala) {
        Object tagTamanhoOriginal = textView.getTag(KEY_ORIGINAL_TEXT_SIZE);
        float tamanhoOriginalPx;

        if (tagTamanhoOriginal instanceof Float) {
            tamanhoOriginalPx = (Float) tagTamanhoOriginal;
        } else {
            // // Salva o tamanho original (em pixels) na primeira vez que o TextView é lido
            tamanhoOriginalPx = textView.getTextSize();
            textView.setTag(KEY_ORIGINAL_TEXT_SIZE, tamanhoOriginalPx);
        }

        float novoTamanhoPx = tamanhoOriginalPx * fatorEscala;
        if (Math.abs(textView.getTextSize() - novoTamanhoPx) > 0.5f) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, novoTamanhoPx);
        }
    }

    // -------------------------------------------------------------------------
    // SEÇÃO 5: LÓGICA DE TALKBACK (ÁREA DE TOQUE, RÓTULOS E ANÚNCIO DE VOZ)
    // -------------------------------------------------------------------------
    private static void otimizarComponenteParaTalkBack(View view, float density) {
        // // 1. Garante área mínima de toque de 48dp x 48dp (Diretriz WCAG / Google)
        if (view.isClickable() || view instanceof ImageButton) {
            int minTouchPx = (int) (48 * density);
            if (view.getMinimumWidth() < minTouchPx) view.setMinimumWidth(minTouchPx);
            if (view.getMinimumHeight() < minTouchPx) view.setMinimumHeight(minTouchPx);
        }

        // // 2. Gera contentDescription automático se um item clicável não tiver descrição
        if (view.isClickable() && TextUtils.isEmpty(view.getContentDescription())) {
            if (view instanceof EditText) {
                CharSequence hint = ((EditText) view).getHint();
                if (!TextUtils.isEmpty(hint)) {
                    view.setContentDescription("Campo de texto: " + hint);
                }
            } else if (view instanceof ViewGroup) {
                String textoAgrupado = extrairTextosFilhos((ViewGroup) view);
                if (!textoAgrupado.isEmpty()) {
                    view.setContentDescription(textoAgrupado);
                }
            }
        }
    }

    // // Concatena os textos internos de um Card/Row clicável para leitura fluida do TalkBack
    private static String extrairTextosFilhos(ViewGroup group) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView && !(child instanceof MaterialButton)) {
                CharSequence txt = ((TextView) child).getText();
                if (!TextUtils.isEmpty(txt)) {
                    if (sb.length() > 0) sb.append(". ");
                    sb.append(txt);
                }
            } else if (child instanceof ViewGroup) {
                String nested = extrairTextosFilhos((ViewGroup) child);
                if (!nested.isEmpty()) {
                    if (sb.length() > 0) sb.append(". ");
                    sb.append(nested);
                }
            }
        }
        return sb.toString();
    }

    // // Emite anúncio de voz imediato se TalkBack estiver ativo no aparelho e otimizado no app
    public static void anunciarParaTalkBack(Context context, View view, String mensagem) {
        if (context == null || view == null || TextUtils.isEmpty(mensagem)) return;

        ConfiguracaoAcessibilidade config = carregarConfiguracoes(context);
        if (!config.isTalkbackOtimizado()) return;

        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null && am.isEnabled()) {
            view.announceForAccessibility(mensagem);
            view.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        }
    }

    // -------------------------------------------------------------------------
    // SEÇÃO 6: LÓGICA DE VIBRAÇÃO TÁTIL (BOTÕES, ALERTAS E SOS)
    // -------------------------------------------------------------------------
    // // Vibração curta para confirmação de clique ou troca de opção (45ms)
    public static void vibrarToque(Context context) {
        executarVibracao(context, 45L);
    }

    // // Vibração reforçada para alertas críticos e acionamento do botão SOS (300ms)
    public static void vibrarAlertaSOS(Context context) {
        executarVibracao(context, 300L);
    }

    private static void executarVibracao(Context context, long duracaoMs) {
        if (context == null) return;

        ConfiguracaoAcessibilidade config = carregarConfiguracoes(context);
        if (!config.isVibracaoAtiva()) return;

        Vibrator vibrator = obterVibrator(context);
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duracaoMs, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(duracaoMs);
        }
    }

    @SuppressWarnings("deprecation")
    private static Vibrator obterVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return (manager != null) ? manager.getDefaultVibrator() : null;
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    // -------------------------------------------------------------------------
    // SEÇÃO 7: RESOLUÇÃO DE ATRIBUTOS E OVERLAYS DE TEMA
    // -------------------------------------------------------------------------
    public static int resolverAtributoCor(Activity activity, int attrResId) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }

    private static ConfiguracaoAcessibilidade carregarConfiguracoes(Context context) {
        AcessibilidadePreferencesStorage storage = new AcessibilidadePreferencesStorage(context);
        return new AcessibilidadeRepositoryImpl(storage).obterConfiguracoes();
    }

    private static int obterStyleOverlayCor(CorPrincipal cor) {
        switch (cor) {
            case VERDE:
                return R.style.ThemeOverlay_Aurora_Verde;
            case AMARELO:
                return R.style.ThemeOverlay_Aurora_Amarelo;
            case VERMELHO:
                return R.style.ThemeOverlay_Aurora_Vermelho;
            case AZUL:
            default:
                return R.style.ThemeOverlay_Aurora_Azul;
        }
    }
}