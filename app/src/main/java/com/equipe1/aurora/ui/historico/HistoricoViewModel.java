package com.equipe1.aurora.ui.historico;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.equipe1.aurora.domain.model.FiltroHistorico;
import com.equipe1.aurora.domain.model.HistoricoItem;
import com.equipe1.aurora.domain.model.ResumoEstatistico;
import com.equipe1.aurora.domain.usecase.ObterHistoricoUseCase;
import com.equipe1.aurora.domain.usecase.ObterResumoEstatisticoUseCase;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel responsável pela gestão do estado da UI do Histórico e
 * integração com os UseCases do Clean Architecture.
 */
public class HistoricoViewModel extends ViewModel {

    private final ObterHistoricoUseCase obterHistoricoUseCase;
    private final ObterResumoEstatisticoUseCase obterResumoEstatisticoUseCase;

    private final MutableLiveData<List<HistoricoItem>> _historicoItems = new MutableLiveData<>();
    public LiveData<List<HistoricoItem>> historicoItems = _historicoItems;

    private final MutableLiveData<ResumoEstatistico> _resumoEstatistico = new MutableLiveData<>();
    public LiveData<ResumoEstatistico> resumoEstatistico = _resumoEstatistico;

    private final MutableLiveData<Boolean> _carregando = new MutableLiveData<>(false);
    public LiveData<Boolean> carregando = _carregando;

    private final MutableLiveData<String> _mensagemErro = new MutableLiveData<>();
    public LiveData<String> mensagemErro = _mensagemErro;

    private List<HistoricoItem> listaCache = new ArrayList<>();

    public HistoricoViewModel(ObterHistoricoUseCase obterHistoricoUseCase,
                              ObterResumoEstatisticoUseCase obterResumoEstatisticoUseCase) {
        this.obterHistoricoUseCase = obterHistoricoUseCase;
        this.obterResumoEstatisticoUseCase = obterResumoEstatisticoUseCase;
    }

    /**
     * Carrega o histórico consolidado (Room + MySQL via ponte)
     */
    public void carregarHistorico(long idUsuario) {
        _carregando.setValue(true);

        obterHistoricoUseCase.executar(idUsuario, null, new ObterHistoricoUseCase.Callback() {
            @Override
            public void onSuccess(List<HistoricoItem> itens) {
                listaCache = itens;
                _historicoItems.postValue(itens);
                _carregando.postValue(false);
            }

            @Override
            public void onError(Throwable throwable) {
                _carregando.postValue(false);
                _mensagemErro.postValue("Erro ao carregar o histórico: " + throwable.getMessage());
            }
        });

        obterResumoEstatisticoUseCase.executar(idUsuario, new ObterResumoEstatisticoUseCase.Callback() {
            @Override
            public void onSuccess(ResumoEstatistico resumo) {
                _resumoEstatistico.postValue(resumo);
            }

            @Override
            public void onError(Throwable throwable) {
                // Silencioso ou log interno
            }
        });
    }

    /**
     * Filtra a lista atual em memória com base no tipo selecionado nos Chips M3
     */
    public void filtrarPorTipo(HistoricoItem.TipoHistorico tipo) {
        if (tipo == null) {
            _historicoItems.setValue(listaCache);
            return;
        }

        List<HistoricoItem> filtrados = new ArrayList<>();
        for (HistoricoItem item : listaCache) {
            if (item.getTipo() == tipo) {
                filtrados.add(item);
            }
        }
        _historicoItems.setValue(filtrados);
    }
}