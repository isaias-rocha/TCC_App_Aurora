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
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * =====================================================================
 * GERENCIADOR DE COMUNICAÇÃO BLUETOOTH LOW ENERGY (BLE) - ANDROID
 * =====================================================================
 * Esta classe é responsável por todo o ciclo de vida da conexão BLE com o ESP32:
 * 1. Varredura de rádio (Scan) e identificação de periféricos no ambiente.
 * 2. Estabelecimento de canal GATT de baixa energia.
 * 3. Inscrição para recebimento de Notificações via Descritor CCCD (0x2902).
 * 4. Tratamento de concorrência com o Handler para a Main Thread (UI).
 * 5. Parse e decodificação dos pacotes de texto (Telemetria GPS e Alertas SOS).
 */
public class AuroraBleManager {
    private static final String TAG = "AuroraBleManager";

    // -----------------------------------------------------------------
    // IDENTIFICADORES ÚNICOS DA APLICAÇÃO (UUIDs)
    // -----------------------------------------------------------------
    // UUID do Serviço Customizado do ESP32
    public static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");

    // UUID da Característica por onde transitam os dados de telemetria e pânico
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");

    // UUID Oficial da Bluetooth SIG para o Client Characteristic Configuration Descriptor (CCCD)
    // Usado para habilitação explícita de Notificações no hardware remoto
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // -----------------------------------------------------------------
    // ATRIBUTOS DE GERENCIAMENTO DE SISTEMA E ESTADO
    // -----------------------------------------------------------------
    private final Context context;
    private final BleCallback callback;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    private final Handler mainHandler; // Garante que atualizações do callback rodem na UI Thread

    private boolean escaneando = false;

    // Conjunto para evitar notificar dispositivos duplicados durante o escaneamento ativo
    private final Set<String> dispositivosEncontradosMac = new HashSet<>();

    // -----------------------------------------------------------------
    // INTERFACE DE CALLBACK PARA A CAMADA DE APRESENTAÇÃO/SERVIÇO
    // -----------------------------------------------------------------
    public interface BleCallback {
        void onStatusConexaoAlterado(boolean conectado);
        void onDadosGpsRecebidos(double lat, double lng, int bateria);
        void onSosFisicoAcionado(double lat, double lng);
        void onNomeDispositivoLido(String nome);
        void onDispositivoEncontrado(BluetoothDevice device);
    }

    /**
     * CONSTRUTOR
     * Inicializa os gerenciadores de rádio Bluetooth nativos do Android.
     */
    public AuroraBleManager(Context context, BleCallback callback) {
        this.context = context;
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Obtém a instância do adaptador Bluetooth através do serviço do sistema
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;
    }

    /**
     * INICIALIZAÇÃO DE ESCANEAMENTO E PAREAMENTO
     * Realiza verificações de segurança do hardware, verifica conexões ativas no SO
     * e inicia a busca por novos periféricos via Bluetooth LE.
     */
    @SuppressLint("MissingPermission")
    public void iniciarEscaneamentoEPareamento() {
        // Bloco 1: Validação de presença e estado do hardware de rádio
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth desligado ou indisponível.");
            notificarStatus(false);
            return;
        }

