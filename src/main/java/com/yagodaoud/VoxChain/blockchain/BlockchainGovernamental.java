package com.yagodaoud.VoxChain.blockchain;

import com.yagodaoud.VoxChain.modelo.*;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;

import java.io.Serializable;
import java.util.*;

public class BlockchainGovernamental implements Serializable {
    private static final long serialVersionUID = 1L;
    private static BlockchainGovernamental instance;

    private List<Bloco> cadeia;
    private List<Transacao> transacoesPendentes;
    private int dificuldade;
    private int transacoesMaximasPorBloco;
    private boolean modoTeste = false;

    // ÍNDICES PARA CONSULTA RÁPIDA (reconstruídos da blockchain)
    private Map<String, Administrador> admins;
    private Map<String, Eleitor> eleitores;
    private Map<String, Candidato> candidatos;
    private Map<String, Eleicao> eleicoes;

    // Controle de votos por eleição
    private Map<String, Set<String>> votosRegistrados; // idEleicao -> Set de "titulo-cargo"
    private Map<String, Map<String, Map<String, Integer>>> resultados; // idEleicao -> cargo -> candidato -> contagem

    public BlockchainGovernamental() {
        this(2, 5); // Valores padrão
    }

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
    }

    // ==================== BLOCO GÊNESIS ====================

    private void criarBlocoGenesis() {
        List<Transacao> transacoesGenesis = new ArrayList<>();
        Bloco genesis = new Bloco(0, transacoesGenesis, "0", "SYSTEM", 1700000000000L);
        genesis.minerarBloco(dificuldade);
        cadeia.add(genesis);
    }

    // ==================== GERENCIAMENTO DE TRANSAÇÕES ====================

    public synchronized boolean adicionarAoPool(Transacao t) {
        if (t == null) return false;

        // Verifica se já existe
        if (transacaoExiste(t)) {
            return false;
        }

        transacoesPendentes.add(t);

        if (modoTeste && temTransacoesPendentes()) {
            minerarBlocoImediato();
        }

        return true;
    }

    public synchronized boolean transacaoExiste(Transacao t) {
        if (t == null || t.getId() == null) {
            System.out.println("[DEBUG-EXISTE] Transação nula ou sem ID!");
            return false;
        }

        String idTransacao = t.getId();
        System.out.println("[DEBUG-EXISTE] Verificando se existe: " + idTransacao);
        System.out.println("[DEBUG-EXISTE]   Pool size: " + transacoesPendentes.size());

        // Verifica em TODOS os blocos
        for (Bloco bloco : cadeia) {
            for (Transacao tx : bloco.getTransacoes()) {
                if (tx != null && tx.getId() != null) {
                    System.out.println("[DEBUG-EXISTE]   Bloco [" + bloco.getIndice() + "]: " + tx.getId());
                    if (idTransacao.equals(tx.getId())) {
                        System.out.println("[DEBUG-EXISTE] ✓ ENCONTRADA no bloco " + bloco.getIndice());
                        return true;
                    }
                }
            }
        }

        // Também verifica NO POOL
        for (Transacao tx : transacoesPendentes) {
            if (tx != null && tx.getId() != null) {
                System.out.println("[DEBUG-EXISTE]   Pool: " + tx.getId());
                if (idTransacao.equals(tx.getId())) {
                    System.out.println("[DEBUG-EXISTE] ✓ ENCONTRADA no pool");
                    return true;
                }
            }
        }

        System.out.println("[DEBUG-EXISTE] ✗ NÃO ENCONTRADA");
        return false;
    }

    public synchronized int getPoolSize() {
        return transacoesPendentes.size();
    }

    public synchronized boolean temTransacoesPendentes() {
        return !transacoesPendentes.isEmpty();
    }

    // ==================== CRIAÇÃO E MINERAÇÃO DE BLOCOS ====================

    public synchronized Bloco criarBlocoCandidato(String mineradoPor, Long timestampFixo) {
        if (transacoesPendentes.isEmpty()) {
            return null;
        }

        List<Transacao> transacoesBloco = new ArrayList<>();

        // Pega até o máximo de transações
        for (int i = 0; i < Math.min(transacoesMaximasPorBloco, transacoesPendentes.size()); i++) {
            transacoesBloco.add(transacoesPendentes.get(i));
        }

        Bloco novoBloco = new Bloco(
                cadeia.size(),
                transacoesBloco,
                obterUltimoBloco().getHash(),
                mineradoPor,
                timestampFixo
        );

        return novoBloco;
    }

    public synchronized void adicionarBloco(Bloco bloco) {
        if (bloco == null) return;

        cadeia.add(bloco);
        atualizarIndices(bloco);
    }

    public synchronized void limparTransacoesProcessadas(Bloco bloco) {
        if (bloco == null) return;

        for (Transacao t : bloco.getTransacoes()) {
            transacoesPendentes.remove(t);
        }
    }

    private synchronized void minerarBlocoImediato() {
        Bloco bloco = criarBlocoCandidato("TEST-NODE", null);
        if (bloco != null) {
            bloco.minerarBloco(dificuldade);
            if (validarBloco(bloco)) {
                adicionarBloco(bloco);
                limparTransacoesProcessadas(bloco);
            }
        }
    }

    // ==================== VALIDAÇÃO ====================

    public synchronized boolean validarBloco(Bloco bloco) {
        if (bloco == null) {
            System.err.println("Bloco nulo");
            return false;
        }

        // Valida hash
        if (!bloco.getHash().equals(bloco.calcularHash())) {
            System.err.println("Hash do bloco inválido");
            return false;
        }

        // Valida encadeamento
        Bloco ultimoBloco = obterUltimoBloco();
        if (!bloco.getHashAnterior().equals(ultimoBloco.getHash())) {
            System.err.println("Hash anterior não corresponde");
            return false;
        }

        // Valida índice
        if (bloco.getIndice() != cadeia.size()) {
            System.err.println("Índice do bloco inválido. Esperado: " + cadeia.size() +
                    ", Recebido: " + bloco.getIndice());
            return false;
        }

        // Valida proof of work (começa com zeros)
        String alvo = new String(new char[dificuldade]).replace('\0', '0');
        if (!bloco.getHash().substring(0, dificuldade).equals(alvo)) {
            System.err.println("Proof of Work inválido");
            return false;
        }

        return true;
    }

    public synchronized boolean validarCadeia() {
        for (int i = 1; i < cadeia.size(); i++) {
            Bloco blocoAtual = cadeia.get(i);
            Bloco blocoAnterior = cadeia.get(i - 1);

            if (!blocoAtual.getHash().equals(blocoAtual.calcularHash())) {
                System.out.println("Hash inválido no bloco " + i);
                return false;
            }

            if (!blocoAtual.getHashAnterior().equals(blocoAnterior.getHash())) {
                System.out.println("Encadeamento inválido no bloco " + i);
                return false;
            }
        }
        return true;
    }

    // ==================== SINCRONIZAÇÃO ====================

    public synchronized List<Bloco> getBlocos() {
        return new ArrayList<>(cadeia);
    }

    public synchronized void substituir(List<Bloco> novasCadeias) {
        if (novasCadeias == null || novasCadeias.isEmpty()) {
            System.err.println("Cadeia remota inválida (nula ou vazia)");
            return;
        }

        // Valida a nova cadeia SEM criar uma blockchain temporária
        if (!validarCadeiaRemota(novasCadeias)) {
            System.err.println("Cadeia remota inválida, rejeitando");
            return;
        }

        // Se chegou aqui, a cadeia remota é válida
        this.cadeia = new ArrayList<>(novasCadeias);
        this.transacoesPendentes.clear();
        reconstruirIndices();

        System.out.println("Blockchain substituída com sucesso. Novo tamanho: " + cadeia.size());
    }

    private synchronized boolean validarCadeiaRemota(List<Bloco> cadeiRemota) {
        if (cadeiRemota == null || cadeiRemota.isEmpty()) {
            return false;
        }

        // Valida o bloco gênesis (índice 0)
        Bloco blocoGenesis = cadeiRemota.get(0);
        if (blocoGenesis.getIndice() != 0) {
            System.err.println("Primeiro bloco não é gênesis");
            return false;
        }

        if (!blocoGenesis.getHashAnterior().equals("0")) {
            System.err.println("Hash anterior do gênesis não é '0'");
            return false;
        }

        // Valida encadeamento e hashes de todos os blocos
        for (int i = 0; i < cadeiRemota.size(); i++) {
            Bloco blocoAtual = cadeiRemota.get(i);

            // Verifica índice
            if (blocoAtual.getIndice() != i) {
                System.err.println("Índice inválido no bloco " + i);
                return false;
            }

            // Verifica hash
            if (!blocoAtual.getHash().equals(blocoAtual.calcularHash())) {
                System.err.println("Hash inválido no bloco " + i);
                return false;
            }

            // Verifica encadeamento (a partir do segundo bloco)
            if (i > 0) {
                Bloco blocoAnterior = cadeiRemota.get(i - 1);
                if (!blocoAtual.getHashAnterior().equals(blocoAnterior.getHash())) {
                    System.err.println("Hash anterior não corresponde no bloco " + i);
                    return false;
                }
            }

            // Verifica proof of work
            String alvo = "0".repeat(Math.max(0, dificuldade));
            if (!blocoAtual.getHash().startsWith(alvo)) {
                System.err.println("Proof of Work inválido no bloco " + i);
                return false;
            }
        }

        return true;
    }

    // ==================== ATUALIZAÇÃO DE ÍNDICES ====================

    public synchronized void atualizarIndices(Bloco bloco) {
        for (Transacao t : bloco.getTransacoes()) {
            if (t == null) continue;

            switch (t.getTipo()) {
                case CADASTRO_ADMIN:
                    // ★ Use getPayloadAs() para desserializar
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

                case VOTO:
                    Voto voto = t.getPayloadAs(Voto.class);
                    // Processamento de voto se necessário
                    break;

                default:
                    break;
            }
        }
    }

    public synchronized void reconstruirIndices() {
        admins.clear();
        eleitores.clear();
        candidatos.clear();
        eleicoes.clear();
        votosRegistrados.clear();
        resultados.clear();

        System.out.println("Reconstruindo índices...");

        for (int i = 1; i < cadeia.size(); i++) {
            atualizarIndices(cadeia.get(i));
        }

        System.out.println("Índices reconstruídos: " +
                admins.size() + " admins, " +
                eleitores.size() + " eleitores, " +
                candidatos.size() + " candidatos, " +
                eleicoes.size() + " eleições");
    }

    // ==================== GETTERS ====================

    public synchronized int getTamanho() {
        return cadeia.size();
    }

    public synchronized Bloco obterUltimoBloco() {
        return cadeia.get(cadeia.size() - 1);
    }

    public int getDificuldade() {
        return dificuldade;
    }

    public synchronized int getTotalTransacoes() {
        int total = 0;
        for (int i = 1; i < cadeia.size(); i++) {
            total += cadeia.get(i).getTransacoes().size();
        }
        return total;
    }

    // ==================== CONSULTAS AOS ÍNDICES ====================

    public synchronized Administrador buscarAdmin(String id) {
        return admins.get(id);
    }

    public synchronized Eleitor buscarEleitor(String titulo) {
        return eleitores.get(titulo);
    }

    public synchronized Candidato buscarCandidato(String numero) {
        return candidatos.get(numero);
    }

    public synchronized Eleicao buscarEleicao(String id) {
        return eleicoes.get(id);
    }

    public synchronized List<Administrador> listarAdmins() {
        return new ArrayList<>(admins.values());
    }

    public synchronized List<Eleitor> listarEleitores() {
        return new ArrayList<>(eleitores.values());
    }

    public synchronized List<Candidato> listarCandidatos() {
        return new ArrayList<>(candidatos.values());
    }

    public synchronized List<Eleicao> listarEleicoes() {
        return new ArrayList<>(eleicoes.values());
    }

    public synchronized Bloco getBloco(int index) {
        return cadeia.get(index);
    }

    public synchronized Voto buscarVotoPorHash(String hash) {
        for (Bloco bloco : cadeia) {
            for (Transacao t : bloco.getTransacoes()) {
                if (t.getTipo() == TipoTransacao.VOTO) {
                    Voto voto = t.getPayloadAs(Voto.class);
                    if (voto != null && voto.getIdEleitorHash().equals(hash)) {
                        return voto;
                    }
                }
            }
        }
        return null;
    }

    public void setModoTeste(boolean modoTeste) {
        this.modoTeste = modoTeste;
    }


    public static String gerarIdUnico(String idAdmin, TipoTransacao tipo, Object dados, long timestamp) {
        String dadosString = dados == null ? "" : dados.toString();
        return idAdmin + tipo + dadosString + timestamp;
    }

    public static BlockchainGovernamental getInstance(int dificuldade, int transacoesMaximasPorBloco) {
        if (instance == null) {
            return new BlockchainGovernamental(dificuldade, transacoesMaximasPorBloco);
        }

        return instance;
    }

    public static BlockchainGovernamental getInstance() {
        if (instance == null) {
            return new BlockchainGovernamental();
        }

        return instance;
    }

    // ==================== STATUS ====================

    public String getStatus() {
        return "Tamanho: " + cadeia.size() +
                " | Pool: " + transacoesPendentes.size() +
                " | Total Transações: " + getTotalTransacoes();
    }

    @Override
    public String toString() {
        return "BlockchainGovernamental{" +
                "tamanho=" + cadeia.size() +
                ", poolSize=" + transacoesPendentes.size() +
                ", dificuldade=" + dificuldade +
                '}';
    }
}