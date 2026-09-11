package com.equipe1.aurora.ui.solicitacao;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Import gerado automaticamente pelo Android Studio a partir de activity_solicitacao_amizade.xml
import com.equipe1.aurora.databinding.ActivitySolicitacaoAmizadeBinding;

public class SolicitacaoAmizadeActivity extends AppCompatActivity {
    // Declaração do objeto de Binding que substitui o findViewById e dá acesso direto às Views
    private ActivitySolicitacaoAmizadeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Habilita a renderização sob as barras de sistema (Status Bar e Navigation Bar)
        EdgeToEdge.enable(this);

        // 1. Infla o layout usando a classe de Binding
        binding = ActivitySolicitacaoAmizadeBinding.inflate(getLayoutInflater());

        // 2. Define o elemento raiz (ScrollView no seu XML) como a visualização da Activity
        setContentView(binding.getRoot());

        // 3. Métodos organizacionais
        configurarWindowInsets();
        configurarCliques();
    }

    /**
     * Garante que os componentes da interface não fiquem cobertos pela barra de status
     * ou barra de navegação do dispositivo ao usar EdgeToEdge.
     */
    private void configurarWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Aplica o espaçamento necessário nas bordas da ScrollView raiz
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Centraliza os eventos de clique dos botões e elementos interativos.
     */
    private void configurarCliques() {
        // Botão de voltar: encerra a Activity e retorna à tela anterior da pilha
        binding.btnVoltar.setOnClickListener(v -> finish());

        // Botão de enviar solicitação: recupera os dados digitados e aciona a lógica necessária
        binding.btnEnviarSolicitacao.setOnClickListener(v -> {
            String email = binding.etBuscaUsuario.getText() != null
                    ? binding.etBuscaUsuario.getText().toString().trim()
                    : "";

            String telefone = binding.etBuscaTelefone.getText() != null
                    ? binding.etBuscaTelefone.getText().toString().trim()
                    : "";

            // A ser integrado com o SolicitacaoViewModel futuramente
            processarEnvioSolicitacao(email, telefone);
        });
    }

    /**
     * Exemplo de lógica para enviar os dados digitados ao ViewModel.
     */
    private void processarEnvioSolicitacao(String email, String telefone) {
        // TODO: Chame o método correspondente no ViewModel para realizar a busca/envio
    }

    /**
     * Método utilitário que você pode usar ao integrar com LiveData/StateFlow para
     * exibir ou ocultar o card quando um usuário for localizado.
     */
    public void alternarVisibilidadeCardUsuario(boolean exibir) {
        binding.cardUsuario.setVisibility(exibir ? View.VISIBLE : View.GONE);
    }
}