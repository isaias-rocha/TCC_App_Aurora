package com.equipe1.aurora.infra.storage;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gerenciador de nomes customizados de dispositivos via SharedPreferences.
 * Utiliza o Endereço MAC como chave única para suportar múltiplos rastreadores.
 */
public class DeviceStorageManager {

    private static final String PREF_NAME = "aurora_dispositivos_prefs";
    private static final String PREFIX_KEY = "nome_dispositivo";

    public static void salvarNomeCustomizado(Context context, String macAddress, String novoNome) {
        if (context == null || macAddress == null || novoNome == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREFIX_KEY + macAddress.toUpperCase(), novoNome.trim()).apply();
    }

    public static String obterNomeCustomizado(Context context, String macAddress, String nomePadrao) {
        if (context == null || macAddress == null) return nomePadrao;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREFIX_KEY + macAddress.toUpperCase(), nomePadrao);
    }
}