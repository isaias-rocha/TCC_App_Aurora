package com.equipe1.aurora.ui.historico;

// =========================================================================
// BLOCO 1: IMPORTAÇÕES DE DEPENDÊNCIAS
// =========================================================================
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.ItemHistoricoCardBinding;
import com.equipe1.aurora.domain.model.HistoricoItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// =========================================================================
// BLOCO 2: CLASSE DO ADAPTER COM MULTI-VIEW HOLDER
// =========================================================================
public class HistoricoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface OnItemClickListener {
        void onItemClick(HistoricoItem item);
    }

    private final List<Object> itensComHeaders = new ArrayList<>();
    private final OnItemClickListener listener;

    public HistoricoAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    // =========================================================================
    // BLOCO 3: PROCESSAMENTO DA LISTA E AGRUPAMENTO POR CABEÇALHO (HOJE, ONTEM, DATA)
    // =========================================================================
    public void setItens(List<HistoricoItem> novosItens) {
        this.itensComHeaders.clear();
        if (novosItens != null && !novosItens.isEmpty()) {
            String ultimaDataFormatada = "";

            for (HistoricoItem item : novosItens) {
                String dataHeader = formatarHeaderData(item.getDataHora());
                if (!dataHeader.equals(ultimaDataFormatada)) {
                    itensComHeaders.add(dataHeader);
                    ultimaDataFormatada = dataHeader;
                }
                itensComHeaders.add(item);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return itensComHeaders.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    // =========================================================================
    // BLOCO 4: CRIAÇÃO E VINCULAÇÃO DE VIEWS
    // =========================================================================
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cabecalho_data, parent, false);
            return new HeaderViewHolder(view);
        } else {
            ItemHistoricoCardBinding binding = ItemHistoricoCardBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new ItemViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            ((HeaderViewHolder) holder).bind((String) itensComHeaders.get(position));
        } else {
            ((ItemViewHolder) holder).bind((HistoricoItem) itensComHeaders.get(position), listener);
        }
    }

    @Override
    public int getItemCount() {
        return itensComHeaders.size();
    }

    // =========================================================================
    // BLOCO 5: FORMATADOR DE DATA PARA OS CABEÇALHOS
    // =========================================================================
    private String formatarHeaderData(Date data) {
        if (data == null) return "Sem Data";
        if (DateUtils.isToday(data.getTime())) {
            return "Hoje";
        }
        if (DateUtils.isToday(data.getTime() + DateUtils.DAY_IN_MILLIS)) {
            return "Ontem";
        }
        return DateFormat.format("dd/MM/yyyy", data).toString();
    }

    // =========================================================================
    // BLOCO 6: VIEWHOLDERS (CABECALHO E CARDS)
    // =========================================================================
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvHeader;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeaderData);
        }

        public void bind(String textoHeader) {
            tvHeader.setText(textoHeader);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoricoCardBinding binding;

        public ItemViewHolder(ItemHistoricoCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(HistoricoItem item, OnItemClickListener listener) {
            binding.tvTitulo.setText(item.getTitulo());
            binding.tvDescricao.setText(item.getDescricao());

            CharSequence dataFormatada = DateFormat.format("HH:mm:ss", item.getDataHora());
            binding.tvDataHora.setText(dataFormatada);

            if ("DISPOSITIVO".equalsIgnoreCase(item.getOrigem())) {
                binding.tvOrigem.setText("Dispositivo");
            } else {
                binding.tvOrigem.setText("App");
            }

            if (item.getTipo() == HistoricoItem.TipoHistorico.ALERTA_SOS) {
                binding.imgIconeTipo.setImageResource(R.drawable.ic_alerta);
            } else {
                binding.imgIconeTipo.setImageResource(R.drawable.ic_localizacao);
            }

            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}