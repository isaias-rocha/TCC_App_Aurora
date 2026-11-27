// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Modelo de domínio imutável que representa todas as preferências de
// acessibilidade do usuário. Centraliza regras puras de cálculo de escala
// de fonte (sp e multiplicador), validação de limites e estado padrão.
// =========================================================================

package com.equipe1.aurora.domain.model;

public class ConfiguracaoAcessibilidade {

    // -------------------------------------------------------------------------
    // SEÇÃO: CONSTANTES DE DOMÍNIO (LIMITES DE ESCALA DE FONTE)
    // -------------------------------------------------------------------------
    public static final int MIN_FONTE_INDEX = 0;
    public static final int DEFAULT_FONTE_INDEX = 1;
    public static final int MAX_FONTE_INDEX = 3;

    // -------------------------------------------------------------------------
    // SEÇÃO: ATRIBUTOS IMUTÁVEIS DA CONFIGURAÇÃO
    // -------------------------------------------------------------------------
    private final boolean modoEscuro;
    private final CorPrincipal corPrincipal;
    private final boolean daltonismoAtivo;
    private final TipoDaltonismo tipoDaltonismo;
    private final boolean altoContraste;
    private final int tamanhoFonteIndex;
    private final boolean vibracaoAtiva;
    private final boolean talkbackOtimizado;

    // -------------------------------------------------------------------------
    // SEÇÃO: CONSTRUTOR COM VALIDAÇÃO DE INTEGRIDADE
    // -------------------------------------------------------------------------
    public ConfiguracaoAcessibilidade(
            boolean modoEscuro,
            CorPrincipal corPrincipal,
            boolean daltonismoAtivo,
            TipoDaltonismo tipoDaltonismo,
            boolean altoContraste,
            int tamanhoFonteIndex,
            boolean vibracaoAtiva,
            boolean talkbackOtimizado
    ) {
        this.modoEscuro = modoEscuro;
        this.corPrincipal = (corPrincipal != null) ? corPrincipal : CorPrincipal.AZUL;
        this.daltonismoAtivo = daltonismoAtivo;
        this.tipoDaltonismo = (tipoDaltonismo != null) ? tipoDaltonismo : TipoDaltonismo.NENHUM;
        this.altoContraste = altoContraste;
        this.tamanhoFonteIndex = limitarIndiceFonte(tamanhoFonteIndex);
        this.vibracaoAtiva = vibracaoAtiva;
        this.talkbackOtimizado = talkbackOtimizado;
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: FÁBRICA DE CONFIGURAÇÃO PADRÃO DO APLICATIVO
    // -------------------------------------------------------------------------
    public static ConfiguracaoAcessibilidade padrao() {
        return new ConfiguracaoAcessibilidade(
                true,
                CorPrincipal.AZUL,
                false,
                TipoDaltonismo.NENHUM,
                false,
                DEFAULT_FONTE_INDEX,
                true,
                true
        );
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: REGRAS DE NEGÓCIO (FONTE, DALTONISMO E COR EFETIVA)
    // -------------------------------------------------------------------------
    public static int limitarIndiceFonte(int index) {
        return Math.max(MIN_FONTE_INDEX, Math.min(index, MAX_FONTE_INDEX));
    }

    // Retorna o fator multiplicador aplicado aos textos de todas as telas
    public float getFatorEscalaFonte() {
        switch (tamanhoFonteIndex) {
            case 0:
                return 0.88f; // Pequeno (14sp)
            case 2:
                return 1.15f; // Grande (18sp)
            case 3:
                return 1.30f; // Extra Grande (20sp)
            case 1:
            default:
                return 1.00f; // Padrão (16sp)
        }
    }

    // Retorna o valor numérico em sp para exibição no indicador do Slider
    public int getTamanhoFonteSp() {
        int[] tamanhosSp = {14, 16, 18, 20};
        return tamanhosSp[tamanhoFonteIndex];
    }

    // Retorna o texto formatado (ex.: "16sp")
    public String getTamanhoFonteLabel() {
        return getTamanhoFonteSp() + "sp";
    }

    // Verifica se o modo daltonismo está realmente ativo com um tipo válido selecionado
    public boolean isDaltonismoEfetivo() {
        return daltonismoAtivo && tipoDaltonismo != TipoDaltonismo.NENHUM;
    }

    // Retorna o hexadecimal da cor considerando adaptação para daltonismo
    public String getHexCorEfetiva() {
        return corPrincipal.getHexCorAdaptada(daltonismoAtivo, tipoDaltonismo);
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: GETTERS PÚBLICOS
    // -------------------------------------------------------------------------
    public boolean isModoEscuro() { return modoEscuro; }
    public CorPrincipal getCorPrincipal() { return corPrincipal; }
    public boolean isDaltonismoAtivo() { return daltonismoAtivo; }
    public TipoDaltonismo getTipoDaltonismo() { return tipoDaltonismo; }
    public boolean isAltoContraste() { return altoContraste; }
    public int getTamanhoFonteIndex() { return tamanhoFonteIndex; }
    public boolean isVibracaoAtiva() { return vibracaoAtiva; }
    public boolean isTalkbackOtimizado() { return talkbackOtimizado; }
}