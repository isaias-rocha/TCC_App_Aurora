package com.equipe1.aurora.infra.repository;

import androidx.annotation.NonNull;

import com.equipe1.aurora.domain.model.Endereco;
import com.equipe1.aurora.domain.repository.EnderecoRepository;
import com.equipe1.aurora.infra.dto.ViaCepResponseDto;
import com.equipe1.aurora.infra.network.api.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Implementação do repositório que utiliza o ApiClient principal do projeto para consultar a ViaCEP
public class EnderecoRepositoryImpl implements EnderecoRepository {

    @Override
    public void consultarCep(String cep, ConsultaCepCallback callback) {
        // Monta a URL completa exigida pela API pública da ViaCEP
        String urlViaCep = "https://viacep.com.br/ws/" + cep + "/json/";

        // Reutiliza o Singleton ApiClient existente no projeto
        ApiClient.getApiService().buscarEnderecoPorCep(urlViaCep)
                .enqueue(new Callback<ViaCepResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<ViaCepResponseDto> call,
                                           @NonNull Response<ViaCepResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ViaCepResponseDto dto = response.body();

                            // Verifica se a ViaCEP retornou {"erro": true} para CEP inexistente
                            if (dto.isErro()) {
                                callback.onErro("CEP não encontrado.");
                                return;
                            }

                            // Converte o DTO da resposta para o modelo de domínio Endereco
                            Endereco endereco = new Endereco();
                            endereco.setCep(cep);
                            endereco.setLogradouro(dto.getLogradouro() != null ? dto.getLogradouro() : "");
                            endereco.setBairro(dto.getBairro() != null ? dto.getBairro() : "");
                            endereco.setCidade(dto.getLocalidade() != null ? dto.getLocalidade() : "");
                            endereco.setEstado(dto.getUf() != null ? dto.getUf() : "");

                            callback.onSucesso(endereco);
                        } else {
                            callback.onErro("Erro ao consultar CEP (Código: " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ViaCepResponseDto> call, @NonNull Throwable t) {
                        callback.onErro("Sem conexão para buscar o CEP: " + t.getMessage());
                    }
                });
    }
}