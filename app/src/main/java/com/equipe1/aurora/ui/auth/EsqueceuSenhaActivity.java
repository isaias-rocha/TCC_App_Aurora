package com.equipe1.aurora.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.ActivityEsqueceuSenhaBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EsqueceuSenhaActivity extends AppCompatActivity {
    // Instância da classe de Binding que substitui os componentes da interface (Views)
    private ActivityEsqueceuSenhaBinding binding;

    // Instância do ViewModel que contém a lógica de negócios da tela
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Infla o layout usando o View Binding.
        // O método inflate - cria as referências para todos os IDs definidos no XML.
        binding = ActivityEsqueceuSenhaBinding.inflate(getLayoutInflater());

        // 2. Define a View raiz (root) do Binding como o conteúdo visual da Activity.
        setContentView(binding.getRoot());

        // 3. Inicializa o ViewModel através do ViewModelProvider.
        // Mantém os dados da tela salvos mesmo em mudanças de configuração (ex: rotação de tela).
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 4. Configura as funções da Activity
        configurarObservadores();
        configurarCliques();
    }
    private void configurarObservadores() {
        // Observa erros de validação do campo de e-mail
        viewModel.getEmailEsqueceuError().observe(this, erro -> {
            if (erro != null) {
                binding.etEmail.setError(erro);
                binding.etEmail.requestFocus(); // Foca o cursor no campo com erro
            }
        });

        // Observa mensagens genéricas de feedback (Toasts) enviadas pelo ViewModel
        viewModel.getMensagemToast().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Observa o resultado do envio do e-mail para autorizar a navegação
        viewModel.getEnvioEmailSucesso().observe(this, sucesso -> {
            if (sucesso) {
                // Navega para a tela do código OTP, apenas se o envio for bem-sucedido
                Intent intent = new Intent(EsqueceuSenhaActivity.this, OtpActivity.class);
                startActivity(intent);

                // Finaliza a tela atual para não permitir que o usuário volte pressionando "Voltar"
                finish();
            }
        });
    }

     // Configura os ouvintes de clique (Click Listeners) para os componentes da interface.
    private void configurarCliques() {
        // Botão de ícone "Voltar": fecha a Activity atual e retorna à tela anterior (Login)
        binding.btnVoltar.setOnClickListener(v -> finish());

        // Texto "Voltar para Login": mesmo comportamento do botão de voltar
        binding.tvVoltarLogin.setOnClickListener(v -> finish());

        // Botão "Enviar Código": dispara o processo de recuperação no ViewModel
        binding.btnEnviarCodigo.setOnClickListener(v -> {
            // Obtém e limpa o texto digitado no campo de e-mail
            String email = binding.etEmail.getText() != null
                    ? binding.etEmail.getText().toString().trim()
                    : "";

            // Envia a requisição para a camada de negócios (ViewModel)
            // A navegação será tratada no observador - getEnvioEmailSucesso()
            viewModel.enviarEmailRecuperacao(email);
        });
    }
}