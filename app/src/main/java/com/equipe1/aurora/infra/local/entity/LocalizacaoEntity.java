package com.equipe1.aurora.infra.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entidade para armazenar coordenadas de GPS no Room localmente.
 */
@Entity(tableName = "localizacao")
public class LocalizacaoEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private double latitude;
    private double longitude;
    private long timestamp;
    private boolean sincronizado;

    // Construtor padrão que o Room usará obrigatoriamente
    public LocalizacaoEntity() {}

    // A anotação @Ignore avisa o Room para ignorar este construtor durante a instanciação da entidade
    @Ignore
    public LocalizacaoEntity(double latitude, double longitude, long timestamp, boolean sincronizado) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.sincronizado = sincronizado;
    }

    // Getters e Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSincronizado() { return sincronizado; }
    public void setSincronizado(boolean sincronizado) { this.sincronizado = sincronizado; }
}