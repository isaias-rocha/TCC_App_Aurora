package com.equipe1.aurora.ui.contatos;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.equipe1.aurora.R;
import com.equipe1.aurora.infra.network.api.ApiClient;
import com.equipe1.aurora.domain.model.Contato;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContatosFragment extends Fragment {

    private RecyclerView recyclerView;
    private ContatoAdapter contatoAdapter;
    private Button btnAdicionarContato;
    private  Long usuarioIdLogado;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Infla o layut do fragmento
        View view = inflater.inflate(R.layout.fragment_contatos, container, false);

        //Mapeando os componentes da tela
        recyclerView = view.findViewById(R.id.rv_contatos);
        btnAdicionarContato = view.findViewById(R.id.btn_adicionar_contato);

        //Configurando o RecycleView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //Recuperar o ID do usuário logado armazenado no SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("AuroraPrefs", Context.MODE_PRIVATE);
        usuarioIdLogado = prefs.getLong("usuarioId_logado", prefs.getLong("usuariosId_logado", -1L)); //-1 se não encontrar

        //Carregar os contatos reais do servirdor
        if(usuarioIdLogado != -1L){
            carregarContatosDaApi();
        }else {
            Toast.makeText(getContext(), "Erro: Usuário não identificado.", Toast.LENGTH_SHORT).show();
        }

        //Configuração do borão Azul: Abrir Dialong para adicionar contato
        btnAdicionarContato.setOnClickListener(v -> abrirDialogAdicionarContato());

        return view;
    }

    private void carregarContatosDaApi() {
        ApiClient.getApiService().listarContatos(usuarioIdLogado).enqueue(new Callback<List<Contato>>() {
            @Override
            public void onResponse(Call<List<Contato>> call, Response<List<Contato>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Contato> listaContatos = response.body();

                    contatoAdapter = new ContatoAdapter(listaContatos, new ContatoAdapter.OnContatoListener() {
                        @Override
                        public void onDeleteClick(Long id) {
                            // Chamada Retrofit para deletar
                            ApiClient.getApiService().deletarContato(id).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (getContext() == null) return;
                                    if (response.isSuccessful()) {
                                        Toast.makeText(getContext(), "Contato removido com sucesso!", Toast.LENGTH_SHORT).show();
                                        carregarContatosDaApi(); // Recarrega a lista
                                    } else {
                                        Toast.makeText(getContext(), "Erro ao remover contato.", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Erro de conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onSosChecked(Long id, boolean isChecked) {
                            // Chamada Retrofit para atualizar o SOS corrigida com .enqueue()
                            ApiClient.getApiService().atualizarSosContato(id, isChecked).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (getContext() == null) return;
                                    if (!response.isSuccessful()) {
                                        Toast.makeText(getContext(), "Erro ao atualizar Status SOS", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Erro de conexão.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
                    recyclerView.setAdapter(contatoAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<Contato>> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Erro de Conexão ao Buscar Contatos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void abrirDialogAdicionarContato() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Adicionar Contato");

        final  EditText input = new EditText(getContext());
        input.setHint("Digite o telefone do contato");
        builder.setView(input);

        builder.setPositiveButton("Enviar Convite", (dialog, which) ->{
            String telefone = input.getText().toString().trim();
            if (!telefone.isEmpty()) {
                //Se você tiver uma rota que busca ou adiciona por telefone:
                Toast.makeText(getContext(), "Enviando convite para: " + telefone, Toast.LENGTH_SHORT).show();
                // Exemplo: ApiClient.getApiService().enviarConviteContato(usuarioIdLogado, telefone)...
            }else {
                Toast.makeText(getContext(), "Digite um telefone válido", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}