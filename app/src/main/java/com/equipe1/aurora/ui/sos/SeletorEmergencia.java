package com.equipe1.aurora.ui.sos;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.equipe1.aurora.databinding.DialogSeletorEmergenciaBinding;

/**
 * Activity opcional para a tela de seleção de chamadas de emergência (Polícia, SAMU e Bombeiros).
 * Nota: Os botões também podem ser inflados como modal (AlertDialog) dentro do SosFragment.
 */
public class SeletorEmergencia extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Habilita layout Edge-to-Edge
        EdgeToEdge.enable(this);

        // Infla o layout com ViewBinding
        // ViewBinding do layout do diálogo/seletor
        DialogSeletorEmergenciaBinding binding = DialogSeletorEmergenciaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Aplica o tratamento das margens das barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configura o botão de fechar a tela
        binding.btnVoltar.setOnClickListener(v -> finish());
    }
}