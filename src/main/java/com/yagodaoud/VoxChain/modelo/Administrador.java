package com.yagodaoud.VoxChain.modelo;

import com.yagodaoud.VoxChain.modelo.enums.JurisdicaoAdmin;
import com.yagodaoud.VoxChain.modelo.enums.NivelAcesso;
import com.yagodaoud.VoxChain.utils.SecurityUtils;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.UUID; // Importar UUID

public class Administrador implements Serializable {

    private String id;
    private String hashCpf;
    private String senhaHash;
    private NivelAcesso nivel;
    private JurisdicaoAdmin jurisdicao;
    private boolean ativo;

    public Administrador(String cpf, String senha, NivelAcesso nivel, JurisdicaoAdmin jurisdicao) {
        this.id = "ADM-" + UUID.randomUUID(); // Gera um ID único e legível
        this.hashCpf = Eleitor.hashCpf(cpf);
        this.senhaHash = SecurityUtils.hashSenhaSegura(senha);
        this.nivel = nivel;
        this.jurisdicao = jurisdicao;
        this.ativo = true;
    }

    public Administrador(String id, String cpf, String senha, NivelAcesso nivel, JurisdicaoAdmin jurisdicao) {
        this.id = id;
        this.hashCpf = Eleitor.hashCpf(cpf);
        this.senhaHash = SecurityUtils.hashSenhaSegura(senha);
        this.nivel = nivel;
        this.jurisdicao = jurisdicao;
        this.ativo = true;
    }

    public boolean verificarSenha(String senha) {
        return SecurityUtils.verificarSenha(senha, senhaHash);
    }

    public String getId() {
        return id;
    }

    public String getHashCpf() {
        return hashCpf;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public NivelAcesso getNivel() {
        return nivel;
    }

    public JurisdicaoAdmin getJurisdicao() {
        return jurisdicao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void desativar() { this.ativo = false; }
    public void ativar() { this.ativo = true; }
}