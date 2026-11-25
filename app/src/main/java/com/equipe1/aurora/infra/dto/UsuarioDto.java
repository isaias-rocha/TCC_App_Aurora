package com.equipe1.aurora.infra.dto;

// =========================================================================
// 1. IMPORTAÇÕES DE BIBLIOTECAS EXTERNAS
// =========================================================================
import com.google.gson.annotations.SerializedName;

// =========================================================================
// 2. DATA TRANSFER OBJECT (DTO) DE USUÁRIO
// =========================================================================
// Objeto utilizado exclusivamente no transporte de dados via API REST (Retrofit/Gson).
public class UsuarioDto {

    // =========================================================================
    // 3. ATRIBUTOS E MAPEAMENTOS JSON DE SERIALIZAÇÃO
    // =========================================================================
    @SerializedName(value = "id_usuario", alternate = {"id", "idUsuario"})
    private Long id;

    @SerializedName("firebase_uid")
    private String firebaseUid;

    @SerializedName("nome")
    private String nome;

    @SerializedName("email")
    private String email;

    @SerializedName("senha")
    private String senha;

    @SerializedName("telefone")
    private String telefone;

    @SerializedName("foto_url")
    private String fotoUrl;

    @SerializedName("tipo_usuario")
    private String tipoUsuario;

    @SerializedName("excluido_em")
    private String excluidoEm;

    // =========================================================================
    // 4. CONSTRUTORES
    // =========================================================================
    public UsuarioDto() {
    }

    public UsuarioDto(String nome, String email, String senha, String telefone, String firebaseUid) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.telefone = telefone;
        this.firebaseUid = firebaseUid;
    }

    // =========================================================================
    // 5. GETTERS E SETTERS
    // =========================================================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public String getExcluidoEm() { return excluidoEm; }
    public void setExcluidoEm(String excluidoEm) { this.excluidoEm = excluidoEm; }
}