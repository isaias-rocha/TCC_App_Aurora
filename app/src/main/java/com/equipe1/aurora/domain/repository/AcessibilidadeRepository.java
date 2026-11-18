// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Contrato (Interface) da camada de domínio que define as operações de
// leitura e persistência das preferências de acessibilidade, desacoplando
// as regras de negócio da tecnologia de armazenamento (SharedPreferences).
// =========================================================================

package com.equipe1.aurora.domain.repository;

import com.equipe1.aurora.domain.model.ConfiguracaoAcessibilidade;
import com.equipe1.aurora.domain.model.CorPrincipal;
import com.equipe1.aurora.domain.model.TipoDaltonismo;

public interface AcessibilidadeRepository {

    // -------------------------------------------------------------------------
    // SEÇÃO: LEITURA E RESTAURAÇÃO GLOBAL
    // -------------------------------------------------------------------------
    ConfiguracaoAcessibilidade obterConfiguracoes();

    void salvarConfiguracoes(ConfiguracaoAcessibilidade configuracao);

    void restaurarPadrao();

    // -------------------------------------------------------------------------
    // SEÇÃO: PERSISTÊNCIA INDIVIDUAL POR CONFIGURAÇÃO
    // -------------------------------------------------------------------------
    void salvarModoEscuro(boolean escuro);

    void salvarCorPrincipal(CorPrincipal cor);

    void salvarDaltonismoAtivo(boolean ativo);

    void salvarTipoDaltonismo(TipoDaltonismo tipo);

    void salvarAltoContraste(boolean ativo);

    void salvarTamanhoFonteIndex(int index);

    void salvarVibracaoAtiva(boolean ativa);

    void salvarTalkbackOtimizado(boolean ativo);
}