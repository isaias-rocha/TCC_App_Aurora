// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Gerencia o estado reativo (LiveData) da tela de Acessibilidade seguindo o
// padrão MVVM. Atua como intermediário entre a AcessibilidadeActivity e os
// casos de uso (UseCases) da camada domain, mantendo os estados em memória
// durante mudanças de configuração (como troca de tema ou rotação de tela).
// =========================================================================

package com.equipe1.aurora.ui.acessibilidade;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.equipe1.aurora.domain.model.ConfiguracaoAcessibilidade;
import com.equipe1.aurora.domain.model.CorPrincipal;
import com.equipe1.aurora.domain.model.TipoDaltonismo;
import com.equipe1.aurora.domain.repository.AcessibilidadeRepository;
import com.equipe1.aurora.domain.usecase.ObterConfiguracoesAcessibilidadeUseCase;
import com.equipe1.aurora.domain.usecase.SalvarConfiguracoesAcessibilidadeUseCase;
import com.equipe1.aurora.infra.repository.AcessibilidadeRepositoryImpl;
import com.equipe1.aurora.infra.storage.AcessibilidadePreferencesStorage;

public class AcessibilidadeViewModel extends AndroidViewModel {

    // -------------------------------------------------------------------------
    // SEÇÃO: CASOS DE USO (CAMADA DOMAIN)
    // -------------------------------------------------------------------------
    private final ObterConfiguracoesAcessibilidadeUseCase obterConfiguracoesUseCase;
    private final SalvarConfiguracoesAcessibilidadeUseCase salvarConfiguracoesUseCase;

    // -------------------------------------------------------------------------
    // SEÇÃO: ESTADOS OBSERVÁVEIS INTERNOS (MUTABLE LIVEDATA)
    // -------------------------------------------------------------------------
    private final MutableLiveData<Boolean> modoEscuro = new MutableLiveData<>();
    private final MutableLiveData<CorPrincipal> corPrincipal = new MutableLiveData<>();
    private final MutableLiveData<Boolean> daltonismoAtivo = new MutableLiveData<>();
    private final MutableLiveData<TipoDaltonismo> tipoDaltonismo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> altoContraste = new MutableLiveData<>();
    private final MutableLiveData<Integer> tamanhoFonteIndex = new MutableLiveData<>();
    private final MutableLiveData<Boolean> vibracaoAtiva = new MutableLiveData<>();
    private final MutableLiveData<Boolean> talkbackOtimizado = new MutableLiveData<>();

    // -------------------------------------------------------------------------
    // SEÇÃO: CONSTRUTOR E INICIALIZAÇÃO DAS DEPENDÊNCIAS
    // -------------------------------------------------------------------------
    public AcessibilidadeViewModel(@NonNull Application application) {
        super(application);

        // Conecta Storage (SharedPreferences) -> Repository -> UseCases
        AcessibilidadePreferencesStorage storage = new AcessibilidadePreferencesStorage(application);
        AcessibilidadeRepository repository = new AcessibilidadeRepositoryImpl(storage);
        this.obterConfiguracoesUseCase = new ObterConfiguracoesAcessibilidadeUseCase(repository);
        this.salvarConfiguracoesUseCase = new SalvarConfiguracoesAcessibilidadeUseCase(repository);

        carregarPreferencias();
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: CARREGAMENTO INICIAL DAS PREFERÊNCIAS SALVAS
    // -------------------------------------------------------------------------
    private void carregarPreferencias() {
        ConfiguracaoAcessibilidade config = obterConfiguracoesUseCase.executar();
        modoEscuro.setValue(config.isModoEscuro());
        corPrincipal.setValue(config.getCorPrincipal());
        daltonismoAtivo.setValue(config.isDaltonismoAtivo());
        tipoDaltonismo.setValue(config.getTipoDaltonismo());
        altoContraste.setValue(config.isAltoContraste());
        tamanhoFonteIndex.setValue(config.getTamanhoFonteIndex());
        vibracaoAtiva.setValue(config.isVibracaoAtiva());
        talkbackOtimizado.setValue(config.isTalkbackOtimizado());
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: GETTERS PÚBLICOS (EXPOSIÇÃO DE LIVEDATA IMUTÁVEL PARA A UI)
    // -------------------------------------------------------------------------
    public LiveData<Boolean> getModoEscuro() { return modoEscuro; }
    public LiveData<CorPrincipal> getCorPrincipal() { return corPrincipal; }
    public LiveData<Boolean> getDaltonismoAtivo() { return daltonismoAtivo; }
    public LiveData<TipoDaltonismo> getTipoDaltonismo() { return tipoDaltonismo; }
    public LiveData<Boolean> getAltoContraste() { return altoContraste; }
    public LiveData<Integer> getTamanhoFonteIndex() { return tamanhoFonteIndex; }
    public LiveData<Boolean> getVibracaoAtiva() { return vibracaoAtiva; }
    public LiveData<Boolean> getTalkbackOtimizado() { return talkbackOtimizado; }

    // -------------------------------------------------------------------------
    // SEÇÃO: AÇÕES DO USUÁRIO E PERSISTÊNCIA VIA USECASES
    // -------------------------------------------------------------------------
    public void selecionarModoEscuro(boolean escuro) {
        modoEscuro.setValue(escuro);
        salvarConfiguracoesUseCase.salvarModoEscuro(escuro);
    }

    public void selecionarCorPrincipal(CorPrincipal cor) {
        corPrincipal.setValue(cor);
        salvarConfiguracoesUseCase.salvarCorPrincipal(cor);
    }

    public void alternarDaltonismo(boolean ativo) {
        daltonismoAtivo.setValue(ativo);
        salvarConfiguracoesUseCase.salvarDaltonismoAtivo(ativo);
    }

    public void selecionarTipoDaltonismo(TipoDaltonismo tipo) {
        tipoDaltonismo.setValue(tipo);
        salvarConfiguracoesUseCase.salvarTipoDaltonismo(tipo);
    }

    public void alternarAltoContraste(boolean ativo) {
        altoContraste.setValue(ativo);
        salvarConfiguracoesUseCase.salvarAltoContraste(ativo);
    }

    public void selecionarTamanhoFonte(int index) {
        tamanhoFonteIndex.setValue(index);
        salvarConfiguracoesUseCase.salvarTamanhoFonteIndex(index);
    }

    public void alternarVibracao(boolean ativa) {
        vibracaoAtiva.setValue(ativa);
        salvarConfiguracoesUseCase.salvarVibracaoAtiva(ativa);
    }

    public void alternarTalkback(boolean ativo) {
        talkbackOtimizado.setValue(ativo);
        salvarConfiguracoesUseCase.salvarTalkbackOtimizado(ativo);
    }
}