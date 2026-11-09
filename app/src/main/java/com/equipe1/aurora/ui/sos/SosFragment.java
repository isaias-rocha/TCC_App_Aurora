package com.equipe1.aurora.ui.sos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.equipe1.aurora.databinding.DialogSeletorEmergenciaBinding;
import com.equipe1.aurora.databinding.FragmentSosBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

// =====================================================================
// AURORA GPS - FRAGMENT DA TELA DE EMERGÊNCIA SOS (SosFragment.java)
// =====================================================================
// Captura geolocalização em tempo real do Android 10+ via FusedLocation
// e dispara pânico por toque mantido (1 segundo) [RF026].
// =====================================================================

public class SosFragment extends Fragment {

    // // BLOCO 1: ATRIBUTOS E BINDINGS
    private FragmentSosBinding binding;
    private SosViewModel sosViewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private CountDownTimer countDownTimer;
    private boolean isHolding = false;
    private static final long TEMPO_PRESSIONADO_MS = 1000; // Retenção de 1 segundo

    // // BLOCO 2: CICLO DE VIDA DO FRAGMENT
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSosBinding.inflate(inflater, container, false);
        sosViewModel = new ViewModelProvider(this).get(SosViewModel.class);

        // Inicializa o cliente nativo de localização do Android
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        configurarBotaoVoltar();
        configurarBotaoSos();
        configurarSeletorEmergencia();
        observarViewModel();

        return binding.getRoot();
    }

    // // BLOCO 3: NAVEGAÇÃO E RETORNO
    private void configurarBotaoVoltar() {
        if (binding == null) return;
        binding.btnVoltar.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );
    }

    // // BLOCO 4: OBSERVAÇÃO REATIVA DO VIEWMODEL
    private void observarViewModel() {
        sosViewModel.alertaEnviadoComSucesso.observe(getViewLifecycleOwner(), sucesso -> {
            if (Boolean.TRUE.equals(sucesso)) {
                Toast.makeText(requireContext(), "🚨 SOS DO APP ATIVADO! \n" +
                        "Alerta com sua localização em tempo real gravado localmente.", Toast.LENGTH_LONG).show();
            }
        });

        sosViewModel.mensagemErro.observe(getViewLifecycleOwner(), erro -> {
            if (erro != null) {
                Toast.makeText(requireContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // // BLOCO 5: LÓGICA DO TOQUE CONTÍNUO DE 1 SEGUNDO
    private void configurarBotaoSos() {
        if (binding == null) return;

        binding.btnSos.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    iniciarContagemRegressiva();
                    return true;

                case MotionEvent.ACTION_UP:
                    v.performClick();
                case MotionEvent.ACTION_CANCEL:
                    cancelarContagemRegressiva();
                    return true;
            }
            return false;
        });
    }

    // // BLOCO 6: CONTAGEM REGRESSIVA E FEEDBACK VISUAL/TÁTIL
    private void iniciarContagemRegressiva() {
        isHolding = true;
        vibrar(100);

        countDownTimer = new CountDownTimer(TEMPO_PRESSIONADO_MS, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null) return;
                binding.btnSos.setText("...");
                binding.btnSos.announceForAccessibility("Mantenha pressionado por 1 segundo");
            }

            @Override
            public void onFinish() {
                if (isHolding && binding != null) {
                    dispararAlertaSosApp();
                }
            }
        }.start();
    }

    // // BLOCO 7: CANCELAMENTO DA CONTAGEM
    private void cancelarContagemRegressiva() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (isHolding) {
            isHolding = false;
            if (binding != null) {
                binding.btnSos.setText("SOS");
                binding.btnSos.setContentDescription("Acionar emergência SOS. Mantenha pressionado por 1 segundo.");
            }
        }
    }

    // // BLOCO 8: ACIONAMENTO DO SOS DO APLICATIVO COM GEOLOCALIZAÇÃO REAL
    private void dispararAlertaSosApp() {
        isHolding = false;
        vibrar(600); // Feedback tátil do disparo

        if (binding != null) {
            binding.btnSos.setText("SOS");
            binding.btnSos.announceForAccessibility("Emergência do App disparada com sucesso.");
        }

        // Captura a latitude e longitude reais do smartphone
        obterLocalizacaoEmTempoRealEEnviar();
    }

    // // BLOCO 8.1: LEITURA DE GPS EM TEMPO REAL (ANDROID 10+ / API 29+)
    @SuppressLint("MissingPermission")
    private void obterLocalizacaoEmTempoRealEEnviar() {
        // Valida se as permissões de localização foram concedidas
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(requireContext(), "Permissão de GPS necessária para obter a localização exata.", Toast.LENGTH_LONG).show();
            // Dispara com zero como salvaguarda caso não haja permissão
            sosViewModel.enviarAlertaSosApp(0.0, 0.0);
            return;
        }

        // Solicita leitura instantânea com alta precisão
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitudeAtual = location.getLatitude();
                        double longitudeAtual = location.getLongitude();
                        sosViewModel.enviarAlertaSosApp(latitudeAtual, longitudeAtual);
                    } else {
                        // Fallback: Tenta a última localização conhecida armazenada no cache do Android
                        obterUltimaLocalizacaoConhecida();
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback em caso de falha na busca rápida de satélites
                    obterUltimaLocalizacaoConhecida();
                });
    }

    // // BLOCO 8.2: FALLBACK PARA ÚLTIMA POSIÇÃO CONHECIDA
    @SuppressLint("MissingPermission")
    private void obterUltimaLocalizacaoConhecida() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                sosViewModel.enviarAlertaSosApp(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(requireContext(), "Não foi possível obter a posição atual do GPS.", Toast.LENGTH_SHORT).show();
                sosViewModel.enviarAlertaSosApp(0.0, 0.0);
            }
        });
    }

    // // BLOCO 9: VIBRAÇÃO DO SMARTPHONE
    private void vibrar(long milissegundos) {
        if (getContext() == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("AuroraAccessibilityPrefs", Context.MODE_PRIVATE);
        boolean feedbackTatilAtivo = prefs.getBoolean("key_feedback_tatil", true);
        if (!feedbackTatilAtivo) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager != null) {
                vibratorManager.getDefaultVibrator().vibrate(VibrationEffect.createOneShot(milissegundos, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(milissegundos, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    // // BLOCO 10: SELETOR DE LIGAÇÕES DE EMERGÊNCIA
    private void configurarSeletorEmergencia() {
        if (binding == null) return;

        binding.btnLigarEmergencia.setOnClickListener(v -> {
            DialogSeletorEmergenciaBinding dialogBinding = DialogSeletorEmergenciaBinding.inflate(getLayoutInflater());
            AlertDialog.Builder construtorAlerta = new AlertDialog.Builder(requireContext());
            construtorAlerta.setView(dialogBinding.getRoot());

            AlertDialog dialogo = construtorAlerta.create();
            if (dialogo.getWindow() != null) {
                dialogo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

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

    // // BLOCO 11: LIMPEZA DE RECURSOS
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelarContagemRegressiva();
        binding = null;
    }
}