        // Bloco 2: Reconexão Rápida Fallback
        // Verifica se o SO já possui uma conexão GATT ativa com o dispositivo "Aurora GPS"
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            List<BluetoothDevice> dispositivosConectados = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            for (BluetoothDevice device : dispositivosConectados) {
                String nome = device.getName();
                if (nome != null && nome.contains("Aurora GPS")) {
                    Log.d(TAG, "ESP32 já pareado no sistema. Conectando diretamente ao canal GATT...");
                    conectarAoGatt(device);
                    return;
                }
            }
        }

        // Evita chamadas de escaneamento concorrentes
        if (escaneando) return;

        // Bloco 3: Inicialização do Scanner LE
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) return;

        dispositivosEncontradosMac.clear();
        escaneando = true;

        // Configuração para priorizar baixa latência na busca do sinal do ESP32
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bleScanner.startScan(null, settings, scanCallback);

        // Bloco 4: Temporizador de segurança (Timeout)
        // Encerra a busca após 15 segundos para preservação da bateria do dispositivo móvel
        mainHandler.postDelayed(() -> {
            if (escaneando) {
                pararEscaneamento();
                Log.w(TAG, "Timeout de escaneamento atingido (15s). Varredura interrompida.");
            }
        }, 15000);
    }

    /**
     * INTERRUPÇÃO DO ESCANEAMENTO
     * Para a busca de rádio e redefinição da flag de controle.
     */
    @SuppressLint("MissingPermission")
    public void pararEscaneamento() {
        if (escaneando && bleScanner != null && bluetoothAdapter.isEnabled()) {
            bleScanner.stopScan(scanCallback);
            escaneando = false;
        }
    }

    /**
     * CONEXÃO MANUALLY SELECIONADA NA INTERFACE
     * Conecta a um dispositivo específico escolhido no diálogo de seleção da UI.
     */
    @SuppressLint("MissingPermission")
    public void conectarDispositivoEspecifico(BluetoothDevice device) {
        pararEscaneamento();
        if (device != null) {
            Log.d(TAG, "Conectando ao dispositivo selecionado: " + device.getName() + " [" + device.getAddress() + "]");
            conectarAoGatt(device);
        }
    }

    /**
     * CALLBACKS DE RESULTADO DE ESCANEAMENTO
     * Processa os pacotes de anúncio (Advertising) recebidos pelo rádio do dispositivo.
     */
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device == null) return;

            @SuppressLint("MissingPermission")
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();

            // Filtra duplicatas com base no endereço MAC para não sobrecarregar a lista na UI
            if (dispositivosEncontradosMac.add(deviceAddress)) {
                mainHandler.post(() -> callback.onDispositivoEncontrado(device));
            }

            // Conexão automática caso o dispositivo oficial "Aurora GPS" seja identificado no ar
            if (deviceName != null && deviceName.contains("Aurora GPS")) {
                Log.d(TAG, "ESP32 'Aurora GPS' localizado automaticamente. Conectando...");
                pararEscaneamento();
                conectarAoGatt(device);
            }
        }
    };

    /**
     * ABERTURA DO CANAL GATT
     * Inicializa a conexão cliente com o hardware remoto especificando o transporte exclusivo BLE.
     */
    @SuppressLint("MissingPermission")
    private void conectarAoGatt(BluetoothDevice device) {
        // Encerra qualquer sessão GATT anterior para evitar vazamento de memória e orfãos no SO
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        // TRANSPORT_LE: Impede o Android de tentar realizar fallback para Bluetooth Classic
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    /**
     * CALLBACKS DE GERENCIAMENTO DA CONEXÃO GATT
     * Monitora alterações de estado, descoberta de serviços e mudanças em características.
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        // Bloco 1: Alteração no Estado da Conexão
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT Conectado com sucesso. Solicitando descoberta de serviços...");

                // Notifica a UI com o nome do dispositivo detectado
                if (gatt.getDevice() != null) {
                    String nomeBle = gatt.getDevice().getName();
                    if (nomeBle != null && !nomeBle.isEmpty()) {
                        mainHandler.post(() -> callback.onNomeDispositivoLido(nomeBle));
                    }
                }

                notificarStatus(true);
                gatt.discoverServices(); // Solicita a árvore de serviços e características do ESP32

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "GATT Desconectado do hardware.");

                // Liberação assíncrona segura de recursos somente após a confirmação da desconexão
                gatt.close();
                if (bluetoothGatt == gatt) {
                    bluetoothGatt = null;
                }

                notificarStatus(false);
            }
        }

        // Bloco 2: Descoberta de Serviços Concluída
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                    if (characteristic != null) {
                        // Ativa o recebimento de notificações na pilha local
                        gatt.setCharacteristicNotification(characteristic, true);

                        // Escreve no descritor CCCD (0x2902) do ESP32 para autorizar a transmissão de dados
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                }
            }
        }

        // Bloco 3A: Recebimento de Notificações - Compatibilidade Android 13+ (API 33+)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            if (value != null && value.length > 0) {
                String payload = new String(value).trim();
                Log.d(TAG, "Payload recebido via BLE (API 33+): " + payload);
                processarPayload(payload);
            }
        }

        // Bloco 3B: Recebimento de Notificações - Suporte Legado (API < 33)
        @SuppressWarnings("deprecation")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] rawData = characteristic.getValue();
            if (rawData != null && rawData.length > 0) {
                String payload = new String(rawData).trim();
                Log.d(TAG, "Payload recebido via BLE (Legado): " + payload);
                processarPayload(payload);
            }
        }
    };

    /**
     * PARSER DE STRING E DECODIFICAÇÃO DE TELEMETRIA
     * Processa o texto delimitado por barras verticais gerado pelo ESP32.
     */
    private void processarPayload(String payload) {
        try {
            // Bloco 1: Processamento de Alerta SOS
            if (payload.startsWith("ALERT:SOS")) {
                String[] partes = payload.split("\\|");
                double lat = 0.0, lng = 0.0;
                for (String p : partes) {
                    if (p.startsWith("Lat:")) lat = Double.parseDouble(p.replace("Lat:", ""));
                    if (p.startsWith("Lon:")) lng = Double.parseDouble(p.replace("Lon:", ""));
                }
                final double fLat = lat, fLng = lng;
                mainHandler.post(() -> callback.onSosFisicoAcionado(fLat, fLng));
            }
            // Bloco 2: Processamento de Telemetria Contínua (GPS e Bateria)
            else if (payload.contains("Lat:") && payload.contains("Lon:")) {
                String[] partes = payload.split("\\|");
                double lat = 0.0, lng = 0.0;
                int bat = 100;
                for (String p : partes) {
                    if (p.startsWith("Lat:")) lat = Double.parseDouble(p.replace("Lat:", ""));
                    if (p.startsWith("Lon:")) lng = Double.parseDouble(p.replace("Lon:", ""));
                    if (p.startsWith("Bat:")) bat = Integer.parseInt(p.replace("Bat:", "").replace("%", ""));
                }
                final double fLat = lat, fLng = lng;
                final int fBat = bat;
                mainHandler.post(() -> callback.onDadosGpsRecebidos(fLat, fLng, fBat));
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar dados recebidos: " + payload, e);
        }
    }

    /**
     * DISPATCHER DE STATUS DE CONEXÃO
     * Envia o estado atual da conexão para a Thread Principal da aplicação.
     */
    private void notificarStatus(boolean conectado) {
        mainHandler.post(() -> callback.onStatusConexaoAlterado(conectado));
    }

    /**
     * ENCERRAMENTO DE SESSÃO
     * Solicita o desligamento gracioso da conexão de rádio.
     */
    @SuppressLint("MissingPermission")
    public void desconectar() {
        pararEscaneamento();

        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect(); // Dispara o desligamento assíncrono (tratado no gattCallback)
        } else {
            notificarStatus(false);
        }
    }
}