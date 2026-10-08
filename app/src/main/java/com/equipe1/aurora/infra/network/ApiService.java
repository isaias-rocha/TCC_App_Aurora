package com.equipe1.aurora.infra.network;

import com.equipe1.aurora.infra.dto.UsuarioDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/auth/cadastro")
    Call<UsuarioDto> cadastrarUsuario(@Body UsuarioDto usuario);

    // Adicione esta rota para o Login
    @POST("api/auth/login")
    Call<UsuarioDto> realizarLoginApi(@Body UsuarioDto usuario);
}