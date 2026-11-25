package com.equipe1.aurora.ui.contatos;

import android.view.LayoutInflater;//transforma XML em View Android
import android.view.View;//classe base para Elementos visuais
import android.view.ViewGroup;//Componente que guarda outras Views
import androidx.annotation.NonNull;//não deve ser nulo
import androidx.recyclerview.widget.RecyclerView;//Cria lista de elementos

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.ImageButton;

import com.bumptech.glide.Glide;
import com.equipe1.aurora.R;
import com.equipe1.aurora.domain.model.Contato;

import java.util.List;//permite trabalha com lista


public class ContatoAdapter extends RecyclerView.Adapter<ContatoAdapter.ViewHolder> {

    private List<Contato> contatos;
    private OnContatoListener listener;

    public interface OnContatoListener {
        void onDeleteClick(Long id);
        void onSosChecked(Long id, boolean isChecked);
    }

    public ContatoAdapter(List<Contato> contatos, OnContatoListener listener) {
        this.contatos = contatos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_contatos_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contato contato = contatos.get(position);

        holder.txtNome.setText(contato.getNome());
        holder.txtTelefone.setText(contato.getTelefone());




        // Evita disparo falso do listener ao reciclar a view
        holder.checkSos.setOnCheckedChangeListener(null);
        holder.checkSos.setChecked(contato.isSos());

        holder.checkSos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.onSosChecked(contato.getId(), isChecked);
        });

        holder.btnDeletar.setOnClickListener(v -> listener.onDeleteClick(contato.getId())); // Ajuste rápido de visualização
        //O Glinde
        Glide.with(holder.itemView.getContext())
                .load(contato.getUrlFoto())// Puxa direto da string que veio do banco
                .placeholder(R.drawable.ic_launcher_foreground)// Imagem padrão enquanto carrega
                .into(holder.imgPerfil);
    }

    @Override
    public int getItemCount() { return contatos.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNome, txtTelefone;
        CheckBox checkSos;
        ImageButton btnDeletar;
        ImageView imgPerfil;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgPerfil = itemView.findViewById(R.id.imgPerfil);
            txtNome = itemView.findViewById(R.id.txtNome);
            txtTelefone = itemView.findViewById(R.id.txtTelefone);
            checkSos = itemView.findViewById(R.id.checkSos);
            btnDeletar = itemView.findViewById(R.id.btnDelete);
        }
    }
}