package com.equipe1.aurora.infra.storage;

import android.content.SharedPreferences;
import com.equipe1.aurora.domain.model.Endereco;

// Classe utilitária de persistência local via SharedPreferences (padrão infra/storage do projeto)
public class EnderecoPreferencesStorage {

    private static final String KEY_CEP = "endereco_cep";
    private static final String KEY_LOGRADOURO = "endereco_logradouro";
    private static final String KEY_BAIRRO = "endereco_bairro";
    private static final String KEY_CIDADE = "endereco_cidade";
    private static final String KEY_ESTADO = "endereco_estado";
    private static final String KEY_NUMERO = "endereco_numero";
    private static final String KEY_COMPLEMENTO = "endereco_complemento";
    private static final String KEY_APELIDO = "endereco_apelido";

    // Salva os dados do endereço cadastrado localmente no dispositivo
    public static void salvarEndereco(SharedPreferences preferences, Endereco endereco) {
        if (preferences == null || endereco == null) return;

        preferences.edit()
                .putString(KEY_CEP, endereco.getCep())
                .putString(KEY_LOGRADOURO, endereco.getLogradouro())
                .putString(KEY_BAIRRO, endereco.getBairro())
                .putString(KEY_CIDADE, endereco.getCidade())
                .putString(KEY_ESTADO, endereco.getEstado())
                .putString(KEY_NUMERO, endereco.getNumero())
                .putString(KEY_COMPLEMENTO, endereco.getComplemento())
                .putString(KEY_APELIDO, endereco.getApelido())
                .apply();
    }

    // Recupera o endereço salvo anteriormente no SharedPreferences
    public static Endereco obterEndereco(SharedPreferences preferences) {
        if (preferences == null) return null;

        return new Endereco(
                preferences.getString(KEY_CEP, ""),
                preferences.getString(KEY_LOGRADOURO, ""),
                preferences.getString(KEY_BAIRRO, ""),
                preferences.getString(KEY_CIDADE, ""),
                preferences.getString(KEY_ESTADO, ""),
                preferences.getString(KEY_NUMERO, ""),
                preferences.getString(KEY_COMPLEMENTO, ""),
                preferences.getString(KEY_APELIDO, "")
        );
    }
}