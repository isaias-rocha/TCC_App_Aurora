package com.equipe1.aurora.infra.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// DTO wrapper para encapsular a resposta completa do consultar.php
public class ConsultaResponseDto {

    @SerializedName("sucesso")
    private boolean sucesso;

    @SerializedName("dados")
    private List<DispositivoDto> dados;

    public boolean isSucesso() { return sucesso; }
    public List<DispositivoDto> getDados() { return dados; }
}