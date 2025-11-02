package com.yagodaoud.VoxChain.modelo;

import com.yagodaoud.VoxChain.utils.SecurityUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Eleitor {
    private String cpfHash;
    private String senhaHash;
    private int zona;
    private int secao;

    private static final String salt = "ELeicao2025";

    public Eleitor(String cpf, String senha, int zona, int secao) {
        this.cpfHash = hashCpf(cpf);
        this.senhaHash = hashSenha(senha);
        this.zona = zona;
        this.secao = secao;
    }

    public static String hashCpf(String cpf) {
        return SecurityUtils.hash(cpf, salt);
    }

    public static String hashSenha(String senha) {
        return SecurityUtils.hash(senha, salt);
    }

    public String getCpfHash() {
        return cpfHash;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getTituloDeEleitorHash() {
        return cpfHash;
    }

    public int getZona() {
        return zona;
    }

    public int getSecao() {
        return secao;
    }
}
