// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Enumeração das 4 cores principais disponíveis no aplicativo (Azul, Verde,
// Amarelo e Vermelho). Fornece códigos hexadecimais padrão, cores de texto
// com contraste WCAG e adaptação cromática para tipos de daltonismo.
// =========================================================================

package com.equipe1.aurora.domain.model;

public enum CorPrincipal {

    // -------------------------------------------------------------------------
    // SEÇÃO: OPÇÕES DE CORES PRINCIPAIS DO SISTEMA
    // -------------------------------------------------------------------------
    AZUL("Azul", "#1F51E6", "#FFFFFF"),
    VERDE("Verde", "#1F874B", "#FFFFFF"),
    AMARELO("Amarelo", "#EAB308", "#121216"), // Texto escuro para contraste WCAG
    VERMELHO("Vermelho", "#DB3929", "#FFFFFF");

    // -------------------------------------------------------------------------
    // SEÇÃO: ATRIBUTOS E CONSTRUTOR
    // -------------------------------------------------------------------------
    private final String nome;
    private final String hexCor;
    private final String hexTextoContraste;

    CorPrincipal(String nome, String hexCor, String hexTextoContraste) {
        this.nome = nome;
        this.hexCor = hexCor;
        this.hexTextoContraste = hexTextoContraste;
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: GETTERS E ADAPTAÇÃO PARA DALTONISMO
    // -------------------------------------------------------------------------
    public String getNome() { return nome; }
    public String getHexCor() { return hexCor; }
    public String getHexTextoContraste() { return hexTextoContraste; }

    // Ajusta o matiz quando o Modo Daltonismo está ativo para evitar confusão visual
    public String getHexCorAdaptada(boolean daltonismoAtivo, TipoDaltonismo tipo) {
        if (!daltonismoAtivo || tipo == null || tipo == TipoDaltonismo.NENHUM) {
            return hexCor;
        }

        switch (tipo) {
            case PROTANOPIA:
            case DEUTERANOPIA:
                // Diferencia Verde (Ciano/Teal) e Vermelho (Laranja Intenso)
                if (this == VERDE) return "#0D9488";
                if (this == VERMELHO) return "#EA580C";
                return hexCor;

            case TRITANOPIA:
                // Diferencia Azul (Azul Petróleo) e Amarelo (Rosa/Coral)
                if (this == AZUL) return "#0284C7";
                if (this == AMARELO) return "#E11D48";
                return hexCor;

            case ACROMATOPSIA:
                // Escala de luminância de alto contraste para visão monocromática
                if (this == AZUL) return "#334155";
                if (this == VERDE) return "#475569";
                if (this == AMARELO) return "#CBD5E1";
                if (this == VERMELHO) return "#1E293B";
                return hexCor;

            default:
                return hexCor;
        }
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: CONVERSÃO SEGURA A PARTIR DE STRING SALVA
    // -------------------------------------------------------------------------
    public static CorPrincipal fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return AZUL;
        }
        for (CorPrincipal cor : values()) {
            if (cor.name().equalsIgnoreCase(name.trim())) {
                return cor;
            }
        }
        return AZUL;
    }
}