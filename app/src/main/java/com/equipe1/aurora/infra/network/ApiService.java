package com.equipe1.aurora.infra.network;

import com.equipe1.aurora.infra.dto.ConsultaResponseDto;
import com.equipe1.aurora.infra.dto.UsuarioDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // -----------------------------------------------------------------
    // BLOCO 1: AUTENTICAÇÃO DE USUÁRIOS
    // -----------------------------------------------------------------
    @POST("api/auth/cadastro")
    Call<UsuarioDto> cadastrarUsuario(@Body UsuarioDto usuario);

    @POST("api/auth/login")
    Call<UsuarioDto> realizarLoginApi(@Body UsuarioDto usuario);

    // -----------------------------------------------------------------
    // BLOCO 2: CONSULTA DE DISPOSITIVOS E RASTREAMENTO GPS
    // -----------------------------------------------------------------
    // Busca o histórico geral de pontos/dispositivos gravados no MySQL
    @Headers("X-API-Key: aurora-gps-2026")
    @GET("consultar.php")
    Call<ConsultaResponseDto> consultarDispositivos(
            @Query("limite") Integer limite,
            @Query("ordem") String ordem
    );

    // Busca filtrando por um dispositivo específico e/ou ocorrência de SOS
    @Headers("X-API-Key: aurora-gps-2026")
    @GET("consultar.php")
    Call<ConsultaResponseDto> consultarDispositivoPorUid(
            @Query("device_uid") String deviceUid,
            @Query("sos") Boolean sos,
            @Query("limite") Integer limite
    );
}