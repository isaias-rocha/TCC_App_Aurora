package com.equipe1.aurora.domain.model;

// =========================================================================
// 1. IMPORTAÇÕES DE CLASSES NATIVAS
// =========================================================================
import java.time.LocalDateTime;

// =========================================================================
// 2. ENTIDADE DE NEGÓCIO DA CAMADA DE DOMÍNIO
// =========================================================================
// Objeto de domínio puro, livre de dependências externas ou anotações de framework.
public class Usuario {

    // =========================================================================
    // 3. ENUMERADOR DE TIPO DE USUÁRIO
    // =========================================================================
    public enum TipoUsuario {
        COMUM,
        PREMIUM
    }

    // =========================================================================
    // 4. ATRIBUTOS DO USUÁRIO
    // =========================================================================
    private Long id;
    private String firebaseUid;
    private String nome;
    private String email;
    private String senha;
    private String telefone;
    private String fotoUrl;
    private TipoUsuario tipoUsuario;
    private LocalDateTime excluidoEm;

    // =========================================================================
    // 5. CONSTRUTORES
    // =========================================================================
    public Usuario() {
        this.tipoUsuario = TipoUsuario.COMUM; // Valor padrão inicial
    }

    public Usuario(Long id, String firebaseUid, String nome, String email, String senha,
                   String telefone, String fotoUrl, TipoUsuario tipoUsuario, LocalDateTime excluidoEm) {
        this.id = id;
        this.firebaseUid = firebaseUid;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.telefone = telefone;
        this.fotoUrl = fotoUrl;
        this.tipoUsuario = (tipoUsuario != null) ? tipoUsuario : TipoUsuario.COMUM;
        this.excluidoEm = excluidoEm;
    }

    public Usuario(String nome, String email, String senha, String telefone, String firebaseUid) {
        this();
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.telefone = telefone;
        this.firebaseUid = firebaseUid;
    }

    // =========================================================================
    // 6. GETTERS E SETTERS
    // =========================================================================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public LocalDateTime getExcluidoEm() {
        return excluidoEm;
    }

    public void setExcluidoEm(LocalDateTime excluidoEm) {
        this.excluidoEm = excluidoEm;
    }
}