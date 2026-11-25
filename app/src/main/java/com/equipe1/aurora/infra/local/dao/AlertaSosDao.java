package com.equipe1.aurora.infra.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.equipe1.aurora.infra.local.entity.AlertaSosEntity;

import java.util.List;

/**
 * Interface DAO responsável pelas operações diretas na tabela 'alerta_sos_local'.
 */
@Dao
public interface AlertaSosDao {

    // =========================================================================
    // OPERAÇÕES DE INSERÇÃO E ATUALIZAÇÃO LOCAL
    // =========================================================================

    /**
     * Insere um novo alerta SOS na base de dados local.
     * @param alerta Objeto com os dados do evento de emergência.
     * @return Retorna o ID gerado para a nova linha inserida.
     */
    @Insert
    long inserir(AlertaSosEntity alerta);

    /**
     * Atualiza o estado de um alerta quando o utilizador decide encerrá-lo ou cancelá-lo
     * @param id Identificador do alerta na base local.
     * @param novoStatus Novo estado ('ENCERRADO' ou 'CANCELADO')[cite: 74].
     * @param dataHoraFim Timestamp do momento em que o alerta foi finalizado.
     */
    @Query("UPDATE alerta_sos_local SET status = :novoStatus, dataHoraFim = :dataHoraFim WHERE id = :id")
    void atualizarStatus(int id, String novoStatus, long dataHoraFim);

    // =========================================================================
    // OPERAÇÕES DE SINCRONIZAÇÃO DE REDE (SUPORTE OFFLINE)
    // =========================================================================

    /**
     * Procura todos os alertas efetuados offline que ainda não foram enviados para o servidor central
     * @return Lista de alertas pendentes de envio.
     */
    @Query("SELECT * FROM alerta_sos_local WHERE sincronizado = 0")
    List<AlertaSosEntity> obterNaoSincronizados();

    /**
     * Marca o alerta como sincronizado após a confirmação da recepção da API Web (MySQL)
     * @param id Identificador local do alerta.
     */
    @Query("UPDATE alerta_sos_local SET sincronizado = 1 WHERE id = :id")
    void marcarComoSincronizado(int id);
}