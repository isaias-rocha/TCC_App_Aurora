package com.equipe1.aurora.ui.dispositivo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.FragmentDispositivoBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * =====================================================================
 * AURORA GPS - FRAGMENT DA TELA DE DISPOSITIVOS
 * =====================================================================
 * Camada de interface gráfica reativa responsável por apresentar o estado
 * do hardware ESP32, dados de localização e diálogos de conexão Bluetooth.
 * =====================================================================
 */
public class DispositivoFragment extends Fragment {

    // Códigos identificadores das requisições de permissão do sistema
    private static final int REQUEST_PERMISSIONS_CODE_CONECTAR = 101;
    private static final int REQUEST_PERMISSIONS_CODE_PAREAR = 102;

    private FragmentDispositivoBinding binding;
    private DispositivoViewModel viewModel;

    // -----------------------------------------------------------------
    // BLOCO 1: CICLO DE VIDA - INFLAÇÃO DO LAYOUT XML
    // -----------------------------------------------------------------
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDispositivoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    // -----------------------------------------------------------------
    // BLOCO 2: CICLO DE VIDA - INICIALIZAÇÃO DE COMPONENTES E VIEWMODEL
    // -----------------------------------------------------------------
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtém o ViewModel partilhado no âmbito da Activity principal
        viewModel = new ViewModelProvider(requireActivity()).get(DispositivoViewModel.class);

        configurarObservadores();
        configurarListeners();

