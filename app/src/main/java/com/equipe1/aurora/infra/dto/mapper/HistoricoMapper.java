package com.equipe1.aurora.infra.dto.mapper;

import com.equipe1.aurora.domain.model.HistoricoItem;
import com.equipe1.aurora.infra.dto.DispositivoDto;
import com.equipe1.aurora.infra.local.entity.AlertaSosEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Converte objetos de persistência local (Room) e de transporte remoto (DTO)
 * para o modelo de domínio HistoricoItem
 */
public class HistoricoMapper {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    /**
     * Converte uma entidade AlertaSosEntity (Room) para HistoricoItem (Domínio)
     */
    public static HistoricoItem paraDominio(AlertaSosEntity entity) {
        if (entity == null) return null;

        String titulo = "APP".equalsIgnoreCase(entity.getOrigem())
                ? "Alerta SOS (App)"
                : "Alerta SOS (Dispositivo Embarcado)";

        String descricao = "Status: " + entity.getStatus();

        return new HistoricoItem(
                String.valueOf(entity.getId()),
                HistoricoItem.TipoHistorico.ALERTA_SOS,
                titulo,
                descricao,
                new Date(entity.getDataHoraInicio()),
                entity.getOrigem(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.isSincronizado(),
                null
        );
    }

    /**
     * Converte um DispositivoDto retornado pelo consultar.php para HistoricoItem (Domínio)
     */
    public static HistoricoItem paraDominio(DispositivoDto dto) {
        if (dto == null) return null;

        Date dataHora;
        try {
            dataHora = dto.getDataHoraGps() != null ? dateFormat.parse(dto.getDataHoraGps()) : new Date();
        } catch (Exception e) {
            dataHora = new Date();
        }

        HistoricoItem.TipoHistorico tipo = dto.isSos()
                ? HistoricoItem.TipoHistorico.ALERTA_SOS
                : HistoricoItem.TipoHistorico.ROTA;

        String nomeDisp = dto.getDispositivoNome() != null ? dto.getDispositivoNome() : "Dispositivo GPS";
        String titulo = dto.isSos() ? "Alerta SOS - " + nomeDisp : "Registro de Ponto - " + nomeDisp;
        String descricao = "Velocidade: " + (dto.getVelocidadeKmh() != null ? dto.getVelocidadeKmh() : 0.0) + " km/h";

        return new HistoricoItem(
                String.valueOf(dto.getId()),
                tipo,
                titulo,
                descricao,
                dataHora,
                "DISPOSITIVO",
                dto.getLatitude(),
                dto.getLongitude(),
                true, // Dados do MySQL já estão sincronizados
                dto.getBateria()
        );
    }

    /**
     * Converte lista de entidades locais do Room em lista de domínio[cite: 119].
     */
    public static List<HistoricoItem> paraListaDominioDasEntidades(List<AlertaSosEntity> entities) {
        List<HistoricoItem> lista = new ArrayList<>();
        if (entities != null) {
            for (AlertaSosEntity entity : entities) {
                lista.add(paraDominio(entity));
            }
        }
        return lista;
    }

    /**
     * Converte lista de DTOs da API em lista de domínio
     */
    public static List<HistoricoItem> paraListaDominioDosDtos(List<DispositivoDto> dtos) {
        List<HistoricoItem> lista = new ArrayList<>();
        if (dtos != null) {
            for (DispositivoDto dto : dtos) {
                lista.add(paraDominio(dto));
            }
        }
        return lista;
    }
}