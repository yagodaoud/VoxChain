package blockchain;

import modelo.Transacao;
import rede.MensagemP2P;
import rede.Peer;
import rede.TipoMensagem;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class No {
    private String id;
    private String enderecoIP;
    private int porta;
    private BlockchainGovernamental blockchain;
    private List<Peer> peers;
    private ServerSocket servidor;
    private volatile boolean rodando = false;
    private Thread threadServidorAceitar;
    private Minerador minerador;

    public No(String id, String ip, int porta) {
        this.id = id;
        this.enderecoIP = ip;
        this.porta = porta;
        this.blockchain = new BlockchainGovernamental(2, 5);
        this.peers = new CopyOnWriteArrayList<>();
    }

    // ============ INICIALIZAÇÃO ============

    public void iniciar() {
        rodando = true;

        // Inicia servidor
        try {
            servidor = new ServerSocket(porta);
            System.out.println("[" + id + "] Servidor iniciado em " + enderecoIP + ":" + porta);
        } catch (IOException e) {
            System.err.println("[" + id + "] Erro ao criar servidor: " + e.getMessage());
            return;
        }

        // Thread para aceitar conexões
        threadServidorAceitar = new Thread(this::aceitarConexoes);
        threadServidorAceitar.setName("Servidor-" + id);
        threadServidorAceitar.start();

        // Inicia minerador
        minerador = new Minerador(this);
        Thread threadMinerador = new Thread(minerador);
        threadMinerador.setName("Minerador-" + id);
        threadMinerador.start();
    }

    public void parar() {
        rodando = false;
        minerador.parar();
        for (Peer p : peers) {
            p.desconectar();
        }
        try {
            if (servidor != null) servidor.close();
        } catch (IOException e) {
            // ignored
        }
    }

    // ============ CONEXÃO COM PEERS ============

    public void conectarPeer(String ipRemoto, int portaRemota, String idRemoto) {
        new Thread(() -> {
            try {
                System.out.println("[" + id + "] Tentando conectar em " + idRemoto);
                Socket socket = new Socket(ipRemoto, portaRemota);
                Peer peer = new Peer(idRemoto, socket, this);
                peers.add(peer);

                new Thread(peer).start();
                System.out.println("[" + id + "] ✓ Conectado a " + idRemoto);

                // Requisita blockchain do peer
                peer.enviar(new MensagemP2P(TipoMensagem.REQUISITAR_BLOCKCHAIN, null, this.id));

            } catch (IOException e) {
                System.err.println("[" + id + "] Erro ao conectar em " + idRemoto + ": " + e.getMessage());
            }
        }).start();
    }

    private void aceitarConexoes() {
        while (rodando) {
            try {
                Socket socketCliente = servidor.accept();
                System.out.println("[" + id + "] Conexão recebida");

                // Cria peer para essa conexão
                Peer peer = new Peer("Remoto-" + System.nanoTime(), socketCliente, this);
                peers.add(peer);

                new Thread(peer).start();

            } catch (IOException e) {
                if (rodando) {
                    System.err.println("[" + id + "] Erro ao aceitar conexão: " + e.getMessage());
                }
            }
        }
    }

    // ============ BROADCAST ============

    public void broadcastTransacao(Transacao t) {
        MensagemP2P msg = new MensagemP2P(TipoMensagem.NOVA_TRANSACAO, t, this.id);
        for (Peer peer : peers) {
            if (peer.isConectado()) {
                peer.enviar(msg);
            }
        }
    }

    public void rebroadcastTransacao(Transacao t, String peerOrigem) {
        MensagemP2P msg = new MensagemP2P(TipoMensagem.NOVA_TRANSACAO, t, this.id);
        for (Peer peer : peers) {
            if (peer.isConectado() && !peer.getId().equals(peerOrigem)) {
                peer.enviar(msg);
            }
        }
    }

    public void broadcastBloco(Bloco b) {
        MensagemP2P msg = new MensagemP2P(TipoMensagem.NOVO_BLOCO, b, this.id);
        for (Peer peer : peers) {
            if (peer.isConectado()) {
                peer.enviar(msg);
            }
        }
    }

    // ============ PROCESSAMENTO DE BLOCOS ============

    public synchronized void processarNovoBloco(Bloco blocoRecebido, String peerOrigem) {
        if (blocoRecebido == null) {
            System.out.println("[" + id + "] ✗ Bloco nulo recebido");
            return;
        }

        int meuTamanho = blockchain.getTamanho();
        int blocoIndice = blocoRecebido.getIndice();

        System.out.println("[" + id + "] Recebendo bloco " + blocoIndice +
                " (meu tamanho: " + meuTamanho + ")");

        if (blocoIndice == meuTamanho) {
            // ========== BLOCO SEQUENCIAL ==========
            System.out.println("[" + id + "] Bloco sequencial recebido (esperado)");

            if (blockchain.validarBloco(blocoRecebido)) {
                System.out.println("[" + id + "] ✓ Bloco válido adicionado: " + blocoIndice);

                blockchain.adicionarBloco(blocoRecebido);
                minerador.parar(); // Para mineração (já temos novo bloco)
                blockchain.limparTransacoesProcessadas(blocoRecebido);

                // Rebroadcast para outros nós (exceto quem enviou)
                rebroadcastBloco(blocoRecebido, peerOrigem);

                System.out.println("[" + id + "] Tamanho blockchain agora: " + blockchain.getTamanho());
            } else {
                System.out.println("[" + id + "] ✗ Bloco inválido rejeitado");
                // Pode indicar fork ou ataque - não faz nada, espera novo bloco
            }

        } else if (blocoIndice > meuTamanho) {
            // ========== BLOCKCHAIN DESATUALIZADA ==========
            int diferenca = blocoIndice - meuTamanho;
            System.out.println("[" + id + "] ⚠ Blockchain desatualizada (" + diferenca +
                    " blocos atrás), sincronizando...");

            // Requisita blockchain completa
            for (Peer peer : peers) {
                if (peer.getId().equals(peerOrigem) && peer.isConectado()) {
                    peer.enviar(new MensagemP2P(TipoMensagem.REQUISITAR_BLOCKCHAIN, null, this.id));
                    System.out.println("[" + id + "] Requisição de blockchain enviada para " + peerOrigem);
                    break;
                }
            }

        } else if (blocoIndice < meuTamanho) {
            // ========== BLOCO ANTIGO (FORK) ==========
            System.out.println("[" + id + "] ⚠ Bloco mais antigo recebido (possível fork)");
            System.out.println("[" + id + "]   Bloco: " + blocoIndice + ", Meu tamanho: " + meuTamanho);

            // Se for apenas 1 bloco atrás, pode ser fork - ignora silenciosamente
            // Se for muito atrás, ignora (já tem a cadeia correta)

            if (meuTamanho - blocoIndice <= 2) {
                System.out.println("[" + id + "] Fork detectado, mantendo minha cadeia (maior)");
            }
        }
    }

    private void rebroadcastBloco(Bloco b, String peerOrigem) {
        MensagemP2P msg = new MensagemP2P(TipoMensagem.NOVO_BLOCO, b, this.id);
        for (Peer peer : peers) {
            if (peer.isConectado() && !peer.getId().equals(peerOrigem)) {
                peer.enviar(msg);
            }
        }
    }

    public synchronized void sincronizarBlockchain(List<Bloco> blocoRemoto) {
        if (blocoRemoto == null) return;

        if (blocoRemoto.size() > blockchain.getTamanho()) {
            System.out.println("[" + id + "] Atualizando blockchain (tamanho remoto: " +
                    blocoRemoto.size() + ")");
            blockchain.substituir(blocoRemoto);
            minerador.parar();
        }
    }

    // ============ OPERAÇÕES ============

    public void adicionarTransacao(Transacao t) {
        if (blockchain.adicionarAoPool(t)) {
            broadcastTransacao(t);
        }
    }

    public void minerarManualmente() {
        minerador.minerarAgora();
    }

    // ============ GETTERS ============

    public String getId() { return id; }
    public BlockchainGovernamental getBlockchain() { return blockchain; }
    public int getNumPeers() { return peers.size(); }

    public String getStatus() {
        return "Id: " + id +
                " | Tamanho blockchain: " + blockchain.getTamanho() +
                " | Transações pendentes: " + blockchain.getPoolSize() +
                " | Peers: " + peers.size();
    }
}