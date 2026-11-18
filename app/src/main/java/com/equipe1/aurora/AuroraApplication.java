// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Classe global de inicialização do aplicativo (Application).
// Registra observadores globais de ciclo de vida para Activities e
// Fragments, garantindo que todas as telas do app apliquem automaticamente
// o tema (Claro/Escuro), cor principal, modo daltonismo, alto contraste,
// tamanho de fonte, otimização de TalkBack e vibração.
// =========================================================================

package com.equipe1.aurora;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import com.equipe1.aurora.domain.model.ConfiguracaoAcessibilidade;
import com.equipe1.aurora.infra.repository.AcessibilidadeRepositoryImpl;
import com.equipe1.aurora.infra.storage.AcessibilidadePreferencesStorage;
import com.equipe1.aurora.ui.theme.TemaDinamicoHelper;

public class AuroraApplication extends Application {

    // -------------------------------------------------------------------------
    // SEÇÃO: INICIALIZAÇÃO GLOBAL DO APLICATIVO
    // -------------------------------------------------------------------------
    @Override
    public void onCreate() {
        super.onCreate();
        registrarObservadorGlobalDeTelas();
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: MONITORAMENTO DE TODAS AS ACTIVITIES E FRAGMENTS DO APP
    // -------------------------------------------------------------------------
    private void registrarObservadorGlobalDeTelas() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                // // 1. Aplica ThemeOverlay e escala base antes do XML ser inflado
                ConfiguracaoAcessibilidade config = carregarConfiguracoes();
                TemaDinamicoHelper.aplicarTemaNaActivity(activity, config);

                // // 2. Registra observador para todos os Fragments desta Activity
                if (activity instanceof FragmentActivity) {
                    registrarObservadorDeFragments((FragmentActivity) activity);
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                atualizarActivity(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                // // Garante atualização ao voltar da tela de Acessibilidade para telas anteriores
                atualizarActivity(activity);
            }

            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: MONITORAMENTO DE FRAGMENTS (ABAS DO MENU INFERIOR E TELAS INTERNAS)
    // -------------------------------------------------------------------------
    private void registrarObservadorDeFragments(FragmentActivity fragmentActivity) {
        fragmentActivity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {

                    @Override
                    public void onFragmentViewCreated(
                            @NonNull FragmentManager fm,
                            @NonNull Fragment f,
                            @NonNull View v,
                            @Nullable Bundle savedInstanceState
                    ) {
                        ConfiguracaoAcessibilidade config = carregarConfiguracoes();
                        TemaDinamicoHelper.aplicarConfiguracoesNaTela(fragmentActivity, v, config);
                    }

                    @Override
                    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        if (f.getView() != null) {
                            ConfiguracaoAcessibilidade config = carregarConfiguracoes();
                            TemaDinamicoHelper.aplicarConfiguracoesNaTela(fragmentActivity, f.getView(), config);
                        }
                    }
                },
                true // // Monitora também Fragments aninhados (childFragmentManager)
        );
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: UTILITÁRIOS INTERNOS DE ATUALIZAÇÃO E LEITURA DE PREFERÊNCIAS
    // -------------------------------------------------------------------------
    private void atualizarActivity(Activity activity) {
        ConfiguracaoAcessibilidade config = carregarConfiguracoes();
        View rootView = activity.findViewById(android.R.id.content);
        TemaDinamicoHelper.aplicarConfiguracoesNaTela(activity, rootView, config);
    }

    private ConfiguracaoAcessibilidade carregarConfiguracoes() {
        AcessibilidadePreferencesStorage storage = new AcessibilidadePreferencesStorage(this);
        return new AcessibilidadeRepositoryImpl(storage).obterConfiguracoes();
    }
}