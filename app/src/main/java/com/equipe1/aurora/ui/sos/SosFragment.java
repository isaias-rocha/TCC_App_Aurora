package com.equipe1.aurora.ui.sos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.equipe1.aurora.databinding.FragmentSosBinding;
import com.equipe1.aurora.domain.model.AlertaSos;
import com.equipe1.aurora.rede.api.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SosFragment extends Fragment {

    private FragmentSosBinding binding;
    private Long usuarioIdLogado = -1L;
    private Long alertaAtivoId = -1L;
    private SharedPreferences prefs;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fine != null && fine) || (coarse != null && coarse)) {
                    dispararAlertaSos();
                } else {
                    Toast.makeText(getContext(), "Permissão de GPS necessária para enviar sua localização no SOS.", Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSosBinding.inflate(inflater, container, false);

        prefs = requireActivity().getSharedPreferences("AuroraPrefs", Context.MODE_PRIVATE);
        usuarioIdLogado = prefs.getLong("usuarioId_logado", prefs.getLong("usuariosId_logado", -1L));
        alertaAtivoId = prefs.getLong("alertaSosAtivoId", -1L);

        atualizarInterfaceEstadoSos();
        configurarCliques();

        return binding.getRoot();
    }

    private void configurarCliques() {
        // Voltar para a tela anterior
        binding.btnVoltar.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        // Botão principal: Acionar ou Encerrar SOS
        binding.btnSos.setOnClickListener(v -> {
            if (usuarioIdLogado == -1L) {
                Toast.makeText(getContext(), "Erro: Usuário não identificado. Faça login novamente.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (alertaAtivoId != -1L) {
                confirmarEncerramentoSos();
            } else {
                verificarPermissaoEDisparar();
            }
        });

        // Botão de ligação direta de emergência (190)
        binding.btnLigarEmergencia.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:190"));
            startActivity(intent);
        });
    }

    private void verificarPermissaoEDisparar() {
        boolean temFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean temCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (temFine || temCoarse) {
            dispararAlertaSos();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void dispararAlertaSos() {
        double lat = -23.550520; // Fallback (Centro SP) caso o emulador ainda não tenha fixado GPS
        double lon = -46.633308;

        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            Location locGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location melhorLoc = locGps != null ? locGps : locNet;

            if (melhorLoc != null) {
                lat = melhorLoc.getLatitude();
                lon = melhorLoc.getLongitude();
            }
        }

        binding.btnSos.setEnabled(false);
        binding.btnSos.setText("ENVIANDO...");

        ApiClient.getApiService().acionarSos(usuarioIdLogado, lat, lon, "APP").enqueue(new Callback<AlertaSos>() {
            @Override
            public void onResponse(Call<AlertaSos> call, Response<AlertaSos> response) {
                if (!isAdded() || binding == null) return;
                binding.btnSos.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    alertaAtivoId = response.body().getIdAlerta();
                    prefs.edit().putLong("alertaSosAtivoId", alertaAtivoId).apply();

                    atualizarInterfaceEstadoSos();
                    Toast.makeText(getContext(), "🚨 Alerta SOS enviado aos seus círculos!", Toast.LENGTH_LONG).show();
                } else {
                    atualizarInterfaceEstadoSos();
                    Toast.makeText(getContext(), "Erro ao acionar SOS (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AlertaSos> call, Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.btnSos.setEnabled(true);
                atualizarInterfaceEstadoSos();
                Toast.makeText(getContext(), "Falha de conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmarEncerramentoSos() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Encerrar Alerta SOS")
                .setMessage("Você está em segurança e deseja encerrar o pedido de ajuda?")
                .setPositiveButton("Sim, estou seguro", (dialog, which) -> encerrarAlertaSos())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void encerrarAlertaSos() {
        binding.btnSos.setEnabled(false);

        ApiClient.getApiService().encerrarSos(alertaAtivoId).enqueue(new Callback<AlertaSos>() {
            @Override
            public void onResponse(Call<AlertaSos> call, Response<AlertaSos> response) {
                if (!isAdded() || binding == null) return;
                binding.btnSos.setEnabled(true);

                if (response.isSuccessful()) {
                    alertaAtivoId = -1L;
                    prefs.edit().remove("alertaSosAtivoId").apply();

                    atualizarInterfaceEstadoSos();
                    Toast.makeText(getContext(), "Alerta SOS encerrado com sucesso.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Erro ao encerrar SOS (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AlertaSos> call, Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.btnSos.setEnabled(true);
                Toast.makeText(getContext(), "Falha de conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void atualizarInterfaceEstadoSos() {
        if (alertaAtivoId != -1L) {
            binding.tvTitulo.setText("SOS ATIVO");
            binding.tvSubtitulo.setText("Seus círculos estão sendo alertados");
            binding.tvDescricaoSos.setText("Sua localização em tempo real foi compartilhada com seus familiares e contatos de emergência.");
            binding.btnSos.setText("ENCERRAR SOS");
            binding.btnSos.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
        } else {
            binding.tvTitulo.setText("Emergência SOS");
            binding.tvSubtitulo.setText("Pedido de ajuda imediato");
            binding.tvDescricaoSos.setText("Ao acionar o SOS, sua localização exata será enviada imediatamente para todos os membros dos seus círculos.");
            binding.btnSos.setText("SOS");
            binding.btnSos.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}