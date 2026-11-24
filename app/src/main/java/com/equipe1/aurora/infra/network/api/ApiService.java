package com.equipe1.aurora.infra.network.api;

import com.equipe1.aurora.domain.model.AreaRisco;
import com.equipe1.aurora.domain.model.BairroPoligono;
import com.equipe1.aurora.domain.model.Contato;
import com.equipe1.aurora.domain.model.OcorrenciaCrime;
import com.equipe1.aurora.infra.dto.ConsultaResponseDto;
import com.equipe1.aurora.infra.dto.UsuarioDto;
import com.equipe1.aurora.infra.dto.ViaCepResponseDto;
import com.equipe1.aurora.infra.local.entity.AlertaSosEntity;

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
import retrofit2.http.Url;

public interface ApiService {

    // =========================================================================
    // BLOCO 1: AUTENTICAÇÃO DE USUÁRIOS
    // =========================================================================
    @POST("api/auth/cadastro")
    Call<UsuarioDto> cadastrarUsuario(@Body UsuarioDto usuario);

    @POST("api/auth/login")
    Call<UsuarioDto> realizarLoginApi(@Body UsuarioDto usuario);

    // =========================================================================
    // BLOCO 2: CONSULTA DE DISPOSITIVOS E RASTREAMENTO GPS
    // =========================================================================
    @Headers("X-API-Key: equipe01-aurora")
    @GET("consultar.php")
    Call<ConsultaResponseDto> consultarDispositivos(
            @Query("limite") Integer limite,
            @Query("ordem") String ordem
    );

    @Headers("X-API-Key: equipe01-aurora")
    @GET("consultar.php")
    Call<ConsultaResponseDto> consultarDispositivoPorUid(
            @Query("device_uid") String deviceUid,
            @Query("sos") Boolean sos,
            @Query("limite") Integer limite
    );

    // Endpoint adicionado para sincronização de alertas locais gerados offline no Room
    @Headers("X-API-Key: equipe01-aurora")
    @POST("sincronizar_sos.php")
    Call<Void> sincronizarAlertaOffline(@Body AlertaSosEntity alerta);

    // =========================================================================
    // BLOCO 3: GESTÃO DE CONTATOS DE EMERGÊNCIA
    // =========================================================================
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

    // =========================================================================
    // BLOCO 4: ÁREAS DE RISCO, POLÍGONOS E MAPAS
    // =========================================================================
    @GET("api/riscos")
    Call<List<AreaRisco>> listarRiscos();

    @GET("api/mapa/bairros")
    Call<List<BairroPoligono>> listarBairros();

    @GET("api/mapa/crimes")
    Call<List<OcorrenciaCrime>> listarCrimes();

    // =========================================================================
    // BLOCO 5: CONSULTA DE ENDEREÇO EXTERNA (VIACEP)
    // =========================================================================
    // @Url permite passar a URL completa da ViaCEP sobrescrevendo a BASE_URL padrão
    @GET
    Call<ViaCepResponseDto> buscarEnderecoPorCep(@Url String urlCompleta);
}