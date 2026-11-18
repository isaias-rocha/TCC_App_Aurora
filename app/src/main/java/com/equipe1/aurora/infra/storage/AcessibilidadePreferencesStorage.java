// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Camada de acesso direto ao SharedPreferences do Android. Gerencia as
// chaves de armazenamento local das preferências de acessibilidade e
// permite observar mudanças em tempo real entre diferentes telas.
// =========================================================================

package com.equipe1.aurora.infra.storage;

import android.content.Context;
import android.content.SharedPreferences;
import com.equipe1.aurora.domain.model.ConfiguracaoAcessibilidade;

public class AcessibilidadePreferencesStorage {

    // -------------------------------------------------------------------------
    // SEÇÃO: CONSTANTES DE NOME DO ARQUIVO E CHAVES DO SHAREDPREFERENCES
    // -------------------------------------------------------------------------
    private static final String PREFS_NAME = "aurora_acessibilidade_prefs";
    private static final String KEY_MODO_ESCURO = "modo_escuro";
    private static final String KEY_COR_PRINCIPAL = "cor_principal";
    private static final String KEY_DALTONISMO_ATIVO = "daltonismo_ativo";
    private static final String KEY_TIPO_DALTONISMO = "tipo_daltonismo";
    private static final String KEY_ALTO_CONTRASTE = "alto_contraste";
    private static final String KEY_TAMANHO_FONTE = "tamanho_fonte_index";
    private static final String KEY_VIBRACAO = "vibracao_ativa";
    private static final String KEY_TALKBACK = "talkback_otimizado";

    private final SharedPreferences prefs;

    // -------------------------------------------------------------------------
    // SEÇÃO: CONSTRUTOR
    // -------------------------------------------------------------------------
    public AcessibilidadePreferencesStorage(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: GETTERS E SETTERS DE TEMA E COR PRINCIPAL
    // -------------------------------------------------------------------------
    public boolean isModoEscuro() {
        return prefs.getBoolean(KEY_MODO_ESCURO, true);
    }

    public void setModoEscuro(boolean escuro) {
        prefs.edit().putBoolean(KEY_MODO_ESCURO, escuro).apply();
    }

    public String getCorPrincipal() {
        return prefs.getString(KEY_COR_PRINCIPAL, "AZUL");
    }

    public void setCorPrincipal(String nomeCor) {
        prefs.edit().putString(KEY_COR_PRINCIPAL, nomeCor).apply();
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: GETTERS E SETTERS DE DALTONISMO E ALTO CONTRASTE
    // -------------------------------------------------------------------------
    public boolean isDaltonismoAtivo() {
        return prefs.getBoolean(KEY_DALTONISMO_ATIVO, false);
    }

    public void setDaltonismoAtivo(boolean ativo) {
        prefs.edit().putBoolean(KEY_DALTONISMO_ATIVO, ativo).apply();
    }

    public String getTipoDaltonismo() {
        return prefs.getString(KEY_TIPO_DALTONISMO, "NENHUM");
    }

    public void setTipoDaltonismo(String nomeTipo) {
        prefs.edit().putString(KEY_TIPO_DALTONISMO, nomeTipo).apply();
    }

    public boolean isAltoContraste() {
        return prefs.getBoolean(KEY_ALTO_CONTRASTE, false);
    }

    public void setAltoContraste(boolean ativo) {
        prefs.edit().putBoolean(KEY_ALTO_CONTRASTE, ativo).apply();
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: GETTERS E SETTERS DE FONTE, VIBRAÇÃO E TALKBACK
    // -------------------------------------------------------------------------
    public int getTamanhoFonteIndex() {
        int salvo = prefs.getInt(KEY_TAMANHO_FONTE, ConfiguracaoAcessibilidade.DEFAULT_FONTE_INDEX);
        return ConfiguracaoAcessibilidade.limitarIndiceFonte(salvo);
    }

    public void setTamanhoFonteIndex(int index) {
        prefs.edit().putInt(KEY_TAMANHO_FONTE, ConfiguracaoAcessibilidade.limitarIndiceFonte(index)).apply();
    }

    public boolean isVibracaoAtiva() {
        return prefs.getBoolean(KEY_VIBRACAO, true);
    }

    public void setVibracaoAtiva(boolean ativa) {
        prefs.edit().putBoolean(KEY_VIBRACAO, ativa).apply();
    }

    public boolean isTalkbackOtimizado() {
        return prefs.getBoolean(KEY_TALKBACK, true);
    }

    public void setTalkbackOtimizado(boolean ativo) {
        prefs.edit().putBoolean(KEY_TALKBACK, ativo).apply();
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: OPERAÇÕES GLOBAIS E OBSERVAÇÃO DE MUDANÇAS
    // -------------------------------------------------------------------------
    public void limparTudo() {
        prefs.edit().clear().apply();
    }

    public void registrarListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void removerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }
}