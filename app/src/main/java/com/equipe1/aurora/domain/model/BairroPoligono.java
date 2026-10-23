package com.equipe1.aurora.domain.model;

import com.google.gson.annotations.SerializedName;
import org.osmdroid.util.GeoPoint;
import java.util.List;

public class BairroPoligono {

    @SerializedName("nome_bairro")
    private String nomeBairro;

    @SerializedName("nivel_risco")
    private String nivelRisco;

    @SerializedName("coordenadas_vertices")
    private List<GeoPoint> coordenadasVertices; // Mapeado automaticamente pelo Gson!

    public String getNomeBairro() {
        return nomeBairro;
    }

    public String getNivelRisco() {
        return nivelRisco;
    }

    public List<GeoPoint> getCoordenadasVertices() {
        return coordenadasVertices;
    }
}