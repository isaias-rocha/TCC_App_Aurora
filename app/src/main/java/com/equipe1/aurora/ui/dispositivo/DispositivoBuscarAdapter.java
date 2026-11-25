package com.equipe1.aurora.ui.dispositivo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.equipe1.aurora.R;

import java.util.List;

// =====================================================================
// AURORA GPS - ADAPTER DA DIÁLOGO DE BUSCA (DispositivoBuscaAdapter.java)
// =====================================================================
// Exibe a lista de dispositivos BLE encontrados em formato de Cards
// dentro da caixa de diálogo de seleção de pareamento.
// =====================================================================

public class DispositivoBuscarAdapter extends RecyclerView.Adapter<DispositivoBuscarAdapter.ViewHolder> {

    private final List<BluetoothDevice> listaDispositivos;
    private final OnDeviceSelectedListener listener;

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    public DispositivoBuscarAdapter(List<BluetoothDevice> listaDispositivos, OnDeviceSelectedListener listener) {
        this.listaDispositivos = listaDispositivos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dispositivo_buscar, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice device = listaDispositivos.get(position);

        String nome = device.getName();
        if (nome == null || nome.isEmpty()) {
            nome = "Dispositivo Desconhecido";
        }

        holder.txtNome.setText(nome);
        holder.txtMac.setText("MAC: " + device.getAddress());

        // Altera o ícone de destaque para o hardware oficial
        if (nome.contains("Aurora")) {
            holder.imgIcone.setImageResource(R.drawable.ic_hardware);
        } else {
            holder.imgIcone.setImageResource(R.drawable.ic_sinal);
        }

        holder.itemView.setOnClickListener(v -> listener.onDeviceSelected(device));
    }

    @Override
    public int getItemCount() {
        return listaDispositivos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcone;
        TextView txtNome, txtMac;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcone = itemView.findViewById(R.id.imgIcone);
            txtNome = itemView.findViewById(R.id.txtNomeDispositivoBuscar);
            txtMac = itemView.findViewById(R.id.txtMacDispositivoBuscar);
        }
    }
}