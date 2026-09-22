package com.equipe1.aurora.ui.sos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.equipe1.aurora.databinding.DialogSeletorEmergenciaBinding; // <--- IMPORT OBRIGATÓRIO
import com.equipe1.aurora.databinding.FragmentSosBinding;

public class SosFragment extends Fragment {

    private FragmentSosBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSosBinding.inflate(inflater, container, false);

        configurarBotaoSos();
        configurarSeletorEmergencia();

        return binding.getRoot();
    }

    private void configurarBotaoSos() {
        binding.btnSos.setOnLongClickListener(v -> {
            Toast.makeText(requireContext(), "Localização enviada!", Toast.LENGTH_LONG).show();
            return true;
        });
    }

    private void configurarSeletorEmergencia() {
        binding.btnLigarEmergencia.setOnClickListener(v -> {
            // Infla o layout do diálogo customizado
            DialogSeletorEmergenciaBinding dialogBinding = DialogSeletorEmergenciaBinding.inflate(getLayoutInflater());

            AlertDialog.Builder construtorAlerta = new AlertDialog.Builder(requireContext());
            construtorAlerta.setView(dialogBinding.getRoot());

            AlertDialog dialogo = construtorAlerta.create();

            if (dialogo.getWindow() != null) {
                dialogo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // Ações dos botões dentro do diálogo
            dialogBinding.btnVoltar.setOnClickListener(view -> dialogo.dismiss());

            dialogBinding.btnPolicia.setOnClickListener(view -> {
                abrirDiscador("190");
                dialogo.dismiss();
            });

            dialogBinding.btnSamu.setOnClickListener(view -> {
                abrirDiscador("192");
                dialogo.dismiss();
            });

            dialogBinding.btnBombeiros.setOnClickListener(view -> {
                abrirDiscador("193");
                dialogo.dismiss();
            });

            dialogo.show();
        });
    }

    private void abrirDiscador(String numeroTelefone) {
        Intent intentDiscar = new Intent(Intent.ACTION_DIAL);
        intentDiscar.setData(Uri.parse("tel:" + numeroTelefone));
        startActivity(intentDiscar);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}