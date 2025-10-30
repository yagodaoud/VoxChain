package com.yagodaoud.VoxChain.blockchain.servicos;

import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.modelo.*;
import com.yagodaoud.VoxChain.modelo.enums.*;
import com.yagodaoud.VoxChain.blockchain.BlockchainGovernamental;

import java.util.List;

public class ServicoAdministracao {
    private BlockchainGovernamental blockchain;
    private static final String SUPER_ADMIN_ID = "TSE-SUPER-001";
    public static ServicoAdministracao instance;

    public ServicoAdministracao(BlockchainGovernamental blockchain) {
        this.blockchain = blockchain;
        inicializarSuperAdmin();
    }

    public static ServicoAdministracao getInstance(BlockchainGovernamental blockchain) {
        if (instance == null) {
            instance = new ServicoAdministracao(blockchain);
        }
        return instance;
    }

    // ==================== INICIALIZAÇÃO ====================
    private void inicializarSuperAdmin() {
        if (blockchain.buscarAdmin(SUPER_ADMIN_ID) == null) {
            long TIMESTAMP_SUPER_ADMIN = 1700000000000L;

            Administrador superAdmin = new Administrador(
                    SUPER_ADMIN_ID,
                    "11111111111",
                    "superadmin",
                    NivelAcesso.SUPER_ADMIN,
                    JurisdicaoAdmin.NACIONAL
            );

            Transacao t = new Transacao(
                    TipoTransacao.CADASTRO_ADMIN,
                    superAdmin,
                    "SYSTEM",
                    TIMESTAMP_SUPER_ADMIN
            );

            Bloco blocoAdmin = new Bloco(1, List.of(t), blockchain.getBloco(0).getHash(), "SYSTEM", 1700000000000L);
            blocoAdmin.minerarBloco(blockchain.getDificuldade());
            blockchain.adicionarBloco(blocoAdmin);

            System.out.println("✓ Super Admin inicializado: " + SUPER_ADMIN_ID);
        }
    }

    // ==================== VALIDAÇÕES ====================
    public boolean temPermissao(String cpfHash, TipoTransacao tipoTransacao) {
        Administrador admin = blockchain.buscarAdminPorCpfHash(cpfHash);

        if (admin == null || !admin.isAtivo()) {
            System.out.println("[SEGURANÇA] Admin não encontrado ou inativo: " + cpfHash);
            return false;
        }

        if (admin.getNivel() == NivelAcesso.SUPER_ADMIN) {
            return true;
        }

        if (admin.getNivel() == NivelAcesso.ADMIN_TSE) {
            return tipoTransacao == TipoTransacao.CRIACAO_ELEICAO ||
                    tipoTransacao == TipoTransacao.INICIO_ELEICAO ||
                    tipoTransacao == TipoTransacao.FIM_ELEICAO;
        }

        return false;
    }

    // ==================== CADASTRO DE ADMINS ====================
    public Administrador cadastrarNovoAdmin(
            String solicitanteId,
            String cpf,
            String senha,
            NivelAcesso nivel,
            JurisdicaoAdmin jurisdicao) {

        if (!temPermissao(solicitanteId, TipoTransacao.CADASTRO_ADMIN)) {
            throw new SecurityException(
                    "Admin " + solicitanteId + " não tem permissão para criar novos admins"
            );
        }

        if (blockchain.buscarAdminPorCpfHash(cpf) != null) {
            throw new IllegalArgumentException(
                    "Admin com Cpf " + cpf + " já existe"
            );
        }

        if (nivel == NivelAcesso.ADMIN_TSE && jurisdicao == null) {
            throw new IllegalArgumentException(
                    "ADMIN_TSE deve ter uma jurisdição definida"
            );
        }

        Administrador novoAdmin = new Administrador(
                cpf,
                senha,
                nivel,
                jurisdicao != null ? jurisdicao : JurisdicaoAdmin.NACIONAL
        );

        Transacao t = new Transacao(
                TipoTransacao.CADASTRO_ADMIN,
                novoAdmin,
                solicitanteId
        );

        blockchain.adicionarAoPool(t);

        System.out.println("[ADMIN] Novo admin cadastrado com ID: " + novoAdmin.getId());

        return novoAdmin;
    }

    // ==================== GERENCIAMENTO DE ADMINS ====================
    public void desativarAdmin(String solicitanteId, String adminId) {
        if (!temPermissao(solicitanteId, TipoTransacao.CADASTRO_ADMIN)) {
            throw new SecurityException(
                    "Admin " + solicitanteId + " não tem permissão"
            );
        }

        Administrador admin = blockchain.buscarAdmin(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("Admin não encontrado: " + adminId);
        }

        admin.desativar();
        System.out.println("[ADMIN] Admin desativado: " + adminId);
    }

    public void ativarAdmin(String solicitanteId, String adminId) {
        if (!temPermissao(solicitanteId, TipoTransacao.CADASTRO_ADMIN)) {
            throw new SecurityException(
                    "Admin " + solicitanteId + " não tem permissão"
            );
        }

        Administrador admin = blockchain.buscarAdmin(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("Admin não encontrado: " + adminId);
        }

        admin.ativar();
        System.out.println("[ADMIN] Admin ativado: " + adminId);
    }

    // ==================== UTILITÁRIOS ====================

    private String gerarSenhaTemporaria() {
        // Gera senha aleatória de 12 caracteres
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            senha.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return senha.toString();
    }

    public void exibirRelatorioAdmins() {
        System.out.println("\n========== RELATÓRIO DE ADMINS ==========");
        for (Administrador admin : blockchain.listarAdmins()) {
            System.out.println("ID: " + admin.getId());
            System.out.println("  Nome: " + admin.getHashCpf());
            System.out.println("  Nível: " + admin.getNivel());
            System.out.println("  Jurisdição: " + admin.getJurisdicao());
            System.out.println("  Ativo: " + admin.isAtivo());
            System.out.println();
        }
    }
}