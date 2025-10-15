package com.yagodaoud.VoxChain.modelo;

import com.yagodaoud.VoxChain.modelo.enums.JurisdicaoAdmin;
import com.yagodaoud.VoxChain.modelo.enums.NivelAcessoAdmin;

import java.io.Serializable;
import java.security.MessageDigest;

public class Administrador implements Serializable {

    private String id;
    private String nome;
    private String senhaHash;
    private NivelAcessoAdmin nivel;
    private JurisdicaoAdmin jurisdicao;
    private boolean ativo;


    public Administrador(String id, String nome, String senha, NivelAcessoAdmin nivel, JurisdicaoAdmin jurisdicao) {
        this.id = id;
        this.nome = nome;
        this.senhaHash = hashSenha(senha);
        this.nivel = nivel;
        this.jurisdicao = jurisdicao;
        this.ativo = true;
    }

    private String hashSenha(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(senha.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verificarSenha(String senha) {
        return hashSenha(senha).equals(senhaHash);
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public NivelAcessoAdmin getNivel() {
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
