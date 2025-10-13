package com.yagodaoud.VoxChain.modelo;

import com.yagodaoud.VoxChain.modelo.enums.CargoCandidato;

import java.util.UUID;

public class Candidato {
    private String id;
    private String numero;
    private String nome;
    private String partido;
    private CargoCandidato cargo;
    private String uf;
    private String foto;

    public Candidato(String numero, String nome, String partido, String foto, CargoCandidato cargo, String uf) {
        this.id = UUID.randomUUID().toString();
        this.numero = numero;
        this.nome = nome;
        this.partido = partido;
        this.cargo = cargo;
        this.uf = uf;
        this.foto = foto;
    }

    public String getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public String getNome() {
        return nome;
    }

    public String getPartido() {
        return partido;
    }

    public CargoCandidato getCargo() {
        return cargo;
    }

    public String getUf() {
        return uf;
    }

    public String getFoto() {
        return foto;
    }
}
