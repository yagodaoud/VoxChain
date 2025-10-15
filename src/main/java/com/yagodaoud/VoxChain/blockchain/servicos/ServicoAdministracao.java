package com.yagodaoud.VoxChain.blockchain.servicos;

import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.modelo.*;
import com.yagodaoud.VoxChain.modelo.enums.*;
import com.yagodaoud.VoxChain.blockchain.BlockchainGovernamental;

import java.util.List;

public class ServicoAdministracao {
    private BlockchainGovernamental blockchain;
    private static final String SUPER_ADMIN_ID = "TSE-SUPER-001";

    public ServicoAdministracao(BlockchainGovernamental blockchain) {
        this.blockchain = blockchain;
        inicializarSuperAdmin();
    }

    // ==================== INICIALIZAÇÃO ====================

    private void inicializarSuperAdmin() {
        if (blockchain.buscarAdmin(SUPER_ADMIN_ID) == null) {
            long TIMESTAMP_SUPER_ADMIN = 1700000000000L;

            Administrador superAdmin = new Administrador(
                    SUPER_ADMIN_ID,
                    "Super Administrador TSE",
                    "senha-super-admin-temporaria",
                    NivelAcessoAdmin.SUPER_ADMIN,
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

    public boolean temPermissao(String adminId, TipoTransacao tipoTransacao) {
        Administrador admin = blockchain.buscarAdmin(adminId);

        if (admin == null || !admin.isAtivo()) {
            System.out.println("[SEGURANÇA] Admin não encontrado ou inativo: " + adminId);
            return false;
        }

        // Super admin pode tudo
        if (admin.getNivel() == NivelAcessoAdmin.SUPER_ADMIN) {
            return true;
        }

        // ADMIN_TSE pode criar eleições na sua jurisdição
        if (admin.getNivel() == NivelAcessoAdmin.ADMIN_TSE) {
            return tipoTransacao == TipoTransacao.CRIACAO_ELEICAO ||
                    tipoTransacao == TipoTransacao.INICIO_ELEICAO ||
                    tipoTransacao == TipoTransacao.FIM_ELEICAO;
        }

        // OPERADOR só pode consultar (não precisa de permissão para escrita)
        return false;
    }

    // ==================== CADASTRO DE ADMINS ====================

    public void cadastrarNovoAdmin(
            String solicitanteId,
            String novoAdminId,
            String nome,
            NivelAcessoAdmin nivel,
            JurisdicaoAdmin jurisdicao) {

        // 1. Valida permissão do solicitante
        if (!temPermissao(solicitanteId, TipoTransacao.CADASTRO_ADMIN)) {
            throw new SecurityException(
                    "Admin " + solicitanteId + " não tem permissão para criar novos admins"
            );
        }

        // 2. Valida se admin já existe
        if (blockchain.buscarAdmin(novoAdminId) != null) {
            throw new IllegalArgumentException(
                    "Admin com ID " + novoAdminId + " já existe"
            );
        }

        // 3. Valida jurisdição
        if (nivel == NivelAcessoAdmin.ADMIN_TSE && jurisdicao == null) {
            throw new IllegalArgumentException(
                    "ADMIN_TSE deve ter uma jurisdição definida"
            );
        }

        // 4. Cria novo admin
        Administrador novoAdmin = new Administrador(
                novoAdminId,
                nome,
                gerarSenhaTemporaria(),
                nivel,
                jurisdicao != null ? jurisdicao : JurisdicaoAdmin.NACIONAL
        );

        // 5. Cria e registra transação
        Transacao t = new Transacao(
                TipoTransacao.CADASTRO_ADMIN,
                novoAdmin,
                solicitanteId
        );

        blockchain.adicionarAoPool(t);

        System.out.println("[ADMIN] Novo admin cadastrado:");
        System.out.println("  ID: " + novoAdminId);
        System.out.println("  Nome: " + nome);
        System.out.println("  Nível: " + nivel);
        System.out.println("  Jurisdição: " + jurisdicao);
        System.out.println("  Criado por: " + solicitanteId);
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

    // ==================== CRIAÇÃO DE ELEIÇÕES ====================

    public void criarEleicao(
            String solicitanteId,
            String descricao,
            long dataInicio,
            long dataFim) {

        if (!temPermissao(solicitanteId, TipoTransacao.CRIACAO_ELEICAO)) {
            throw new SecurityException(
                    "Admin " + solicitanteId + " não tem permissão para criar eleições"
            );
        }

        if (dataFim <= dataInicio) {
            throw new IllegalArgumentException(
                    "Data de fim deve ser após data de início"
            );
        }

        Eleicao eleicao = new Eleicao(descricao, dataInicio, dataFim);

        Transacao t = new Transacao(
                TipoTransacao.CRIACAO_ELEICAO,
                eleicao,
                solicitanteId
        );

        blockchain.adicionarAoPool(t);

        System.out.println("[ELEIÇÃO] Nova eleição criada:");
        System.out.println("  ID: " + eleicao.getId());
        System.out.println("  Descrição: " + descricao);
        System.out.println("  Criada por: " + solicitanteId);
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
            System.out.println("  Nome: " + admin.getNome());
            System.out.println("  Nível: " + admin.getNivel());
            System.out.println("  Jurisdição: " + admin.getJurisdicao());
            System.out.println("  Ativo: " + admin.isAtivo());
            System.out.println();
        }
    }
}