package com.equipe1.aurora.infra.repository;

// =========================================================================
// 1. IMPORTAÇÕES DAS CAMADAS DOMAIN E INFRA
// =========================================================================
import com.equipe1.aurora.domain.model.Usuario;
import com.equipe1.aurora.domain.repository.AuthRepository;
import com.equipe1.aurora.infra.dto.UsuarioDto;
import com.equipe1.aurora.infra.network.api.ApiClient;
import com.equipe1.aurora.infra.network.api.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// =========================================================================
// 2. IMPLEMENTAÇÃO CONCRETA DO REPOSITÓRIO DE AUTENTICAÇÃO
// =========================================================================
// Realiza as chamadas à API via Retrofit e converte os dados entre DTO e Domínio.
public class AuthRepositoryImpl implements AuthRepository {

    // =========================================================================
    // 3. ATRIBUTOS E CONSTRUTOR
    // =========================================================================
    private final ApiService apiService;
    public AuthRepositoryImpl() {
        this.apiService = ApiClient.getApiService();
    }

    // =========================================================================
    // 4. EXECUÇÃO DA REQUISIÇÃO DE LOGIN NA API
    // =========================================================================
    @Override
    public void realizarLogin(String email, String senha, AuthCallback<Usuario> callback) {
        UsuarioDto loginDto = new UsuarioDto();
        loginDto.setEmail(email);
        loginDto.setSenha(senha);

        apiService.realizarLoginApi(loginDto).enqueue(new Callback<UsuarioDto>() {
            @Override
            public void onResponse(Call<UsuarioDto> call, Response<UsuarioDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuario = converterDtoParaDomain(response.body());
                    callback.onSuccess(usuario);
                } else {
                    callback.onError("E-mail ou senha inválidos.");
                }
            }

            @Override
            public void onFailure(Call<UsuarioDto> call, Throwable t) {
                callback.onError("Falha na conexão com o servidor: " + t.getMessage());
            }
        });
    }

    // =========================================================================
    // 5. EXECUÇÃO DA REQUISIÇÃO DE CADASTRO NA API
    // =========================================================================
    @Override
    public void cadastrarUsuario(Usuario usuario, AuthCallback<Usuario> callback) {
        UsuarioDto dto = converterDomainParaDto(usuario);

        apiService.cadastrarUsuario(dto).enqueue(new Callback<UsuarioDto>() {
            @Override
            public void onResponse(Call<UsuarioDto> call, Response<UsuarioDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuarioCadastrado = converterDtoParaDomain(response.body());
                    callback.onSuccess(usuarioCadastrado);
                } else {
                    String erroMsg = "Erro ao cadastrar usuário.";
                    try {
                        if (response.errorBody() != null) {
                            erroMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    callback.onError(erroMsg);
                }
            }

            @Override
            public void onFailure(Call<UsuarioDto> call, Throwable t) {
                callback.onError("Falha na conexão com o servidor: " + t.getMessage());
            }
        });
    }

    // =========================================================================
    // 6. MAPEAMENTO: DTO -> DOMÍNIO
    // =========================================================================
    private Usuario converterDtoParaDomain(UsuarioDto dto) {
        Usuario.TipoUsuario tipo = Usuario.TipoUsuario.COMUM;
        if (dto.getTipoUsuario() != null && dto.getTipoUsuario().equalsIgnoreCase("PREMIUM")) {
            tipo = Usuario.TipoUsuario.PREMIUM;
        }

        return new Usuario(
                dto.getId(),
                dto.getFirebaseUid(),
                dto.getNome(),
                dto.getEmail(),
                dto.getSenha(),
                dto.getTelefone(),
                dto.getFotoUrl(),
                tipo,
                null
        );
    }

    // =========================================================================
    // 7. MAPEAMENTO: DOMÍNIO -> DTO
    // =========================================================================
    private UsuarioDto converterDomainParaDto(Usuario domain) {
        UsuarioDto dto = new UsuarioDto(
                domain.getNome(),
                domain.getEmail(),
                domain.getSenha(),
                domain.getTelefone(),
                domain.getFirebaseUid()
        );
        dto.setId(domain.getId());
        dto.setFotoUrl(domain.getFotoUrl());
        if (domain.getTipoUsuario() != null) {
            dto.setTipoUsuario(domain.getTipoUsuario().name());
        }
        return dto;
    }
}