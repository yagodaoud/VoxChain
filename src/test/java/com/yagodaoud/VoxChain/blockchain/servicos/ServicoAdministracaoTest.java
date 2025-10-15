package com.yagodaoud.VoxChain.blockchain.servicos;

import com.yagodaoud.VoxChain.blockchain.BlockchainGovernamental;
import com.yagodaoud.VoxChain.modelo.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Testes do Serviço de Administração")
public class ServicoAdministracaoTest {

    private ServicoAdministracao servico;
    private BlockchainGovernamental blockchain;
    private static final String SUPER_ADMIN = "TSE-SUPER-001";

    @BeforeEach
    void setUp() {
        blockchain = new BlockchainGovernamental(2, 5);
        blockchain.setModoTeste(true);
        servico = new ServicoAdministracao(blockchain);
    }

    // ============ TESTES DE INICIALIZAÇÃO ============

    @Test
    @DisplayName("Deve inicializar super admin na blockchain")
    void deveInicializarSuperAdmin() {
        assertThat(blockchain.buscarAdmin(SUPER_ADMIN)).isNotNull();
        assertThat(blockchain.buscarAdmin(SUPER_ADMIN).getNivel())
                .isEqualTo(NivelAcessoAdmin.SUPER_ADMIN);
    }

    // ============ TESTES DE PERMISSÕES ============

    @Test
    @DisplayName("Super admin deve ter permissão para tudo")
    void superAdminTemPermissaoParaTudo() {
        assertThat(servico.temPermissao(SUPER_ADMIN, TipoTransacao.CADASTRO_ADMIN)).isTrue();
        assertThat(servico.temPermissao(SUPER_ADMIN, TipoTransacao.CRIACAO_ELEICAO)).isTrue();
    }

    @Test
    @DisplayName("Admin TSE deve ter permissão para criar eleições")
    void adminTSETemPermissaoCriarEleicoes() {
        // Primeiro, criar um admin TSE
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "TSE-SP-001",
                "Admin SP",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.SP
        );

        assertThat(servico.temPermissao("TSE-SP-001", TipoTransacao.CRIACAO_ELEICAO)).isTrue();
    }

    @Test
    @DisplayName("Operador não deve ter permissão para criar admins")
    void operadorNaoTemPermissaoCriarAdmins() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "OP-001",
                "Operador 1",
                NivelAcessoAdmin.OPERADOR,
                JurisdicaoAdmin.SP
        );

        assertThat(servico.temPermissao("OP-001", TipoTransacao.CADASTRO_ADMIN)).isFalse();
    }

    @Test
    @DisplayName("Admin inativo não deve ter permissões")
    void adminInativoNaoTemPermissoes() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "TSE-RJ-001",
                "Admin RJ",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.RJ
        );

        servico.desativarAdmin(SUPER_ADMIN, "TSE-RJ-001");

        assertThat(servico.temPermissao("TSE-RJ-001", TipoTransacao.CRIACAO_ELEICAO)).isFalse();
    }

    // ============ TESTES DE CADASTRO ============

    @Test
    @DisplayName("Deve cadastrar novo admin TSE")
    void deveCadastrarNovoAdminTSE() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "TSE-MG-001",
                "Admin Minas Gerais",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.RJ
        );

        assertThat(blockchain.buscarAdmin("TSE-MG-001")).isNotNull();
        assertThat(blockchain.buscarAdmin("TSE-MG-001").getNivel())
                .isEqualTo(NivelAcessoAdmin.ADMIN_TSE);
    }

    @Test
    @DisplayName("Não deve permitir cadastro de admin duplicado")
    void naoDeveCadastrarAdminDuplicado() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "TSE-BA-001",
                "Admin Bahia",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.RJ
        );

        assertThatThrownBy(() ->
                servico.cadastrarNovoAdmin(
                        SUPER_ADMIN,
                        "TSE-BA-001",
                        "Outro Admin",
                        NivelAcessoAdmin.ADMIN_TSE,
                        JurisdicaoAdmin.SP
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
    }

    @Test
    @DisplayName("Deve lançar exceção se quem solicita não tem permissão")
    void deveLancarExcecaoSemPermissao() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "OP-002",
                "Operador",
                NivelAcessoAdmin.OPERADOR,
                JurisdicaoAdmin.SP
        );

        assertThatThrownBy(() ->
                servico.cadastrarNovoAdmin(
                        "OP-002",
                        "TSE-NOVO",
                        "Novo Admin",
                        NivelAcessoAdmin.ADMIN_TSE,
                        JurisdicaoAdmin.SP
                )
        ).isInstanceOf(SecurityException.class)
                .hasMessageContaining("permissão");
    }

    // ============ TESTES DE GERENCIAMENTO ============

    @Test
    @DisplayName("Deve desativar admin")
    void deveDesativarAdmin() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "TSE-RS-001",
                "Admin Rio Grande do Sul",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.RJ
        );

        servico.desativarAdmin(SUPER_ADMIN, "TSE-RS-001");

        assertThat(blockchain.buscarAdmin("TSE-RS-001").isAtivo()).isFalse();
    }

    @Test
    @DisplayName("Deve ativar admin")
    void deveAtivarAdmin() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "TSE-PR-001",
                "Admin Paraná",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.SP
        );

        servico.desativarAdmin(SUPER_ADMIN, "TSE-PR-001");
        servico.ativarAdmin(SUPER_ADMIN, "TSE-PR-001");

        assertThat(blockchain.buscarAdmin("TSE-PR-001").isAtivo()).isTrue();
    }

    // ============ TESTES DE ELEIÇÕES ============

    @Test
    @DisplayName("Admin TSE deve criar eleição")
    void adminTSEDeveCriarEleicao() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "TSE-RJ-002",
                "Admin RJ 2",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.RJ
        );

        long agora = System.currentTimeMillis();
        servico.criarEleicao(
                "TSE-RJ-002",
                "Eleição 2024",
                agora + 86400000,  // +1 dia
                agora + 172800000  // +2 dias
        );

        assertThat(blockchain.listarEleicoes()).isNotEmpty();
    }

    @Test
    @DisplayName("Operador não deve criar eleição")
    void operadorNaoDeveCriarEleicao() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "OP-003",
                "Operador 3",
                NivelAcessoAdmin.OPERADOR,
                JurisdicaoAdmin.SP
        );

        long agora = System.currentTimeMillis();
        assertThatThrownBy(() ->
                servico.criarEleicao(
                        "OP-003",
                        "Eleição 2024",
                        agora + 86400000,
                        agora + 172800000
                )
        ).isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("Não deve criar eleição com data fim antes de início")
    void naoDeveCriarEleicaoDataInvalida() {
        long agora = System.currentTimeMillis();

        assertThatThrownBy(() ->
                servico.criarEleicao(
                        SUPER_ADMIN,
                        "Eleição Inválida",
                        agora + 172800000,
                        agora + 86400000  // Fim antes do início
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data de fim");
    }
}