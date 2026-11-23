package com.equipe1.aurora.rede.api;

import com.equipe1.aurora.domain.model.AlertaSos;
import com.equipe1.aurora.domain.model.BairroPoligono;
import com.equipe1.aurora.domain.model.Circulo;
import com.equipe1.aurora.domain.model.MembroMapaDTO;
import com.equipe1.aurora.domain.model.OcorrenciaCrime;
import com.equipe1.aurora.domain.model.UsuarioDto;
import com.equipe1.aurora.ui.contacts.Contato;
import com.equipe1.aurora.domain.model.AreaRisco;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/auth/cadastro")
    Call<UsuarioDto> cadastrarUsuario(@Body UsuarioDto usuario);

    @POST("api/auth/login")
    Call<UsuarioDto> realizarLoginApi(@Body UsuarioDto usuario);

    @POST("api/mapa/crimes")
    Call<OcorrenciaCrime> registrarCrime(@Body OcorrenciaCrime novaOcorrencia);

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

    // --- CÍRCULOS FAMILIARES ---
    @POST("api/circulos")
    Call<Circulo> criarCirculo(
            @Query("usuarioId") Long usuarioId,
            @Query("nome") String nome
    );

    @POST("api/circulos/entrar")
    Call<Object> entrarNoCirculo(
            @Query("usuarioId") Long usuarioId,
            @Query("codigo") String codigo
    );

    @GET("api/circulos/{usuarioId}/membros")
    Call<List<MembroMapaDTO>> listarMembrosCirculos(@Path("usuarioId") Long usuarioId);

    // --- LOCALIZAÇÃO E SOS ---
    @POST("api/localizacao")
    Call<Void> enviarLocalizacao(
            @Query("usuarioId") Long usuarioId,
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("sos") boolean sos
    );

    @POST("api/sos/acionar")
    Call<AlertaSos> acionarSos(
            @Query("usuarioId") Long usuarioId,
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("origem") String origem
    );

    @PUT("api/sos/{idAlerta}/encerrar")
    Call<AlertaSos> encerrarSos(@Path("idAlerta") Long idAlerta);

    @GET("api/sos/ativos/{usuarioId}")
    Call<List<AlertaSos>> buscarAlertasSosAtivos(@Path("usuarioId") Long usuarioId);
}