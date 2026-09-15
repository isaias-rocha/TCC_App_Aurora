package com.equipe1.aurora.domain.model;

import com.google.gson.annotations.SerializedName;

public class UsuarioDto {
    private String nome;
    private String email;
    private String senha;
    private String telefone;

    @SerializedName("firebase_uid")
    private String firebaseUid;

    public UsuarioDto(String nome, String email, String senha, String telefone, String firebaseUid) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.telefone = telefone;
        this.firebaseUid = firebaseUid;
    }

    // Getters e Setters (ou apenas getters se preferir)
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getTelefone() { return telefone; }
    public String getFirebaseUid() { return firebaseUid; }
}