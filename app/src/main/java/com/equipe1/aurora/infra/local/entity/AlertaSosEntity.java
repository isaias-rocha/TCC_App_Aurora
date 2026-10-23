package com.equipe1.aurora.infra.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidade que representa a tabela de Alertas SOS na base de dados local (Room)
 * Permite guardar os registos de sos offline para posterior sincronização com o MySQL remoto
 */
@Entity(tableName = "alerta_sos_local")
public class AlertaSosEntity {

    // =========================================================================
    // ATRIBUTOS DA TABELA (CAMPOS DO BANCO DE DADOS)
    // =========================================================================

    /**
     * Chave primária gerada automaticamente pelo Room para identificação local do registo.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Carimbo de data/hora (Timestamp em milissegundos) do momento exato do disparo do SOS
     */
    private long dataHoraInicio;

    /**
     * Timestamp do encerramento do alerta. Permanece como 'null' enquanto o SOS estiver ativo
     */
    private Long dataHoraFim;

    /**
     * Coordenada geográfica (Latitude) capturada no momento da emergência
     */
    private double latitude;

    /**
     * Coordenada geográfica (Longitude) capturada no momento da emergência
     */
    private double longitude;

    /**
     * Origem do disparo do alerta:
     * - 'APP': Disparado pelo botão virtual no aplicativo móvel
     * - 'DISPOSITIVO': Disparado pelo botão físico do hardware embarcado via BLE
     */
    private String origem;

    /**
     * Estado atual do alerta: 'ATIVO', 'ENCERRADO' ou 'CANCELADO'[cite: 74].
     */
    private String status;

    /**
     * Identificador do dispositivo embarcado (ESP32) associado. Pode ser nulo se o disparo for via aplicativo[cite: 74].
     */
    private Integer dispositivoId;

    /**
     * Flag de controlo de rede:
     * - false: Registo pendente de envio para o servidor MySQL remoto[cite: 32].
     * - true: Registo já sincronizado com a API com sucesso.
     */
    private boolean sincronizado;

    // =========================================================================
    // CONSTRUTOR
    // =========================================================================

    public AlertaSosEntity(long dataHoraInicio, double latitude, double longitude,
                           String origem, String status, Integer dispositivoId, boolean sincronizado) {
        this.dataHoraInicio = dataHoraInicio;
        this.latitude = latitude;
        this.longitude = longitude;
        this.origem = origem;
        this.status = status;
        this.dispositivoId = dispositivoId;
        this.sincronizado = sincronizado;
    }

    // =========================================================================
    // MÉTODOS GETTERS E SETTERS
    // =========================================================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getDataHoraInicio() { return dataHoraInicio; }

    public Long getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(Long dataHoraFim) { this.dataHoraFim = dataHoraFim; }

    public double getLatitude() { return latitude; }

    public double getLongitude() { return longitude; }

    public String getOrigem() { return origem; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDispositivoId() { return dispositivoId; }

    public boolean isSincronizado() { return sincronizado; }
    public void setSincronizado(boolean sincronizado) { this.sincronizado = sincronizado; }
}
