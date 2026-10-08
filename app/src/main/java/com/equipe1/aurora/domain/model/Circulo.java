package com.equipe1.aurora.domain.model;

import java.util.ArrayList;
import java.util.List;

public class Circulo {
    public final String id;
    public final String nome;
    public final List<Membro> membros = new ArrayList<>();

    public Circulo(String id, String nome) {
        this.id = id;
        this.nome = nome;
    }
}