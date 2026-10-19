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
    private static final String KEY_IDIOMA = "key_idioma";
    private ActivityMainBinding binding;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferences = getSharedPreferences("AuroraPrefs", MODE_PRIVATE);

        // Recupera o NavHostFragment responsável por gerenciar o ciclo de vida dos Fragments no NavGraph
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // 1. BottomNavigationView Listener (Sincronização da barra inferior)
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.placeholder) return false;

                if (itemId == R.id.nav_menu) {
                    binding.drawerLayout.openDrawer(GravityCompat.START);
                    return false;
                }

                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() == itemId) {
                    return true;
                }

                NavOptions navOptions = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(R.id.nav_home, false, true)
                        .build();

                try {
                    navController.navigate(itemId, null, navOptions);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });

            // 2. Botão SOS Central
            binding.btnSos.setOnClickListener(v -> {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.nav_sos) {

                    NavOptions navOptions = new NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(R.id.nav_home, false)
                            .build();

                    navController.navigate(R.id.nav_sos, null, navOptions);
                }
            });

            // 3. Drawer Listener (Menu Lateral)
            binding.navigationView.setNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.drawer_perfil) {
                    try {
                        navController.navigate(R.id.nav_perfil);
                    } catch (Exception e) {
                        navController.navigate(R.id.drawer_perfil);                    }
                }
                else if (itemId == R.id.drawer_dispositivo) {
                    try {
                        navController.navigate(R.id.drawer_dispositivo);
                    } catch (Exception e) {
                        navController.navigate(R.id.drawer_dispositivo);
                    }
                }
                else if (itemId == R.id.drawer_mapa) {
                    try {
                        navController.navigate(R.id.nav_mapa);
                    } catch (Exception e) {
                        navController.navigate(R.id.drawer_mapa);
                    }
                }
                // Activities legadas continuam sendo abertas via Intent tradicional
                else if (itemId == R.id.drawer_historico) {
                    startActivity(new Intent(this, HistoricoActivity.class));
                }
                else if (itemId == R.id.drawer_planos) {
                    startActivity(new Intent(this, PlanosAssinaturaActivity.class));
                }
                else if (itemId == R.id.drawer_solicitacoes) {
                    startActivity(new Intent(this, SolicitacaoAmizadeActivity.class));
                }
                else if (itemId == R.id.drawer_configuracoes) {
                    startActivity(new Intent(this, ConfigActivity.class));
                }
                else if (itemId == R.id.drawer_acessibilidade) {
                    startActivity(new Intent(this, AcessibilidadeActivity.class));
                }
                else if (itemId == R.id.drawer_idioma) {
                    abrirDialogoIdioma();
                }
                else if (itemId == R.id.drawer_logout) {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                binding.drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });

            // 4. Sincroniza ícones ativos da BottomBar de acordo com o Fragment visível
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
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

            // 5. Trata intents de atalhos e notificações em segundo plano para o Mapa
            verificarIntentRedirecionamentoMapa(getIntent(), navController);
        }

        // Trata o botão "Voltar" nativo do sistema fechando o Drawer caso ele esteja aberto
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

    private void verificarIntentRedirecionamentoMapa(Intent intent, NavController navController) {
        if (intent != null && intent.getBooleanExtra("ABRIR_MAPA", false)) {
            double lat = intent.getDoubleExtra("LATITUDE_ESP32", 0.0);
            double lng = intent.getDoubleExtra("LONGITUDE_ESP32", 0.0);

            Bundle bundle = new Bundle();
            bundle.putDouble("LATITUDE_ESP32", lat);
            bundle.putDouble("LONGITUDE_ESP32", lng);

            navController.navigate(R.id.nav_mapa, bundle);
        }
    }

    /* ============================================================================================
       LÓGICA: SELEÇÃO DE IDIOMA
       ============================================================================================ */

    private void abrirDialogoIdioma() {
        String[] idiomas = {
                "Português (Brasil)",
                "English (US)",
                "Español (España)",
                "Deutsch (Deutschland)",
                "Русский (Россия)"
        };

        String[] tagsLinguagem = {
                "pt-BR",
                "en-US",
                "es-ES",
                "de-DE",
                "ru-RU"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Selecionar Idioma")
                .setItems(idiomas, (dialog, which) -> {
                    String tagSelecionada = tagsLinguagem[which];

                    preferences.edit().putString(KEY_IDIOMA, tagSelecionada).apply();

                    // Aplica o novo idioma globalmente no app via AppCompatDelegate
                    LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(tagSelecionada);
                    AppCompatDelegate.setApplicationLocales(appLocale);

                    Toast.makeText(this, "Idioma alterado para: " + idiomas[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /* ============================================================================================
       LÓGICA: SOBRE O APLICATIVO
       ============================================================================================ */

    private void abrirDialogoSobre() {
        String versaoApp = obterVersaoDoApp();

        String mensagemSobre = "Projeto Aurora\n" +
                "Versão: " + versaoApp + "\n\n" +
                "Integrantes: Gilson Reis, Isaias Rocha De Jesus, Luis Fernando, Matheus Trindade\n\n" +
                "Desenvolvido pela Equipe 1 do Curso Técnico de Desenvolvimento de Sistemas 2025/2026, com foco em acessibilidade, segurança e melhor experiência do usuário.";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sobre o Aurora")
                .setMessage(mensagemSobre)
                .setPositiveButton("Fechar", null)
                .setNeutralButton("Termos & Privacidade", (dialog, which) -> {
                    Toast.makeText(this, "Abrindo Termos de Uso...", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String obterVersaoDoApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageInfo pInfo = getPackageManager().getPackageInfo(
                        getPackageName(),
                        PackageManager.PackageInfoFlags.of(0)
                );
                return pInfo.versionName;
            } else {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                return pInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }
}