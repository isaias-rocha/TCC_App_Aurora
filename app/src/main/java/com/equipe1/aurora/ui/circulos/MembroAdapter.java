package com.equipe1.aurora.ui.circulos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.equipe1.aurora.R;
import com.equipe1.aurora.domain.model.Membro;

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MembroAdapter extends RecyclerView.Adapter<MembroAdapter.VH> {
    public interface OnMembroClick { void onClick(Membro m); }
    private final List<Membro> itens = new ArrayList<>();
    private final OnMembroClick listener;
    public MembroAdapter(OnMembroClick listener) { this.listener = listener; }
    public void submit(List<Membro> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_membro, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Membro m = itens.get(position);
        h.avatar.setText(m.iniciais());
        h.nome.setText(m.nome);
        h.status.setText(m.status);
        h.bateria.setText(m.bateria + "%");

        // TalkBack: lê a linha inteira de uma vez e anuncia "Ver no mapa" como ação do toque duplo
        h.itemView.setContentDescription(
                m.nome + ", " + m.status + ", bateria em " + m.bateria + " por cento");
        ViewCompat.replaceAccessibilityAction(h.itemView,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                "Ver no mapa", null);

        h.itemView.setOnClickListener(v -> listener.onClick(m));
    }

    @Override public int getItemCount() { return itens.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView avatar, nome, status, bateria;
        VH(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.tvAvatar);
            nome = v.findViewById(R.id.tvNomeMembro);
            status = v.findViewById(R.id.tvStatusMembro);
            bateria = v.findViewById(R.id.tvBateria);
        }
    }
}