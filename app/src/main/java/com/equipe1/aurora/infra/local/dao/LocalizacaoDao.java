package com.equipe1.aurora.infra.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.equipe1.aurora.infra.local.entity.LocalizacaoEntity;

import java.util.List;

@Dao
public interface LocalizacaoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long inserir(LocalizacaoEntity localizacao);

    @Update
    void atualizar(LocalizacaoEntity localizacao);

    // Retorna o último registro de localização salvo no dispositivo
    @Query("SELECT * FROM localizacao ORDER BY timestamp DESC LIMIT 1")
    LocalizacaoEntity obterUltimaLocalizacao();

    // Busca coordenadas offline que ainda não foram enviadas ao banco MySQL
    @Query("SELECT * FROM localizacao WHERE sincronizado = 0")
    List<LocalizacaoEntity> obterPendentesSincronizacao();

    // Limpa coordenadas já enviadas para economizar armazenamento local
    @Query("DELETE FROM localizacao WHERE sincronizado = 1")
    void limparSincronizadas();
}