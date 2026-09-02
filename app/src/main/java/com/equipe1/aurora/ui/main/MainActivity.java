package com.equipe1.aurora.ui.main;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // 1. Clique nas abas do BottomNavigationView
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                // Ignora clique no espaço do SOS
                if (itemId == R.id.placeholder) {
                    return false;
                }

                // Se já estiver na tela selecionada, não faz nada
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() == itemId) {
                    return true;
                }

                // Configura navegação sem acumular histórico nem congelar a tela
                NavOptions navOptions = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(R.id.nav_home, false, true)
                        .build();

                try {
                    navController.navigate(itemId, null, navOptions);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

            // 2. Clique no Botão SOS
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

            // 3. Atualiza o ícone aceso de acordo com a tela visível
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
        }
    }
}