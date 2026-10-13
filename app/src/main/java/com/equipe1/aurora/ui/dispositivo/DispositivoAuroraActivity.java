package com.equipe1.aurora.ui.dispositivo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.equipe1.aurora.databinding.ActivityDispositivoAuroraBinding;
import com.equipe1.aurora.infra.ble.AuroraBleManager;
import com.equipe1.aurora.ui.main.MainActivity;

public class DispositivoAuroraActivity extends AppCompatActivity {

    private ActivityDispositivoAuroraBinding binding;
    private AuroraBleManager bleManager;

    // Coordenadas obtidas do GPS NEO-6M do ESP32
    private double latitudeDispositivo = -23.550520;
    private double longitudeDispositivo = -46.633308;
    private int nivelBateria = 85;
    private String statusConexao = "Desconectado";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityDispositivoAuroraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarBleManager();
        atualizarDadosDispositivo();
        configurarListeners();
    }

    private void inicializarBleManager() {
        // Instância do gerenciador importado da pasta infra.ble
        bleManager = new AuroraBleManager(this, new AuroraBleManager.BleCallback() {
            @Override
            public void onStatusConexaoAlterado(boolean conectado) {
                statusConexao = conectado ? "Conectado via BLE" : "Desconectado";
                binding.txtStatusConexao.setText(statusConexao);
            }

            @Override
            public void onDadosGpsRecebidos(double lat, double lng, int bateria) {
                latitudeDispositivo = lat;
                longitudeDispositivo = lng;
                nivelBateria = bateria;

                binding.txtNivelBateria.setText(bateria + "%");
                binding.txtStatusGps.setText("Ativo (GPS)");
            }

            @Override
            public void onSosFisicoAcionado(double lat, double lng) {
                latitudeDispositivo = lat;
                longitudeDispositivo = lng;

                Toast.makeText(DispositivoAuroraActivity.this,
                        "🚨 ALERTA SOS RECEBIDO DO ESP32!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void configurarListeners() {
        // Redireciona para o mapa na MainActivity passando as coordenadas do ESP32
        binding.btnVerLocalizacao.setOnClickListener(v -> {
            Intent intent = new Intent(DispositivoAuroraActivity.this, MainActivity.class);
            intent.putExtra("ABRIR_MAPA", true);
            intent.putExtra("LATITUDE_ESP32", latitudeDispositivo);
            intent.putExtra("LONGITUDE_ESP32", longitudeDispositivo);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Sincroniza/atualiza as informações exibidas na tela
        binding.btnSincronizar.setOnClickListener(v -> {
            atualizarDadosDispositivo();
            Toast.makeText(this, "Sincronizando coordenadas...", Toast.LENGTH_SHORT).show();
        });

        // Inicia a busca BLE pelo ESP32 ("Aurora GPS")
        binding.btnParearDispositivo.setOnClickListener(v -> {
            Toast.makeText(this, "Buscando dispositivos BLE próximos...", Toast.LENGTH_SHORT).show();
            if (bleManager != null) {
                bleManager.iniciarEscaneamentoEPareamento();
            }
        });
    }

    private void atualizarDadosDispositivo() {
        binding.txtStatusConexao.setText(statusConexao);
        binding.txtNivelBateria.setText(nivelBateria + "%");
        binding.txtStatusGps.setText("Ativo (Até 5s)");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Desconecta o GATT para evitar memory leaks ao fechar a Activity
        if (bleManager != null) {
            bleManager.desconectar();
        }
        binding = null;
    }
}