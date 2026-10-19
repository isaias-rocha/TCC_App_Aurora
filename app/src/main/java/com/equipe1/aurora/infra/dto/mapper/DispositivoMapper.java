package com.equipe1.aurora.infra.dto.mapper;

import com.equipe1.aurora.domain.model.DispositivoEmbarcado;
import com.equipe1.aurora.infra.dto.DispositivoDto;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 * AURORA GPS - MAPPER DE DISPOSITIVOS
 * =====================================================================
 * Classe responsável por converter os Objetos de Transferência de Dados (DTO)
 * recebidos da API REST em modelos do Domínio (DispositivoEmbarcado).
 * =====================================================================
 */
public class DispositivoMapper {

    // -----------------------------------------------------------------
    // BLOCO 1: CONVERSÃO DE OBJETO ÚNICO (DispositivoDto -> DispositivoEmbarcado)
    // -----------------------------------------------------------------
    /**
     * Converte uma instância individual de DispositivoDto para o modelo de domínio.
     * Trata valores nulos para evitar excepções de NullPointerException.
     */
    public static DispositivoEmbarcado paraDominio(DispositivoDto dto) {
        if (dto == null) {
            return null;
        }

        DispositivoEmbarcado dominio = new DispositivoEmbarcado();

        // Mapeamento das propriedades de identificação e localização
        dominio.setDeviceUid(dto.getDeviceUid());
        dominio.setNome(dto.getDispositivoNome() != null ? dto.getDispositivoNome() : "Aurora GPS");
        dominio.setLatitude(dto.getLatitude());
        dominio.setLongitude(dto.getLongitude());
        dominio.setAltitudeM(dto.getAltitudeM());
        dominio.setVelocidadeKmh(dto.getVelocidadeKmh());
        dominio.setSatelites(dto.getSatelites());

        // Mapeamento do estado de energia e alertas SOS
        dominio.setBateria(dto.getBateria() != null ? dto.getBateria() : 0);
        dominio.setSos(dto.isSos());
        dominio.setDataHoraGps(dto.getDataHoraGps() != null ? dto.getDataHoraGps() : "--:--");

        return dominio;
    }

    // -----------------------------------------------------------------
    // BLOCO 2: CONVERSÃO DE LISTAS (List<DispositivoDto> -> List<DispositivoEmbarcado>)
    // -----------------------------------------------------------------
    /**
     * Percorre uma lista de DTOs e converte cada elemento na classe de domínio.
     */
    public static List<DispositivoEmbarcado> paraListaDominio(List<DispositivoDto> dtoList) {
        List<DispositivoEmbarcado> lista = new ArrayList<>();

        if (dtoList != null) {
            for (DispositivoDto dto : dtoList) {
                lista.add(paraDominio(dto));
            }
        }
        return lista;
    }
}