        // Solicita permissões e inicia ligações ao carregar o ecrã
        verificarPermissoesEExecutar(REQUEST_PERMISSIONS_CODE_CONECTAR);
        viewModel.carregarDadosDoBancoServidor();
    }

    // -----------------------------------------------------------------
    // BLOCO 3: MAPEAMENTO REATIVO DA INTERFACE (OBSERVADORES LIVEDATA)
    // -----------------------------------------------------------------
    private void configurarObservadores() {

        // 1. Monitorização do Estado da Ligação BLE
        viewModel.getEstaConectado().observe(getViewLifecycleOwner(), conectado -> {
            if (binding == null) return;
            if (Boolean.TRUE.equals(conectado)) {
                binding.txtStatusConexao.setText("Conectado via BLE");
                binding.txtStatusConexao.setTextColor(ContextCompat.getColor(requireContext(), R.color.cor_segundaria));
            } else {
                binding.txtStatusConexao.setText("Desconectado");
                binding.txtStatusConexao.setTextColor(ContextCompat.getColor(requireContext(), R.color.vermelho_alerta));
            }
        });

        // 2. Atualização do Nome do Dispositivo
        viewModel.getNomeDispositivo().observe(getViewLifecycleOwner(), nome -> {
            if (binding == null) return;
            binding.txtNomeDispositivo.setText((nome != null && !nome.isEmpty()) ? nome : "Localizador Aurora");
        });

        // 3. Atualização do Nível Percentual da Bateria
        viewModel.getNivelBateria().observe(getViewLifecycleOwner(), bateria -> {
            if (binding == null) return;
            binding.txtNivelBateria.setText(bateria + "%");
        });

        // 4. Estado da Fixação do Sinal GPS
        viewModel.getStatusGps().observe(getViewLifecycleOwner(), status -> {
            if (binding == null) return;
            binding.txtStatusGps.setText(status);
        });

        // 5. Atualização das Coordenadas Geográficas na Interface
        viewModel.getUltimasCoordenadas().observe(getViewLifecycleOwner(), coords -> {
            if (binding == null) return;
            if (coords != null && coords.length == 2) {
                binding.txtLatitude.setText(String.format(Locale.getDefault(), "%.6f", coords[0]));
                binding.txtLongitude.setText(String.format(Locale.getDefault(), "%.6f", coords[1]));
            } else {
                binding.txtLatitude.setText("--.------");
                binding.txtLongitude.setText("--.------");
            }
        });

        // 6. Alerta de Disparo do Botão de Emergência SOS
        viewModel.getEventoSosAcionado().observe(getViewLifecycleOwner(), coords -> {
            if (coords != null && getContext() != null) {
                Toast.makeText(getContext(), "🚨 SOS FÍSICO ATIVADO! Lat: " + coords[0] + ", Lng: " + coords[1], Toast.LENGTH_LONG).show();
                viewModel.resetarEventoSos();
            }
        });

        // 7. Notificações de Erros do Servidor Local
        viewModel.getMensagemErroApiLiveData().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && getContext() != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -----------------------------------------------------------------
    // BLOCO 4: CONFIGURAÇÃO DE EVENTOS DE CLIQUE DOS BOTÕES
    // -----------------------------------------------------------------
    private void configurarListeners() {

        // Botão de navegação para voltar ao ecrã anterior
        if (binding.btnVoltar != null) {
            binding.btnVoltar.setOnClickListener(v -> {
                try {
                    NavHostFragment.findNavController(this).navigateUp();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Redirecionamento para o mapa passando os dados atuais de localização
        binding.btnVerLocalizacao.setOnClickListener(v -> {
            double[] coords = viewModel.getUltimasCoordenadas().getValue();
            if (coords != null && coords.length == 2) {
                Bundle bundle = new Bundle();
                bundle.putDouble("latitude", coords[0]);
                bundle.putDouble("longitude", coords[1]);
                bundle.putBoolean("veio_do_dispositivo", true);

                try {
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_dispositivoFragment_to_mapaFragment, bundle);
                } catch (Exception e) {
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.nav_mapa, bundle);
                }
            } else {
                Toast.makeText(requireContext(), "Aguardando sinal GPS do dispositivo...", Toast.LENGTH_SHORT).show();
            }
        });

        // Botão de Sincronização Dupla (Reinicia varredura BLE e consulta o MySQL)
        binding.btnSincronizar.setOnClickListener(v -> {
            verificarPermissoesEExecutar(REQUEST_PERMISSIONS_CODE_CONECTAR);
            viewModel.carregarDadosDoBancoServidor();
            Toast.makeText(requireContext(), "Sincronizando dados com o servidor local...", Toast.LENGTH_SHORT).show();
        });

        // Abertura do diálogo para pareamento com um novo dispositivo próximo
        binding.btnParearDispositivo.setOnClickListener(v -> verificarPermissoesEExecutar(REQUEST_PERMISSIONS_CODE_PAREAR));
    }

    // -----------------------------------------------------------------
    // BLOCO 5: VERIFICAÇÃO DE PERMISSÕES DE RUNTIME (ANDROID API 31+, 33+)
    // -----------------------------------------------------------------
    private void verificarPermissoesEExecutar(int requestCode) {
        List<String> permissoes = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissoes.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissoes.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissoes.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissoes.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissoes.isEmpty()) {
            requestPermissions(permissoes.toArray(new String[0]), requestCode);
        } else {
            executarAcaoAposPermissoes(requestCode);
        }
    }

    private void executarAcaoAposPermissoes(int requestCode) {
        if (requestCode == REQUEST_PERMISSIONS_CODE_PAREAR) {
            abrirDialogoSelecaoDispositivo();
        } else {
            viewModel.iniciarConexaoBle();
        }
    }

    // -----------------------------------------------------------------
    // BLOCO 6: DIÁLOGO MATERIAL ALERT DE SELEÇÃO DE DISPOSITIVO BLE
    // -----------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void abrirDialogoSelecaoDispositivo() {
        viewModel.limparListaDispositivos();
        viewModel.iniciarConexaoBle();

        List<BluetoothDevice> listaDevices = new ArrayList<>();
        List<String> listaNomesExibicao = new ArrayList<>();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                listaNomesExibicao
        );

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Dispositivos Aurora Próximos");

        builder.setAdapter(adapter, (dialog, which) -> {
            if (which < listaDevices.size()) {
                BluetoothDevice selecionado = listaDevices.get(which);
                Toast.makeText(requireContext(), "Conectando a " + adapter.getItem(which), Toast.LENGTH_SHORT).show();
                viewModel.conectarDispositivoSelecionado(selecionado);
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();

        viewModel.getDispositivosEncontrados().observe(getViewLifecycleOwner(), novosDispositivos -> {
            listaDevices.clear();
            listaNomesExibicao.clear();

            for (BluetoothDevice dev : novosDispositivos) {
                String nome = dev.getName();
                if (nome == null || nome.isEmpty()) {
                    nome = "Dispositivo Desconhecido";
                }

                listaDevices.add(dev);

                if (nome.contains("Aurora")) {
                    listaNomesExibicao.add("🚨 " + nome + "\n MAC: " + dev.getAddress());
                } else {
                    listaNomesExibicao.add("📱 " + nome + "\n MAC: " + dev.getAddress());
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    // -----------------------------------------------------------------
    // BLOCO 7: RETORNO DAS PERMISSÕES SOLICITADAS
    // -----------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean todasConcedidas = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                todasConcedidas = false;
                break;
            }
        }

        if (todasConcedidas) {
            executarAcaoAposPermissoes(requestCode);
        } else {
            Toast.makeText(requireContext(), "Permissões de Bluetooth, Notificação e GPS são necessárias.", Toast.LENGTH_LONG).show();
        }
    }

    // -----------------------------------------------------------------
    // BLOCO 8: PREVENÇÃO DE LEAK DE MEMÓRIA NO BINDING DO FRAGMENT
    // -----------------------------------------------------------------
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}