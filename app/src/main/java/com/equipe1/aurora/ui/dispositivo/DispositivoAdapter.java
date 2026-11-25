package com.equipe1.aurora.ui.dispositivo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.equipe1.aurora.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;
import java.util.Locale;

// =====================================================================
// AURORA GPS - ADAPTER DO RECYCLERVIEW (DispositivoAdapter.java)
// =====================================================================
// Vincula os dados do DispositivoItem à interface, tratando a exibição
// expandida ("Mais detalhes") e eventos do switch, renomear e excluir.
// =====================================================================

public class DispositivoAdapter extends RecyclerView.Adapter<DispositivoAdapter.ViewHolder> {

    private final List<DispositivoItem> listaDispositivos;
    private final OnDispositivoActionListener listener;

    public interface OnDispositivoActionListener {
        void onVerNoMapaClicked(DispositivoItem item);
        void onSwitchAtivoChanged(DispositivoItem item, boolean ativo);
        void onRenomearClicked(DispositivoItem item);
        void onExcluirClicked(DispositivoItem item);
    }

    public DispositivoAdapter(List<DispositivoItem> listaDispositivos, OnDispositivoActionListener listener) {
        this.listaDispositivos = listaDispositivos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dispositivo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DispositivoItem item = listaDispositivos.get(position);
        Context context = holder.itemView.getContext();

        // 1. Preenchimento do Cabeçalho
        holder.txtNome.setText(item.getNome());
        holder.txtMac.setText("MAC: " + item.getMacAddress());

        if (item.isConectado()) {
            holder.txtStatusConexao.setText("Conectado via BLE");
            holder.txtStatusConexao.setTextColor(ContextCompat.getColor(context, R.color.cor_segundaria));
        } else {
            holder.txtStatusConexao.setText("Desconectado");
            holder.txtStatusConexao.setTextColor(ContextCompat.getColor(context, R.color.vermelho_alerta));
        }

        // 2. Configuração do Switch Ativar/Desativar
        holder.switchAtivo.setOnCheckedChangeListener(null);
        holder.switchAtivo.setChecked(item.isAtivo());
        holder.switchAtivo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setAtivo(isChecked);
            listener.onSwitchAtivoChanged(item, isChecked);
        });

        // 3. Controle do Painel Expandível "Mais detalhes"
        holder.layoutDetalhes.setVisibility(item.isExpandido() ? View.VISIBLE : View.GONE);
        holder.btnMaisDetalhes.setOnClickListener(v -> {
            item.setExpandido(!item.isExpandido());
            notifyItemChanged(position);
        });

        // 4. Preenchimento do Painel de Detalhes
        holder.txtBateria.setText(item.getNivelBateria() + "%");
        holder.txtStatusGps.setText(item.getStatusGps());

        if (item.getLatitude() != 0.0 || item.getLongitude() != 0.0) {
            holder.txtLat.setText(String.format(Locale.getDefault(), "%.7f", item.getLatitude()));
            holder.txtLon.setText(String.format(Locale.getDefault(), "%.7f", item.getLongitude()));
        } else {
            holder.txtLat.setText("--.------");
            holder.txtLon.setText("--.------");
        }

        holder.txtAltitude.setText(String.format(Locale.getDefault(), "%.0f m", item.getAltitude()));
        holder.txtVelocidade.setText(String.format(Locale.getDefault(), "%.0f km/h", item.getVelocidade()));
        holder.txtSatelites.setText(item.getSatelites() + " sat");

        // 5. Acionadores de Ações
        holder.btnMapa.setOnClickListener(v -> listener.onVerNoMapaClicked(item));
        holder.btnRenomear.setOnClickListener(v -> listener.onRenomearClicked(item));
        holder.btnExcluir.setOnClickListener(v -> listener.onExcluirClicked(item));
    }

    @Override
    public int getItemCount() {
        return listaDispositivos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNome, txtMac, txtStatusConexao, btnMaisDetalhes;
        SwitchMaterial switchAtivo;
        View layoutDetalhes;
        TextView txtBateria, txtStatusGps, txtLat, txtLon, txtAltitude, txtVelocidade, txtSatelites;
        View btnMapa, btnRenomear, btnExcluir;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNome = itemView.findViewById(R.id.txtNomeDispositivo);
            txtMac = itemView.findViewById(R.id.txtMacDispositivo);
            txtStatusConexao = itemView.findViewById(R.id.txtStatusConexao);
            btnMaisDetalhes = itemView.findViewById(R.id.btnMaisDetalhes);
            switchAtivo = itemView.findViewById(R.id.switchAtivo);

            layoutDetalhes = itemView.findViewById(R.id.layoutDetalhes);

            txtBateria = itemView.findViewById(R.id.txtNivelBateria);
            txtStatusGps = itemView.findViewById(R.id.txtStatusGps);
            txtLat = itemView.findViewById(R.id.txtLatitude);
            txtLon = itemView.findViewById(R.id.txtLongitude);
            txtAltitude = itemView.findViewById(R.id.txtAltitude);
            txtVelocidade = itemView.findViewById(R.id.txtVelocidade);
            txtSatelites = itemView.findViewById(R.id.txtSatelites);

            btnMapa = itemView.findViewById(R.id.btnVerLocalizacao);
            btnRenomear = itemView.findViewById(R.id.btnRenomear);
            btnExcluir = itemView.findViewById(R.id.btnExcluir);
        }
    }
}