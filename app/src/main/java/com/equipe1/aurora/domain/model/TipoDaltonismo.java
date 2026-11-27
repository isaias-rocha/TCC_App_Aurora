// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Enumeração dos tipos de adaptação cromática para daltonismo suportados
// pelo sistema, incluindo rótulos de exibição e descrições para TalkBack.
// =========================================================================

package com.equipe1.aurora.domain.model;

public enum TipoDaltonismo {

    // -------------------------------------------------------------------------
    // SEÇÃO: TIPOS CLÍNICOS DE ADAPTAÇÃO DE COR
    // -------------------------------------------------------------------------
    NENHUM("Selecione", "Nenhuma adaptação de daltonismo selecionada"),
    DEUTERANOPIA("Deuteranopia (Verde-Vermelho)", "Adaptação de contraste entre tons de verde e vermelho"),
    PROTANOPIA("Protanopia (Vermelho-Verde)", "Adaptação de luminância para deficiência no espectro vermelho"),
    TRITANOPIA("Tritanopia (Azul-Amarelo)", "Adaptação de contraste entre tons de azul e amarelo"),
    ACROMATOPSIA("Acromatopsia (Escala de Cinza)", "Adaptação monocromática com diferenciação por brilho");

    // -------------------------------------------------------------------------
    // SEÇÃO: ATRIBUTOS E CONSTRUTOR
    // -------------------------------------------------------------------------
    private final String label;
    private final String descricaoAcessibilidade;

    TipoDaltonismo(String label, String descricaoAcessibilidade) {
        this.label = label;
        this.descricaoAcessibilidade = descricaoAcessibilidade;
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: GETTERS E UTILITÁRIOS DE SELEÇÃO
    // -------------------------------------------------------------------------
    public String getLabel() {
        return label;
    }

    public String getDescricaoAcessibilidade() {
        return descricaoAcessibilidade;
    }

    // Retorna apenas os tipos clínicos válidos para o menu suspenso da UI
    public static TipoDaltonismo[] getOpcoesSelecionaveis() {
        return new TipoDaltonismo[]{
                DEUTERANOPIA,
                PROTANOPIA,
                TRITANOPIA,
                ACROMATOPSIA
        };
    }

    // Converte o nome salvo no SharedPreferences de volta para o Enum com segurança
    public static TipoDaltonismo fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return NENHUM;
        }
        for (TipoDaltonismo tipo : values()) {
            if (tipo.name().equalsIgnoreCase(name.trim())) {
                return tipo;
            }
        }
        return NENHUM;
    }
}