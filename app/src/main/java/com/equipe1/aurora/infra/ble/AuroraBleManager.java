package com.equipe1.aurora.infra.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// =====================================================================
// AURORA GPS - GERENCIADOR BLE (AuroraBleManager.java)
// =====================================================================
// Responsável por realizar a varredura de dispositivos próximos sem
// conexão forçada automática, permitindo a seleção manual do usuário.
// =====================================================================

public class AuroraBleManager {
    private static final String TAG = "AuroraBleManager";

    // BLOCO 1: UUIDs CUSTOMIZADOS DO ESP32
    public static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // BLOCO 2: ATRIBUTOS DE SISTEMA
    private final Context context;
    private final BleCallback callback;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    private final Handler mainHandler;

    private boolean escaneando = false;
    private final Set<String> dispositivosEncontradosMac = new HashSet<>();

    // BLOCO 3: INTERFACE DE CALLBACK
    public interface BleCallback {
        void onStatusConexaoAlterado(boolean conectado);
        void onDadosGpsRecebidos(double lat, double lng, int bateria);
        void onSosFisicoAcionadoComDetalhes(double lat, double lng, int bateria, double velocidade, double altitude, String horario, String macAddress);
        void onNomeDispositivoLido(String nome, String macAddress);
        void onDispositivoEncontrado(BluetoothDevice device);
    }

    public AuroraBleManager(Context context, BleCallback callback) {
        this.context = context;
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;
    }

    // BLOCO 4: VARREDURA APENAS DE BUSCA (SEM AUTOCONECTAR)
    @SuppressLint("MissingPermission")
    public void iniciarEscaneamentoParaLista() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth desligado ou indisponível.");
            notificarStatus(false);
            return;
        }

        if (escaneando) pararEscaneamento();

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) return;

        dispositivosEncontradosMac.clear();
        escaneando = true;

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bleScanner.startScan(null, settings, scanCallback);

        // Timeout da varredura após 12 segundos
        mainHandler.postDelayed(() -> {
            if (escaneando) {
                pararEscaneamento();
                Log.d(TAG, "Varredura encerrada por tempo limite.");
            }
        }, 12000);
    }

    @SuppressLint("MissingPermission")
    public void pararEscaneamento() {
        if (escaneando && bleScanner != null && bluetoothAdapter.isEnabled()) {
            bleScanner.stopScan(scanCallback);
            escaneando = false;
        }
    }

    // BLOCO 5: CONEXÃO EXPLICITA APÓS A SELEÇÃO NO DIÁLOGO
    @SuppressLint("MissingPermission")
    public void conectarDispositivoEspecifico(BluetoothDevice device) {
        pararEscaneamento();
        if (device != null) {
            Log.d(TAG, "Iniciando conexao GATT com o dispositivo selecionado: " + device.getAddress());
            conectarAoGatt(device);
        }
    }

    // BLOCO 6: CALLBACK DO SCANNER (APENAS NOTIFICA A UI DA DESCOBERTA)
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device == null) return;

            String deviceAddress = device.getAddress();

            // Adiciona na lista apenas novos endereços MAC encontrados
            if (dispositivosEncontradosMac.add(deviceAddress)) {
                mainHandler.post(() -> callback.onDispositivoEncontrado(device));
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void conectarAoGatt(BluetoothDevice device) {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    // BLOCO 7: CALLBACKS DA CONEXÃO GATT
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT Conectado. Descobrindo servicos...");
                if (gatt.getDevice() != null) {
                    String mac = gatt.getDevice().getAddress();
                    String nome = gatt.getDevice().getName();
                    mainHandler.post(() -> callback.onNomeDispositivoLido(nome, mac));
                }
                notificarStatus(true);
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "GATT Desconectado.");
                gatt.close();
                if (bluetoothGatt == gatt) bluetoothGatt = null;
                notificarStatus(false);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            if (value != null && value.length > 0) {
                String mac = (gatt.getDevice() != null) ? gatt.getDevice().getAddress() : "";
                processarPayload(new String(value).trim(), mac);
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] rawData = characteristic.getValue();
            if (rawData != null && rawData.length > 0) {
                String mac = (gatt.getDevice() != null) ? gatt.getDevice().getAddress() : "";
                processarPayload(new String(rawData).trim(), mac);
            }
        }
    };

    // BLOCO 8: PROCESSAMENTO E PARSER DE MENSAGENS
    private void processarPayload(String payload, String macAddress) {
        try {
            if (payload.contains("ALERTA: SOS") || payload.contains("ALERT:SOS")) {
                double lat = 0.0, lng = 0.0, vel = 0.0, alt = 0.0;
                int bateria = 100;
                String horario = "--:--:--";

                String[] linhas = payload.split("\n");
                for (String linha : linhas) {
                    if (linha.contains("Horario:")) horario = linha.substring(linha.indexOf("Horario:") + 8).trim();
                    if (linha.contains("Bateria:")) {
                        String b = linha.substring(linha.indexOf("Bateria:") + 8).replace("%", "").trim();
                        try { bateria = Integer.parseInt(b); } catch (Exception ignored) {}
                    }
                    if (linha.contains("Lat:") && linha.contains("Lon:")) {
                        String[] partes = linha.split("\\|");
                        for (String p : partes) {
                            if (p.trim().startsWith("Lat:")) lat = Double.parseDouble(p.replace("Lat:", "").trim());
                            if (p.trim().startsWith("Lon:")) lng = Double.parseDouble(p.replace("Lon:", "").trim());
                        }
                    }
                    if (linha.contains("Velocidade:")) {
                        String v = linha.substring(linha.indexOf("Velocidade:") + 11).replace("km/h", "").trim();
                        try { vel = Double.parseDouble(v); } catch (Exception ignored) {}
                    }
                    if (linha.contains("Altitude:")) {
                        String a = linha.substring(linha.indexOf("Altitude:") + 9).replace("m", "").trim();
                        try { alt = Double.parseDouble(a); } catch (Exception ignored) {}
                    }
                }

                final double fLat = lat, fLng = lng, fVel = vel, fAlt = alt;
                final int fBat = bateria;
                final String fHora = horario;

                mainHandler.post(() -> callback.onSosFisicoAcionadoComDetalhes(fLat, fLng, fBat, fVel, fAlt, fHora, macAddress));
            } else if (payload.contains("Lat:") && payload.contains("Lon:")) {
                String[] partes = payload.split("\\|");
                double lat = 0.0, lng = 0.0;
                int bat = 100;
                for (String p : partes) {
                    if (p.trim().startsWith("Lat:")) lat = Double.parseDouble(p.replace("Lat:", "").trim());
                    if (p.trim().startsWith("Lon:")) lng = Double.parseDouble(p.replace("Lon:", "").trim());
                    if (p.trim().startsWith("Bat:")) bat = Integer.parseInt(p.replace("Bat:", "").replace("%", "").trim());
                }
                final double fLat = lat, fLng = lng;
                final int fBat = bat;
                mainHandler.post(() -> callback.onDadosGpsRecebidos(fLat, fLng, fBat));
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar payload BLE: " + payload, e);
        }
    }

    private void notificarStatus(boolean conectado) {
        mainHandler.post(() -> callback.onStatusConexaoAlterado(conectado));
    }

    @SuppressLint("MissingPermission")
    public void desconectar() {
        pararEscaneamento();
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        } else {
            notificarStatus(false);
        }
    }
}