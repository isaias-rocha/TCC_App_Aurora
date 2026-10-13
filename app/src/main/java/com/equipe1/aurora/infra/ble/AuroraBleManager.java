package com.equipe1.aurora.infra.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.UUID;

public class AuroraBleManager {

    private static final String TAG = "AuroraBleManager";

    // UUIDs idênticos aos definidos no código do ESP32
    public static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    private BleCallback callback;

    public interface BleCallback {
        void onStatusConexaoAlterado(boolean conectado);
        void onDadosGpsRecebidos(double lat, double lng, int bateria);
        void onSosFisicoAcionado(double lat, double lng);
    }

    public AuroraBleManager(Context context, BleCallback callback) {
        this.context = context;
        this.callback = callback;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @SuppressLint("MissingPermission")
    public void iniciarEscaneamentoEPareamento() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth desligado ou indisponível.");
            return;
        }

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        Log.d(TAG, "Procurando pelo ESP32 ('Aurora GPS')...");

        bleScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                // Filtra pelo nome definido no ESP32: "Aurora GPS"
                if (device.getName() != null && device.getName().contains("Aurora GPS")) {
                    Log.d(TAG, "ESP32 Encontrado! Endereço MAC: " + device.getAddress());
                    bleScanner.stopScan(this);
                    conectarAoGatt(device);
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void conectarAoGatt(BluetoothDevice device) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        @SuppressLint("MissingPermission")
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Conectado ao ESP32. Descobrindo serviços...");
                new Handler(Looper.getMainLooper()).post(() -> callback.onStatusConexaoAlterado(true));
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Desconectado do ESP32.");
                new Handler(Looper.getMainLooper()).post(() -> callback.onStatusConexaoAlterado(false));
            }
        }

        @Override
        @SuppressLint("MissingPermission")
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                    ativarNotificacao(characteristic);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            if (value == null) return;
            String payload = new String(value);

            Log.d(TAG, "Payload recebido do ESP32: " + payload);

            new Handler(Looper.getMainLooper()).post(() -> processarPayloadEsp32(payload));
        }
    };

    @SuppressLint("MissingPermission")
    private void ativarNotificacao(BluetoothGattCharacteristic characteristic) {
        if (characteristic != null && bluetoothGatt != null) {
            bluetoothGatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);
            }
        }
    }

    /**
     * Trata as mensagens vindas do ESP32:
     * - SOS: "ALERT:SOS|Lat:-23.550520|Lon:-46.633308"
     * - GPS: "Lat:-23.550520|Lon:-46.633308|Bat:85%"
     */
    private void processarPayloadEsp32(String payload) {
        try {
            if (payload.startsWith("ALERT:SOS")) {
                // Alerta de SOS
                String[] partes = payload.split("\\|");
                double lat = Double.parseDouble(partes[1].split(":")[1]);
                double lng = Double.parseDouble(partes[2].split(":")[1]);

                callback.onSosFisicoAcionado(lat, lng);
            }
            else if (payload.startsWith("Lat:")) {
                // Atualização periódica de localização
                String[] partes = payload.split("\\|");
                double lat = Double.parseDouble(partes[0].split(":")[1]);
                double lng = Double.parseDouble(partes[1].split(":")[1]);

                // Extrai a bateria removendo o caractere '%'
                String strBateria = partes[2].split(":")[1].replace("%", "").trim();
                int bateria = Integer.parseInt(strBateria);

                callback.onDadosGpsRecebidos(lat, lng, bateria);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao interpretar payload do ESP32: " + payload, e);
        }
    }

    @SuppressLint("MissingPermission")
    public void desconectar() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}