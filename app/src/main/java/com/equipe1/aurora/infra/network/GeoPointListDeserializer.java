package com.equipe1.aurora.infra.network;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.osmdroid.util.GeoPoint;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GeoPointListDeserializer implements JsonDeserializer<List<GeoPoint>> {

    @Override
    public List<GeoPoint> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<GeoPoint> pontos = new ArrayList<>();

        if (json == null || json.isJsonNull()) {
            return pontos;
        }

        String rawString = json.getAsString();
        if (rawString == null || rawString.trim().isEmpty()) {
            return pontos;
        }

        String[] pares = rawString.split(";");
        for (String par : pares) {
            String[] latLon = par.split(",");
            if (latLon.length == 2) {
                try {
                    double lat = Double.parseDouble(latLon[0].trim());
                    double lon = Double.parseDouble(latLon[1].trim());
                    pontos.add(new GeoPoint(lat, lon));
                } catch (NumberFormatException ignored) {
                    // Ignora pares com formato inválido sem travar a aplicação
                }
            }
        }

        return pontos;
    }
}