package com.equipe1.aurora.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.ActivityMainBinding;
import com.equipe1.aurora.ui.historico.HistoricoActivity;
import com.equipe1.aurora.ui.planos.PlanosAssinaturaActivity;
import com.equipe1.aurora.ui.solicitacao.SolicitacaoAmizadeActivity;
import com.equipe1.aurora.ui.config.ConfigActivity;
import com.equipe1.aurora.ui.acessibilidade.AcessibilidadeActivity;
import com.equipe1.aurora.ui.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private static final String CHAVE_IDIOMA = "key_idioma";
    private ActivityMainBinding binding;
    private SharedPreferences preferencias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflação da Activity usando ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferencias = getSharedPreferences("AuroraPrefs", MODE_PRIVATE);

        // Inicialização do NavHostFragment do Navigation Component
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController controladorNavegacao = navHostFragment.getNavController();

            // 1. Configuração da barra de navegação inferior (BottomNavigationView)
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.placeholder) return false;

                if (itemId == R.id.nav_menu) {
                    binding.drawerLayout.openDrawer(GravityCompat.START);
                    return false;
                }

                if (controladorNavegacao.getCurrentDestination() != null &&
                        controladorNavegacao.getCurrentDestination().getId() == itemId) {
                    return true;
                }

                NavOptions opcoesNavegacao = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(R.id.nav_home, false, true)
                        .build();

                try {
                    controladorNavegacao.navigate(itemId, null, opcoesNavegacao);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });

            // 2. Ação do Botão SOS Central
            binding.btnSos.setOnClickListener(v -> {
                if (controladorNavegacao.getCurrentDestination() != null &&
                        controladorNavegacao.getCurrentDestination().getId() != R.id.nav_sos) {

                    NavOptions opcoesNavegacao = new NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(R.id.nav_home, false)
                            .build();

                    controladorNavegacao.navigate(R.id.nav_sos, null, opcoesNavegacao);
                }
            });

            // 3. Menu Lateral (Drawer Navigation)
            binding.navigationView.setNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.drawer_perfil) {
                    try {
                        controladorNavegacao.navigate(R.id.nav_perfil);
                    } catch (Exception e) {
                        controladorNavegacao.navigate(R.id.drawer_perfil);
                    }
                } else if (itemId == R.id.drawer_dispositivo) {
                    try {
                        controladorNavegacao.navigate(R.id.drawer_dispositivo);
                    } catch (Exception e) {
                        controladorNavegacao.navigate(R.id.drawer_dispositivo);
                    }
                } else if (itemId == R.id.drawer_mapa) {
                    try {
                        controladorNavegacao.navigate(R.id.nav_mapa);
                    } catch (Exception e) {
                        controladorNavegacao.navigate(R.id.drawer_mapa);
                    }
                } else if (itemId == R.id.drawer_historico) {
                    startActivity(new Intent(this, HistoricoActivity.class));
                } else if (itemId == R.id.drawer_planos) {
                    startActivity(new Intent(this, PlanosAssinaturaActivity.class));
                } else if (itemId == R.id.drawer_solicitacoes) {
                    startActivity(new Intent(this, SolicitacaoAmizadeActivity.class));
                } else if (itemId == R.id.drawer_configuracoes) {
                    startActivity(new Intent(this, ConfigActivity.class));
                } else if (itemId == R.id.drawer_acessibilidade) {
                    startActivity(new Intent(this, AcessibilidadeActivity.class));
                } else if (itemId == R.id.drawer_idioma) {
                    abrirDialogoIdioma();
                } else if (itemId == R.id.drawer_logout) {
                    // Confirmação ao clicar em Sair do Menu Lateral
                    exibirDialogoConfirmacaoLogout();
                }

                binding.drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });

            // 4. Sincronização automática do item selecionado na BottomBar
            controladorNavegacao.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                MenuItem menuItem = binding.bottomNavigation.getMenu().findItem(id);

                if (menuItem != null) {
                    menuItem.setChecked(true);
                } else if (id == R.id.nav_sos) {
                    MenuItem placeholder = binding.bottomNavigation.getMenu().findItem(R.id.placeholder);
                    if (placeholder != null) {
                        placeholder.setChecked(true);
                    }
                }
            });

            // 5. Verificação de redirecionamentos diretos ao mapa
            verificarIntentRedirecionamentoMapa(getIntent(), controladorNavegacao);
        }

        // Tratamento prioritário do botão Voltar para fechar o Drawer se estiver aberto
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // Diálogo de confirmação acionado pelo botão Logout do Drawer
    private void exibirDialogoConfirmacaoLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Encerrar Sessão")
                .setMessage("Deseja realmente sair da sua conta?")
                .setPositiveButton("Sair", (dialog, which) -> realizarLogout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Executa a saída limpando as rotas da pilha
    private void realizarLogout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            verificarIntentRedirecionamentoMapa(intent, navHostFragment.getNavController());
        }
    }

    private void verificarIntentRedirecionamentoMapa(Intent intent, NavController controladorNavegacao) {
        if (intent != null && intent.getBooleanExtra("ABRIR_MAPA", false)) {
            double latitude = intent.getDoubleExtra("LATITUDE_DISPOSITIVO", 0.0);
            double longitude = intent.getDoubleExtra("LONGITUDE_DISPOSITIVO", 0.0);

            Bundle parametros = new Bundle();
            parametros.putDouble("LATITUDE_DISPOSITIVO", latitude);
            parametros.putDouble("LONGITUDE_DISPOSITIVO", longitude);

            controladorNavegacao.navigate(R.id.nav_mapa, parametros);
        }
    }

    private void abrirDialogoIdioma() {
        String[] idiomas = {
                "Português (Brasil)",
                "English (US)",
                "Español (España)",
                "Deutsch (Deutschland)"
        };

        String[] tagsLinguagem = {
                "pt-BR",
                "en-US",
                "es-ES",
                "de-DE"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Selecionar Idioma")
                .setItems(idiomas, (dialog, which) -> {
                    String tagSelecionada = tagsLinguagem[which];

                    preferencias.edit().putString(CHAVE_IDIOMA, tagSelecionada).apply();

                    LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(tagSelecionada);
                    AppCompatDelegate.setApplicationLocales(appLocale);

                    Toast.makeText(this, "Idioma alterado para: " + idiomas[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}