package com.equipe1.aurora.ui.contacts;

public class Contato {
    private Long id;
    private String nome;
    private String telefone;
    private String urlFoto;
    private boolean isSos; // Marca se é o contato de emergência

    // Construtores, Getters e Setters
    public Contato(Long id, String nome, String telefone, boolean isSos) {
        this.id = id;
        this.nome = nome;
        this.telefone = telefone;
        this.isSos = isSos;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public boolean isSos() { return isSos; }
    public void setSos(boolean sos) { isSos = sos; }

    public String getUrlFoto() {
        return urlFoto;
    }

    public void setUrlFoto(String urlFoto) {
        this.urlFoto = urlFoto;
    }
}


