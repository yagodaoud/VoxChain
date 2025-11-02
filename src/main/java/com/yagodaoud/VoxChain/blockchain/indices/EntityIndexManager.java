package com.yagodaoud.VoxChain.blockchain.indices;

import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.modelo.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gerencia todos os índices para consulta rápida de entidades.
 * Reconstrói índices a partir da blockchain quando necessário.
 */
public class EntityIndexManager {
    private final Map<String, Administrador> admins;
    private final Map<String, Eleitor> eleitores;
    private final Map<String, Candidato> candidatos;
    private final Map<String, Eleicao> eleicoes;

    public EntityIndexManager() {
        this.admins = new ConcurrentHashMap<>();
        this.eleitores = new ConcurrentHashMap<>();
        this.candidatos = new ConcurrentHashMap<>();
        this.eleicoes = new ConcurrentHashMap<>();
    }

    public void atualizarComBloco(Bloco bloco) {
        if (bloco == null) return;

        for (Transacao t : bloco.getTransacoes()) {
            processarTransacao(t);
        }
    }

    public void reconstruirIndices(List<Bloco> blocos) {
        limpar();

        System.out.println("[ÍNDICES] Reconstruindo índices...");

        blocos.stream()
                .skip(1) // Pula gênesis
                .forEach(this::atualizarComBloco);

        System.out.println(String.format(
                "[ÍNDICES] Reconstruídos: %d admins, %d eleitores, %d candidatos, %d eleições",
                admins.size(), eleitores.size(), candidatos.size(), eleicoes.size()
        ));
    }

    private void processarTransacao(Transacao t) {
        if (t == null) return;

        switch (t.getTipo()) {
            case CADASTRO_ADMIN:
                Administrador admin = t.getPayloadAs(Administrador.class);
                if (admin != null) {
                    admins.put(admin.getId(), admin);
                }
                break;

            case CADASTRO_ELEITOR:
                Eleitor eleitor = t.getPayloadAs(Eleitor.class);
                if (eleitor != null) {
                    eleitores.put(eleitor.getTituloDeEleitorHash(), eleitor);
                }
                break;

            case CADASTRO_CANDIDATO:
                Candidato candidato = t.getPayloadAs(Candidato.class);
                if (candidato != null) {
                    candidatos.put(candidato.getNumero(), candidato);
                }
                break;

            case CRIACAO_ELEICAO:
            case INICIO_ELEICAO:
            case FIM_ELEICAO:
                Eleicao eleicao = t.getPayloadAs(Eleicao.class);
                if (eleicao != null) {
                    eleicoes.put(eleicao.getId(), eleicao);
                }
                break;

            default:
                break;
        }
    }

    public void limpar() {
        admins.clear();
        eleitores.clear();
        candidatos.clear();
        eleicoes.clear();
    }

    // ========== CONSULTAS ==========

    public Administrador buscarAdmin(String id) {
        return admins.get(id);
    }

    public Administrador buscarAdminPorCpfHash(String cpf) {
        return admins.values().stream()
                .filter(admin -> admin.getHashCpf().equals(cpf))
                .findFirst()
                .orElse(null);
    }

    public Administrador buscarAdminPorCpfHashESenhaHash(String cpfHash, String senhaHash) {
        return admins.values().stream()
                .filter(admin -> admin.getHashCpf().equals(cpfHash) && admin.getSenhaHash().equals(senhaHash))
                .findFirst()
                .orElse(null);
    }

    public Eleitor buscarEleitor(String cpfHash) {
        return eleitores.get(cpfHash);
    }

    public Eleitor buscarEleitorPorCpfHashESenhaHash(String cpfHash, String senhaHash) {
        return eleitores.values().stream()
                .filter(eleitor -> eleitor.getCpfHash().equals(cpfHash) && eleitor.getSenhaHash().equals(senhaHash))
                .findFirst()
                .orElse(null);
    }

    public Candidato buscarCandidato(String numero) {
        return candidatos.get(numero);
    }

    public Eleicao buscarEleicao(String id) {
        return eleicoes.get(id);
    }

    public List<Administrador> listarAdmins() {
        return new ArrayList<>(admins.values());
    }

    public List<Eleitor> listarEleitores() {
        return new ArrayList<>(eleitores.values());
    }

    public List<Candidato> listarCandidatos() {
        return new ArrayList<>(candidatos.values());
    }

    public List<Candidato> listarCandidatos(String eleicaoId) {
        return candidatos.values().stream()
                .filter(candidato -> candidato.getEleicaoId().equals(eleicaoId))
                .collect(Collectors.toList());
    }

    public List<Eleicao> listarEleicoes() {
        return new ArrayList<>(eleicoes.values());
    }

    public int getTotalAdmins() {
        return admins.size();
    }

    public int getTotalEleitores() {
        return eleitores.size();
    }

    public int getTotalCandidatos() {
        return candidatos.size();
    }

    public int getTotalEleicoes() {
        return eleicoes.size();
    }
}