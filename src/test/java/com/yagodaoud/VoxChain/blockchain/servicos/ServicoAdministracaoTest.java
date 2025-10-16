package com.yagodaoud.VoxChain.blockchain.servicos;

import com.yagodaoud.VoxChain.blockchain.BlockchainGovernamental;
import com.yagodaoud.VoxChain.modelo.Administrador;
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
        Administrador administrador = servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "Admin SP",
                "senha",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.SP
        );

        assertThat(servico.temPermissao(administrador.getId(), TipoTransacao.CRIACAO_ELEICAO)).isTrue();
    }

    @Test
    @DisplayName("Operador não deve ter permissão para criar admins")
    void operadorNaoTemPermissaoCriarAdmins() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "Operador 1",
                "senha",
                NivelAcessoAdmin.OPERADOR,
                JurisdicaoAdmin.SP
        );

        assertThat(servico.temPermissao("OP-001", TipoTransacao.CADASTRO_ADMIN)).isFalse();
    }

    @Test
    @DisplayName("Admin inativo não deve ter permissões")
    void adminInativoNaoTemPermissoes() {
        Administrador administrador = servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "Admin RJ",
                "senha",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.RJ
        );

        servico.desativarAdmin(SUPER_ADMIN, administrador.getId());

        assertThat(servico.temPermissao(administrador.getId(), TipoTransacao.CRIACAO_ELEICAO)).isFalse();
    }

    // ============ TESTES DE CADASTRO ============

    @Test
    @DisplayName("Deve cadastrar novo admin TSE")
    void deveCadastrarNovoAdminTSE() {
        Administrador administrador = servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "Admin Minas Gerais",
                "senha",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.RJ
        );

        assertThat(blockchain.buscarAdmin(administrador.getId())).isNotNull();
        assertThat(blockchain.buscarAdmin(administrador.getId()).getNivel())
                .isEqualTo(NivelAcessoAdmin.ADMIN_TSE);
    }

    @Test
    @DisplayName("Não deve permitir cadastro de admin duplicado")
    void naoDeveCadastrarAdminDuplicado() {
       servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "Admin RJ",
                "senha",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.RJ
        );

        assertThatThrownBy(() ->
                servico.cadastrarNovoAdmin(
                        SUPER_ADMIN,
                        "Admin RJ",
                        "senha",
                        NivelAcessoAdmin.ADMIN_TSE,
                        JurisdicaoAdmin.RJ
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
    }

    @Test
    @DisplayName("Deve lançar exceção se quem solicita não tem permissão")
    void deveLancarExcecaoSemPermissao() {
        servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "Operador",
                "senha",
                NivelAcessoAdmin.OPERADOR,
                JurisdicaoAdmin.SP
        );

        assertThatThrownBy(() ->
                servico.cadastrarNovoAdmin(
                        "TSE-NOVO",
                        "Novo Admin",
                        "senha",
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
        Administrador administrador = servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "Admin Rio Grande do Sul",
                "senha",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.RJ
        );

        servico.desativarAdmin(SUPER_ADMIN, administrador.getId());

        assertThat(blockchain.buscarAdmin(administrador.getId()).isAtivo()).isFalse();
    }

    @Test
    @DisplayName("Deve ativar admin")
    void deveAtivarAdmin() {
        Administrador administrador = servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "Admin Paraná",
                "senha",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.SP
        );

        servico.desativarAdmin(SUPER_ADMIN, administrador.getId());

        assertThat(blockchain.buscarAdmin(administrador.getId()).isAtivo()).isFalse();

        servico.ativarAdmin(SUPER_ADMIN, administrador.getId());

        assertThat(blockchain.buscarAdmin(administrador.getId()).isAtivo()).isTrue();
    }

    // ============ TESTES DE ELEIÇÕES ============

    @Test
    @DisplayName("Admin TSE deve criar eleição")
    void adminTSEDeveCriarEleicao() {
        Administrador administrador = servico.cadastrarNovoAdmin(
                SUPER_ADMIN,
                "Admin RJ 2",
                "senha",
                NivelAcessoAdmin.ADMIN_TSE,
                JurisdicaoAdmin.RJ
        );

        long agora = System.currentTimeMillis();
        servico.criarEleicao(
                administrador.getId(),
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
                "Operador 3",
                "senha",
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