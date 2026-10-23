package com.equipe1.aurora.infra.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.equipe1.aurora.infra.local.dao.AlertaSosDao;
import com.equipe1.aurora.infra.local.dao.LocalizacaoDao;
import com.equipe1.aurora.infra.local.entity.AlertaSosEntity;
import com.equipe1.aurora.infra.local.entity.LocalizacaoEntity;

/**
 * Base de dados local do Room (SQLite) do ecossistema Aurora[cite: 32].
 * Armazena localmente históricos de localização e eventos de emergência SOS.
 */
@Database(
        entities = {LocalizacaoEntity.class, AlertaSosEntity.class}, // Lista de tabelas associadas
        version = 2,                                                  // Versão incrementada para incluir a nova entidade
        exportSchema = false
)
public abstract class AppDataBase extends RoomDatabase {

    // =========================================================================
    // DECLARAÇÃO DOS DAOs
    // =========================================================================

    /** Fornece o DAO para operações de coordenadas de localização offline[cite: 32]. */
    public abstract LocalizacaoDao localizacaoDao();

    /** Fornece o DAO para operações de alertas SOS (App e Dispositivo)[cite: 28, 29]. */
    public abstract AlertaSosDao alertaSosDao();

    // =========================================================================
    // PADRÃO SINGLETON (GARANTE APENAS UMA INSTÂNCIA DA BASE DE DADOS ABERTA)
    // =========================================================================

    private static volatile AppDataBase INSTANCE;

    public static AppDataBase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDataBase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDataBase.class,
                                    "aurora_local_db"
                            )
                            // Recria o banco de dados caso ocorram alterações na versão da estrutura durante o desenvolvimento
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}