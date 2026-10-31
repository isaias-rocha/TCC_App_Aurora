package com.equipe1.aurora.ui.dispositivo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.FragmentDispositivoBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

// =====================================================================
// AURORA GPS - CONTROLLER DA TELA DE DISPOSITIVOS (DispositivoFragment.java)
// =====================================================================
// Apresenta a lista principal de rastreadores e abre o diálogo com Cards
// para seleção e pareamento de novos dispositivos.
// =====================================================================

public class DispositivoFragment extends Fragment implements DispositivoAdapter.OnDispositivoActionListener {

    private static final int REQUEST_PERMISSIONS_CODE_CONECTAR = 101;
    private static final int REQUEST_PERMISSIONS_CODE_PAREAR = 102;

    private FragmentDispositivoBinding binding;
    private DispositivoViewModel viewModel;
    private DispositivoAdapter adapterMain;
    private final List<DispositivoItem> listaItens = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDispositivoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(DispositivoViewModel.class);

        configurarRecyclerViewPrincipal();
        configurarObservadores();
        configurarListeners();

        verificarPermissoesEExecutar(REQUEST_PERMISSIONS_CODE_CONECTAR);
    }

    // BLOCO 1: CONFIGURAÇÃO DO RECYCLERVIEW PRINCIPAL DE DISPOSITIVOS PAREADOS
    private void configurarRecyclerViewPrincipal() {
        adapterMain = new DispositivoAdapter(listaItens, this);
        binding.rvDispositivos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDispositivos.setAdapter(adapterMain);
    }

    // BLOCO 2: OBSERVADORES DE DADOS REATIVOS (LIVEDATA)
    private void configurarObservadores() {
        viewModel.getListaDispositivosPareados().observe(getViewLifecycleOwner(), itens -> {
            listaItens.clear();
            if (itens != null) listaItens.addAll(itens);
            adapterMain.notifyDataSetChanged();
        });

        viewModel.getEventoSosAcionado().observe(getViewLifecycleOwner(), coords -> {
            if (coords != null && getContext() != null) {
                Toast.makeText(getContext(), "🚨 SOS DE EMERGÊNCIA ATIVADO NO RASTREADOR!", Toast.LENGTH_LONG).show();
                viewModel.resetarEventoSos();
            }
        });
    }

    // BLOCO 3: LISTENERS DE BOTÕES E NAVEGAÇÃO
    private void configurarListeners() {
        if (binding.btnVoltar != null) {
            binding.btnVoltar.setOnClickListener(v -> {
                try { NavHostFragment.findNavController(this).navigateUp(); } catch (Exception ignored) {}
            });
        }

        binding.btnParearDispositivo.setOnClickListener(v -> verificarPermissoesEExecutar(REQUEST_PERMISSIONS_CODE_PAREAR));
    }

    // BLOCO 4: AÇÕES DO ADAPTER PRINCIPAL (MAPA, SWITCH, RENOMEAR E EXCLUIR)
    @Override
    public void onVerNoMapaClicked(DispositivoItem item) {
        if (item.getLatitude() != 0.0 || item.getLongitude() != 0.0) {
            Bundle bundle = new Bundle();
            bundle.putDouble("latitude", item.getLatitude());
            bundle.putDouble("longitude", item.getLongitude());
            bundle.putBoolean("veio_do_dispositivo", true);
            try {
                NavHostFragment.findNavController(this).navigate(R.id.action_dispositivoFragment_to_mapaFragment, bundle);
            } catch (Exception e) {
                NavHostFragment.findNavController(this).navigate(R.id.nav_mapa, bundle);
            }
        } else {
            Toast.makeText(requireContext(), "Aguardando coordenadas do dispositivo " + item.getNome(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSwitchAtivoChanged(DispositivoItem item, boolean ativo) {
        viewModel.alternarEstadoAtivoDispositivo(item.getMacAddress(), ativo);
        String status = ativo ? "ativado para os círculos." : "desativado dos círculos.";
        Toast.makeText(requireContext(), item.getNome() + " " + status, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRenomearClicked(DispositivoItem item) {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(item.getNome());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Renomear Dispositivo")
                .setMessage("Digite o novo nome para este dispositivo:")
                .setView(input)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String novoNome = input.getText().toString().trim();
                    if (!novoNome.isEmpty()) {
                        viewModel.renomearDispositivo(item.getMacAddress(), novoNome);
                        Toast.makeText(requireContext(), "Nome atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onExcluirClicked(DispositivoItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Excluir Dispositivo")
                .setMessage("Tem certeza que deseja remover o rastreador " + item.getNome() + "?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    viewModel.removerDispositivo(item.getMacAddress());
                    Toast.makeText(requireContext(), "Dispositivo removido.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // BLOCO 5: VERIFICAÇÃO DE PERMISSÕES DE RUNTIME
    private void verificarPermissoesEExecutar(int requestCode) {
        List<String> permissoes = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) permissoes.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) permissoes.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissoes.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) permissoes.add(Manifest.permission.ACCESS_FINE_LOCATION);

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
            viewModel.iniciarEConectarServico();
        }
    }

    // BLOCO 6: DIÁLOGO COM RECYCLERVIEW DE CARDS PARA DISPOSITIVOS PRÓXIMOS
    @SuppressLint("MissingPermission")
    private void abrirDialogoSelecaoDispositivo() {
        viewModel.iniciarVarreduraParaSelecao();

        List<BluetoothDevice> listaEncontrados = new ArrayList<>();

        // RecyclerView inflado dinamicamente para exibir a lista de Cards
        RecyclerView rvScan = new RecyclerView(requireContext());
        rvScan.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvScan.setPadding(8, 16, 8, 16);

        AlertDialog dialogScan = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Dispositivos Próximos")
                .setView(rvScan)
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .create();

        DispositivoScanAdapter scanAdapter = new DispositivoScanAdapter(listaEncontrados, device -> {
            dialogScan.dismiss();
            abrirDialogoRenomearESalvar(device);
        });

        rvScan.setAdapter(scanAdapter);
        dialogScan.show();

        // Observa novos dispositivos descobertos no escaneamento BLE
        viewModel.getDispositivosEncontrados().observe(getViewLifecycleOwner(), novosDispositivos -> {
            listaEncontrados.clear();
            if (novosDispositivos != null) {
                listaEncontrados.addAll(novosDispositivos);
            }
            scanAdapter.notifyDataSetChanged();
        });
    }

    // BLOCO 7: DIÁLOGO DE CONFIRMAÇÃO E NOMEAÇÃO DO DISPOSITIVO
    @SuppressLint("MissingPermission")
    private void abrirDialogoRenomearESalvar(BluetoothDevice device) {
        String mac = device.getAddress();

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Ex: Rastreador Moto, GPS Filho");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nome do Dispositivo")
                .setMessage("Digite um nome amigável para este rastreador:")
                .setView(input)
                .setPositiveButton("Salvar e Parear", (dialog, which) -> {
                    String novoNome = input.getText().toString().trim();
                    if (novoNome.isEmpty()) {
                        novoNome = "Aurora GPS (" + mac.substring(mac.length() - 5) + ")";
                    }

                    viewModel.salvarNovoDispositivo(mac, novoNome);
                    viewModel.conectarDispositivoEspecifico(mac);

                    Toast.makeText(requireContext(), "Dispositivo " + novoNome + " pareado!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}