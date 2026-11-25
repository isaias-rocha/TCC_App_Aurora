package com.equipe1.aurora.ui.perfil;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.equipe1.aurora.databinding.ActivityInfoPessoaisBinding;

public class InfoPessoaisActivity extends AppCompatActivity {

    private ActivityInfoPessoaisBinding binding;

    // Declaração do lançador do Photo Picker como campo da classe
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null && binding != null) {
                    binding.imgFotoPerfil.setImageURI(uri);
                    Toast.makeText(this, "Foto atualizada!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Nenhuma foto selecionada", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 2. Inflar binding
        binding = ActivityInfoPessoaisBinding.inflate(getLayoutInflater());

        // 3. Set content view
        setContentView(binding.getRoot());

        // 4. Insets (status/navigation bar + teclado)
        ViewCompat.setOnApplyWindowInsetsListener(binding.personalInfo, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            int bottomPadding = Math.max(systemBars.bottom, ime.bottom);

            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    bottomPadding
            );

            return windowInsets;
        });

        // 5. Inicializar listeners
        configurarListeners();
    }

    private void configurarListeners() {
        // Botão Voltar
        binding.btnVoltar.setOnClickListener(v -> finish());

        // Abrir galeria ao clicar no avatar ou no botão da câmera
        binding.imgFotoPerfil.setOnClickListener(v -> abrirSeletorDeFoto());
        binding.cardAlterarFotoPerfil.setOnClickListener(v -> abrirSeletorDeFoto());

        // Botão Salvar Alterações
        binding.btnSalvarInformacoes.setOnClickListener(v -> salvarDados());
    }

    private void abrirSeletorDeFoto() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void salvarDados() {
        String nome = binding.etNomePessoal.getText() != null ? binding.etNomePessoal.getText().toString().trim() : "";
        String email = binding.etEmailPessoal.getText() != null ? binding.etEmailPessoal.getText().toString().trim() : "";
        String telefone = binding.etTelefonePessoal.getText() != null ? binding.etTelefonePessoal.getText().toString().trim() : "";
        String senha = binding.etSenhaCadastro.getText() != null ? binding.etSenhaCadastro.getText().toString() : "";
        String confirmarSenha = binding.etConfirmarSenhaCadastro.getText() != null ? binding.etConfirmarSenhaCadastro.getText().toString() : "";

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

        Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show();
    }
}