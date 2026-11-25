package com.equipe1.aurora.ui.solicitacao;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.equipe1.aurora.databinding.DialogAdicionarContatoBinding;

public class AdicionarContatoDialogFragment extends DialogFragment {

    private DialogAdicionarContatoBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAdicionarContatoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ação do Botão Voltar (Fecha a janelinha)
        binding.btnVoltar.setOnClickListener(v -> dismiss());

        // Ação do Botão Enviar
        binding.btnEnviar.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
            String telefone = binding.etTelefone.getText() != null ? binding.etTelefone.getText().toString().trim() : "";
            Toast.makeText(getContext(), "Click enviar solicitação teste", Toast.LENGTH_SHORT).show();

            // TODO: Adicione sua lógica para enviar a solicitação aqui...

            dismiss(); // Fecha a caixa dialog central
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Torna o fundo do Dialog transparente para destacar as bordas arredondadas do CardView
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // Define a largura do popup para 90% da largura da tela do usuário
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Evita vazamento de memória
    }
}