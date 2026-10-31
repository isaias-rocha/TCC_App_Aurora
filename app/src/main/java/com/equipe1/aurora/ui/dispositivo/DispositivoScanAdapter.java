package com.equipe1.aurora.ui.dispositivo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.equipe1.aurora.R;

import java.util.List;

// =====================================================================
// AURORA GPS - ADAPTER DA LISTA DE BUSCA BLE (DispositivoScanAdapter.java)
// =====================================================================
// Conecta os dispositivos Bluetooth LE localizados aos Cards do diálogo
// de varredura (item_dispositivo_scan.xml).
// =====================================================================

public class DispositivoScanAdapter extends RecyclerView.Adapter<DispositivoScanAdapter.ScanViewHolder> {

    private final List<BluetoothDevice> listaDispositivos;
    private final OnDeviceClickListener listener;

    // BLOCO 1: INTERFACE DE EVENTO DE CLIQUE NO CARD DE BUSCA
    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    public DispositivoScanAdapter(List<BluetoothDevice> listaDispositivos, OnDeviceClickListener listener) {
        this.listaDispositivos = listaDispositivos;
        this.listener = listener;
    }

    // BLOCO 2: INFLAÇÃO DO LAYOUT DO CARD DA BUSCA
    @NonNull
    @Override
    public ScanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dispositivo_buscar, parent, false);
        return new ScanViewHolder(view);
    }

    // BLOCO 3: ANEXAÇÃO DOS DADOS DO DISPOSITIVO AO CARD
    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull ScanViewHolder holder, int position) {
        BluetoothDevice device = listaDispositivos.get(position);

        String nome = device.getName();
        if (nome == null || nome.trim().isEmpty()) {
            nome = "Dispositivo Desconhecido";
        }

        holder.txtNome.setText(nome);
        holder.txtMac.setText("MAC: " + device.getAddress());

        // Clique no Card dispara a seleção e o diálogo para nomear
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaDispositivos.size();
    }

    // BLOCO 4: VIEWHOLDER DOS COMPONENTES DO CARD DA BUSCA
    public static class ScanViewHolder extends RecyclerView.ViewHolder {
        TextView txtNome, txtMac;

        public ScanViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNome = itemView.findViewById(R.id.txtNomeDispositivoBuscar);
            txtMac = itemView.findViewById(R.id.txtMacDispositivoBuscar);
        }
    }
}