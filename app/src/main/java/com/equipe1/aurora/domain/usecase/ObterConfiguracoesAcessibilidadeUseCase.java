// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Caso de uso responsável por recuperar o estado consolidado de todas as
// configurações de acessibilidade salvas para aplicação nas telas do app.
// =========================================================================

package com.equipe1.aurora.domain.usecase;

import com.equipe1.aurora.domain.model.ConfiguracaoAcessibilidade;
import com.equipe1.aurora.domain.repository.AcessibilidadeRepository;
import java.util.Objects;

public class ObterConfiguracoesAcessibilidadeUseCase {

    // -------------------------------------------------------------------------
    // SEÇÃO: DEPENDÊNCIA DO REPOSITÓRIO
    // -------------------------------------------------------------------------
    private final AcessibilidadeRepository repository;

    public ObterConfiguracoesAcessibilidadeUseCase(AcessibilidadeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "AcessibilidadeRepository não pode ser nulo");
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: EXECUÇÃO DO CASO DE USO
    // -------------------------------------------------------------------------
    public ConfiguracaoAcessibilidade executar() {
        ConfiguracaoAcessibilidade config = repository.obterConfiguracoes();
        return (config != null) ? config : ConfiguracaoAcessibilidade.padrao();
    }
}