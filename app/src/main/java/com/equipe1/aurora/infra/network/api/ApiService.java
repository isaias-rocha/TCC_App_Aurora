package com.equipe1.aurora.infra.network.api;

import com.equipe1.aurora.domain.model.AreaRisco;
import com.equipe1.aurora.domain.model.BairroPoligono;
import com.equipe1.aurora.domain.model.Contato;
import com.equipe1.aurora.domain.model.OcorrenciaCrime;
import com.equipe1.aurora.infra.dto.ConsultaResponseDto;
import com.equipe1.aurora.infra.dto.UsuarioDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
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

    @FormUrlEncoded
    @POST("api/contatos/enviar-convite")
    Call<Void> enviarSolicitacaoContato(
            @Field("usuarioId") Long usuarioId,
            @Field("contatoId") Long contatoId
    );

    @GET("api/contatos/{usuarioId}")
    Call<List<Contato>> listarContatos(@Path("usuarioId") Long usuarioId);

    @DELETE("api/contatos/{id}")
    Call<Void> deletarContato(@Path("id") Long contatoId);

    @FormUrlEncoded
    @PUT("api/contatos/{id}/sos")
    Call<Void> atualizarSosContato(
            @Path("id") Long contatoId,
            @Field("isSos") boolean isSos
    );

    @GET("api/riscos")
    Call<List<AreaRisco>> listarRiscos();

    // Bairro (polígonos e áreas de risco)
    @GET("api/mapa/bairros")
    Call<List<BairroPoligono>> listarBairros();

    // Ocorrências de crimes
    @GET("api/mapa/crimes")
    Call<List<OcorrenciaCrime>> listarCrimes();
}