package com.equipe1.aurora.ui.perfil;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// View Binding gerado a partir do fragment_perfil.xml
import com.equipe1.aurora.databinding.FragmentPerfilBinding;

// Importações das Activities de destino
import com.equipe1.aurora.ui.config.ConfigActivity;
import com.equipe1.aurora.ui.contatos.ContatosActivity;
import com.equipe1.aurora.ui.historico.HistoricoActivity;
import com.equipe1.aurora.ui.planos.PlanosAssinaturaActivity;

public class PerfilFragment extends Fragment {

    // Referência do View Binding
    private FragmentPerfilBinding binding;

    // Lançador do seletor de fotos da galeria (Photo Picker)
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null && binding != null) {
                    // Define a foto escolhida na ImageView do perfil
                    binding.imgPerfilAvatar.setImageURI(uri);
                    Toast.makeText(requireContext(), "Foto atualizada!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Nenhuma foto selecionada", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla o layout usando o View Binding
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Clique no avatar para trocar a foto de perfil
        binding.imgPerfilAvatar.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // Inicializa as ações de navegação dos menus
        initMenuListeners();
    }

    /**
     * Mapeia os eventos de clique dos itens do menu para suas respectivas Activities.
     */
    private void initMenuListeners() {

        // 1. Informações Pessoais -> InfoPessoaisActivity
        binding.menuInfoPessoais.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), InfoPessoaisActivity.class));
        });

        // 2. Dispositivo Embarcado (Mensagem temporária)
        binding.menuDispositivoEmbarcado.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Em breve: Dispositivo Embarcado", Toast.LENGTH_SHORT).show();
        });

        // 3. Contatos -> ContatosActivity
        binding.menuContatos.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ContatosActivity.class));
        });

        // 4. Histórico -> HistoricoActivity
        binding.menuHistorico.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), HistoricoActivity.class));
        });

        // 5. Planos de Assinatura -> PlanosAssinaturaActivity
        binding.menuPlanoAssinatura.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), PlanosAssinaturaActivity.class));
        });

        // 6. Configurações -> ConfigActivity
        binding.menuConfiguracoes.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ConfigActivity.class));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Libera o binding para evitar vazamento de memória (Memory Leak)
        binding = null;
    }
}