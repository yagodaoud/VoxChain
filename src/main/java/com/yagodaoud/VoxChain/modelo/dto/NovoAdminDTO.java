package com.yagodaoud.VoxChain.modelo.dto;

import com.yagodaoud.VoxChain.modelo.enums.JurisdicaoAdmin;
import com.yagodaoud.VoxChain.modelo.enums.NivelAcesso;

public class NovoAdminDTO {
    private String nome;
    private String senha;
    private NivelAcesso nivel;
    private JurisdicaoAdmin jurisdicao;

    // Getters
    public String getNome() { return nome; }
    public String getSenha() { return senha; }
    public NivelAcesso getNivel() { return nivel; }
    public JurisdicaoAdmin getJurisdicao() { return jurisdicao; }
}