package blockchain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import modelo.Administrador;
import modelo.Candidato;
import modelo.Eleicao;
import modelo.Eleitor;
import modelo.Transacao;
import modelo.enums.TipoTransacao;

class BlockchainGovernamental implements Serializable {
    private List<Bloco> cadeia;
    private List<Transacao> transacoesPendentes;
    private int dificuldade;
    private int transacoesMaximasPorBloco;

    // ÍNDICES PARA CONSULTA RÁPIDA (reconstruídos da blockchain)
    private Map<String, Administrador> admins;
    private Map<String, Eleitor> eleitores;
    private Map<String, Candidato> candidatos;
    private Map<String, Eleicao> eleicoes;

    // Controle de votos por eleição
    private Map<String, Set<String>> votosRegistrados; // idEleicao -> Set de títulos
    private Map<String, Map<String, Map<String, Integer>>> resultados; // idEleicao -> cargo -> candidato -> contagem

    public BlockchainGovernamental(int dificuldade, int transacoesMaximasPorBloco) {
        this.cadeia = new ArrayList<>();
        this.transacoesPendentes = new ArrayList<>();
        this.dificuldade = dificuldade;
        this.transacoesMaximasPorBloco = transacoesMaximasPorBloco;

        // Inicializar índices
        this.admins = new HashMap<>();
        this.eleitores = new HashMap<>();
        this.candidatos = new HashMap<>();
        this.eleicoes = new HashMap<>();
        this.votosRegistrados = new HashMap<>();
        this.resultados = new HashMap<>();

        criarBlocoGenesis();
        criarAdminInicial();
    }

    private void criarBlocoGenesis() {
        List<Transacao> transacoesGenesis = new ArrayList<>();
        Bloco genesis = new Bloco(0, transacoesGenesis, "0", "SYSTEM");
        genesis.minerarBloco(dificuldade);
        cadeia.add(genesis);
    }

    private void criarAdminInicial() {
        // Admin root do sistema
        Administrador root = new Administrador("ROOT", "Sistema TSE", "admin123",
                Administrador.NivelAcesso.SUPER_ADMIN);
        Transacao t = new Transacao(TipoTransacao.CADASTRO_ADMIN, root, "SYSTEM");
        transacoesPendentes.add(t);
        minerarTransacoesPendentes("SYSTEM");
    }
}