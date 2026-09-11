package com.equipe1.aurora.ui.contatos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// Importação do ViewBinding gerado automaticamente para o fragment_contatos.xml
import com.equipe1.aurora.databinding.FragmentContatosBinding;
// Importação da classe do DialogFragment do pop-up
import com.equipe1.aurora.ui.solicitacao.AdicionarContatoDialogFragment;

public class ContatosFragment extends Fragment {

    // Objeto de binding que substitui o uso de findViewById
    private FragmentContatosBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla o layout da tela de contatos usando ViewBinding
        binding = FragmentContatosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ação de clique no botão "Adicionar contato"
        binding.btnAdicionarContato.setOnClickListener(v -> {
            // Instancia a janela pop-up
            AdicionarContatoDialogFragment dialog = new AdicionarContatoDialogFragment();

            // Abre o pop-up usando o gerenciador de fragmentos filho
            dialog.show(getChildFragmentManager(), "AdicionarContatoDialog");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpa a referência do binding para evitar vazamento de memória (Memory Leak)
        binding = null;
    }
}