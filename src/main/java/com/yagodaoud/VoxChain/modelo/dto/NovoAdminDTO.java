package com.yagodaoud.VoxChain.modelo.dto;

import com.yagodaoud.VoxChain.modelo.enums.JurisdicaoAdmin;
import com.yagodaoud.VoxChain.modelo.enums.NivelAcessoAdmin;

public class NovoAdminDTO {
    private String nome;
    private String senha;
    private NivelAcessoAdmin nivel;
    private JurisdicaoAdmin jurisdicao;

    // Getters
    public String getNome() { return nome; }
    public String getSenha() { return senha; }
    public NivelAcessoAdmin getNivel() { return nivel; }
    public JurisdicaoAdmin getJurisdicao() { return jurisdicao; }
}