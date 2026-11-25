package com.equipe1.aurora.ui.perfil;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Importação do binding gerado automaticamente com base no arquivo fragment_perfil.xml
import com.equipe1.aurora.databinding.FragmentPerfilBinding;

public class PerfilActivity extends AppCompatActivity {

    // Declaração do objeto de binding
    private FragmentPerfilBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Infla o layout via View Binding
        binding = FragmentPerfilBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Acesso direto ao layout principal usando binding.layoutPerfil
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutPerfil, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


    }
}