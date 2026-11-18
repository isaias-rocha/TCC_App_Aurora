// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Implementação concreta do AcessibilidadeRepository na camada infra.
// Faz o mapeamento entre os tipos primitivos do SharedPreferencesStorage
// e os objetos ricos de domínio (ConfiguracaoAcessibilidade, CorPrincipal
// e TipoDaltonismo).
// =========================================================================

package com.equipe1.aurora.infra.repository;

import com.equipe1.aurora.domain.model.ConfiguracaoAcessibilidade;
import com.equipe1.aurora.domain.model.CorPrincipal;
import com.equipe1.aurora.domain.model.TipoDaltonismo;
import com.equipe1.aurora.domain.repository.AcessibilidadeRepository;
import com.equipe1.aurora.infra.storage.AcessibilidadePreferencesStorage;

public class AcessibilidadeRepositoryImpl implements AcessibilidadeRepository {

    // -------------------------------------------------------------------------
    // SEÇÃO: DEPENDÊNCIA DE ARMAZENAMENTO LOCAL
    // -------------------------------------------------------------------------
    private final AcessibilidadePreferencesStorage storage;

    public AcessibilidadeRepositoryImpl(AcessibilidadePreferencesStorage storage) {
        this.storage = storage;
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: LEITURA E GRAVAÇÃO CONSOLIDADA
    // -------------------------------------------------------------------------
    @Override
    public ConfiguracaoAcessibilidade obterConfiguracoes() {
        return new ConfiguracaoAcessibilidade(
                storage.isModoEscuro(),
                CorPrincipal.fromName(storage.getCorPrincipal()),
                storage.isDaltonismoAtivo(),
                TipoDaltonismo.fromName(storage.getTipoDaltonismo()),
                storage.isAltoContraste(),
                storage.getTamanhoFonteIndex(),
                storage.isVibracaoAtiva(),
                storage.isTalkbackOtimizado()
        );
    }

    @Override
    public void salvarConfiguracoes(ConfiguracaoAcessibilidade config) {
        if (config == null) return;
        storage.setModoEscuro(config.isModoEscuro());
        storage.setCorPrincipal(config.getCorPrincipal().name());
        storage.setDaltonismoAtivo(config.isDaltonismoAtivo());
        storage.setTipoDaltonismo(config.getTipoDaltonismo().name());
        storage.setAltoContraste(config.isAltoContraste());
        storage.setTamanhoFonteIndex(config.getTamanhoFonteIndex());
        storage.setVibracaoAtiva(config.isVibracaoAtiva());
        storage.setTalkbackOtimizado(config.isTalkbackOtimizado());
    }

    @Override
    public void restaurarPadrao() {
        storage.limparTudo();
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: MÉTODOS DE PERSISTÊNCIA INDIVIDUAL
    // -------------------------------------------------------------------------
    @Override
    public void salvarModoEscuro(boolean escuro) {
        storage.setModoEscuro(escuro);
    }

    @Override
    public void salvarCorPrincipal(CorPrincipal cor) {
        CorPrincipal corSegura = (cor != null) ? cor : CorPrincipal.AZUL;
        storage.setCorPrincipal(corSegura.name());
    }

    @Override
    public void salvarDaltonismoAtivo(boolean ativo) {
        storage.setDaltonismoAtivo(ativo);
    }

    @Override
    public void salvarTipoDaltonismo(TipoDaltonismo tipo) {
        TipoDaltonismo tipoSeguro = (tipo != null) ? tipo : TipoDaltonismo.NENHUM;
        storage.setTipoDaltonismo(tipoSeguro.name());
    }

    @Override
    public void salvarAltoContraste(boolean ativo) {
        storage.setAltoContraste(ativo);
    }

    @Override
    public void salvarTamanhoFonteIndex(int index) {
        storage.setTamanhoFonteIndex(index);
    }

    @Override
    public void salvarVibracaoAtiva(boolean ativa) {
        storage.setVibracaoAtiva(ativa);
    }

    @Override
    public void salvarTalkbackOtimizado(boolean ativo) {
        storage.setTalkbackOtimizado(ativo);
    }
}