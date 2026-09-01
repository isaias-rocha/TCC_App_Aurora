package com.equipe1.aurora.ui.profile;

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

import com.google.android.material.imageview.ShapeableImageView;
import com.equipe1.aurora.R;

public class ProfileFragment extends Fragment {

    private ShapeableImageView imgProfileLarge;

    // Criando o "lançador" que abre a galeria do celular
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                // Esse código roda quando o usuário escolhe a foto ou cancela
                if (uri != null) {
                    // Se ele escolheu uma foto, coloca ela no ImageView
                    imgProfileLarge.setImageURI(uri);
                    Toast.makeText(requireContext(), "Foto atualizada!", Toast.LENGTH_SHORT).show();

                    // OBS: Em um app real, aqui você enviaria esse 'uri' para o Firebase Storage ou seu Banco de Dados.
                } else {
                    Toast.makeText(requireContext(), "Nenhuma foto selecionada", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



    }
}