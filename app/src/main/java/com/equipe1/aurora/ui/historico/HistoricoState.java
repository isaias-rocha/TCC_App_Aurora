package com.equipe1.aurora.ui.historico;

import com.equipe1.aurora.domain.model.HistoricoItem;
import com.equipe1.aurora.domain.model.ResumoEstatistico;
import java.util.List;

public class HistoricoState {
    private final boolean loading;
    private final List<HistoricoItem> itens;
    private final ResumoEstatistico resumo;
    private final String mensagemErro;

    public HistoricoState(boolean loading, List<HistoricoItem> itens, ResumoEstatistico resumo, String mensagemErro) {
        this.loading = loading;
        this.itens = itens;
        this.resumo = resumo;
        this.mensagemErro = mensagemErro;
    }

    public static HistoricoState loading() {
        return new HistoricoState(true, null, null, null);
    }

    public static HistoricoState sucesso(List<HistoricoItem> itens, ResumoEstatistico resumo) {
        return new HistoricoState(false, itens, resumo, null);
    }

    public static HistoricoState erro(String mensagem) {
        return new HistoricoState(false, null, null, mensagem);
    }

    public boolean isLoading() { return loading; }
    public List<HistoricoItem> getItens() { return itens; }
    public ResumoEstatistico getResumo() { return resumo; }
    public String getMensagemErro() { return mensagemErro; }
}