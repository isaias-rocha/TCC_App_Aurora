package com.equipe1.aurora.ui.perfil;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

// Importação da classe de binding gerada automaticamente
import com.equipe1.aurora.databinding.ActivityInfoPessoaisBinding;

public class InfoPessoaisActivity extends AppCompatActivity {

    // Declaração da variável de binding
    private ActivityInfoPessoaisBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Habilita o desenho atrás das barras do sistema (Edge-to-Edge)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 2. Infla o layout usando a classe de binding gerada
        binding = ActivityInfoPessoaisBinding.inflate(getLayoutInflater());

        // 3. Define a View raiz
        setContentView(binding.getRoot());

        // 4. Aplica os insets do sistema (status/navigation bar) como padding no ScrollView
        ViewCompat.setOnApplyWindowInsetsListener(binding.personalInfo, (v, windowInsets) -> {
            // Captura barras do sistema (status bar / nav bar) E o teclado (ime)
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            // Usa a maior margem inferior entre a barra de navegação e o teclado
            int bottomPadding = Math.max(systemBars.bottom, ime.bottom);

            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    bottomPadding
            );

            return windowInsets;
        });

        // 5. Inicializa os ouvintes de eventos e lógicas
        configurarListeners();
    }

    private void configurarListeners() {
        // Botão Voltar
        binding.btnVoltar.setOnClickListener(v -> finish());

        // Botão Alterar Foto de Capa
        binding.cardAlterarFotoBg.setOnClickListener(v -> {
            Toast.makeText(this, "Alterar foto de capa", Toast.LENGTH_SHORT).show();
        });

        // Botão Alterar Foto de Perfil
        binding.cardAlterarFotoPerfil.setOnClickListener(v -> {
            Toast.makeText(this, "Alterar foto de perfil", Toast.LENGTH_SHORT).show();
        });

        // Botão Salvar Alterações
        binding.btnSalvarInformacoes.setOnClickListener(v -> salvarDados());
    }

    private void salvarDados() {
        // Lendo textos dos TextInputEditText diretamente
        String nome = binding.etNomePessoal.getText() != null ? binding.etNomePessoal.getText().toString() : "";
        String email = binding.etEmailPessoal.getText() != null ? binding.etEmailPessoal.getText().toString() : "";
        String telefone = binding.etTelefonePessoal.getText() != null ? binding.etTelefonePessoal.getText().toString() : "";
        String senha = binding.etSenhaCadastro.getText() != null ? binding.etSenhaCadastro.getText().toString() : "";
        String confirmarSenha = binding.etConfirmarSenhaCadastro.getText() != null ? binding.etConfirmarSenhaCadastro.getText().toString() : "";

        // Validação simples
        if (nome.isEmpty()) {
            binding.tilNomePessoal.setError("Informe seu nome");
            return;
        } else {
            binding.tilNomePessoal.setError(null);
        }

        if (!senha.equals(confirmarSenha)) {
            binding.tilConfirmarSenha.setError("As senhas não coincidem");
            return;
        } else {
            binding.tilConfirmarSenha.setError(null);
        }

        // Lógica para salvar no banco de dados / API...
        Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show();
    }
}