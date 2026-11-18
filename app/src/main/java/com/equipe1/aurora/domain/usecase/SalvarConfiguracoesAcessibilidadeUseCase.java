// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Caso de uso responsável por validar e persistir as alterações de
// acessibilidade feitas pelo usuário (tema, cor, daltonismo, contraste,
// escala de fonte, vibração e suporte a TalkBack).
// =========================================================================

package com.equipe1.aurora.domain.usecase;

import com.equipe1.aurora.domain.model.ConfiguracaoAcessibilidade;
import com.equipe1.aurora.domain.model.CorPrincipal;
import com.equipe1.aurora.domain.model.TipoDaltonismo;
import com.equipe1.aurora.domain.repository.AcessibilidadeRepository;
import java.util.Objects;

public class SalvarConfiguracoesAcessibilidadeUseCase {

    // -------------------------------------------------------------------------
    // SEÇÃO: DEPENDÊNCIA DO REPOSITÓRIO
    // -------------------------------------------------------------------------
    private final AcessibilidadeRepository repository;

    public SalvarConfiguracoesAcessibilidadeUseCase(AcessibilidadeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "AcessibilidadeRepository não pode ser nulo");
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: REGRAS DE SALVAMENTO INDIVIDUAL COM VALIDAÇÃO
    // -------------------------------------------------------------------------
    public void salvarModoEscuro(boolean escuro) {
        repository.salvarModoEscuro(escuro);
    }

    public void salvarCorPrincipal(CorPrincipal cor) {
        repository.salvarCorPrincipal(cor != null ? cor : CorPrincipal.AZUL);
    }

    public void salvarDaltonismoAtivo(boolean ativo) {
        repository.salvarDaltonismoAtivo(ativo);
        // Se o usuário ativar o modo daltonismo sem tipo escolhido, define Deuteranopia por padrão
        if (ativo) {
            ConfiguracaoAcessibilidade atual = repository.obterConfiguracoes();
            if (atual.getTipoDaltonismo() == TipoDaltonismo.NENHUM) {
                repository.salvarTipoDaltonismo(TipoDaltonismo.DEUTERANOPIA);
            }
        }
    }

    public void salvarTipoDaltonismo(TipoDaltonismo tipo) {
        TipoDaltonismo tipoValido = (tipo != null) ? tipo : TipoDaltonismo.NENHUM;
        repository.salvarTipoDaltonismo(tipoValido);
    }

    public void salvarAltoContraste(boolean ativo) {
        repository.salvarAltoContraste(ativo);
    }

    public void salvarTamanhoFonteIndex(int index) {
        // Garante que o índice esteja sempre dentro do intervalo permitido (0 a 3)
        int indiceSeguro = ConfiguracaoAcessibilidade.limitarIndiceFonte(index);
        repository.salvarTamanhoFonteIndex(indiceSeguro);
    }

    public void salvarVibracaoAtiva(boolean ativa) {
        repository.salvarVibracaoAtiva(ativa);
    }

    public void salvarTalkbackOtimizado(boolean ativo) {
        repository.salvarTalkbackOtimizado(ativo);
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: SALVAMENTO EM LOTE E RESTAURAÇÃO
    // -------------------------------------------------------------------------
    public void salvarTudo(ConfiguracaoAcessibilidade configuracao) {
        if (configuracao != null) {
            repository.salvarConfiguracoes(configuracao);
        }
    }

    public void restaurarPadrao() {
        repository.restaurarPadrao();
    }
}