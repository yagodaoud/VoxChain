package blockchain;

import config.ConfigManager;
import modelo.Transacao;
import rede.MensagemP2P;
import rede.Peer;
import rede.PeerDiscovery;
import rede.TipoMensagem;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class No {
    private final String id;
    private final String enderecoIP;
    private final int porta;
    private final BlockchainGovernamental blockchain;
    private final List<Peer> peers;
    private ServerSocket servidor;
    private volatile boolean rodando = false;
    private Thread threadServidorAceitar;
    private Minerador minerador;
    private PeerDiscovery peerDiscovery;

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

        try {
            servidor = new ServerSocket(porta);
            System.out.println("[" + id + "] Servidor iniciado em " + enderecoIP + ":" + porta);
        } catch (IOException e) {
            System.err.println("[" + id + "] Erro ao criar servidor: " + e.getMessage());
            return;
        }

        // Thread que aceita conexões remotas
        threadServidorAceitar = new Thread(this::aceitarConexoes);
        threadServidorAceitar.setName("Servidor-" + id);
        threadServidorAceitar.start();

        // ★ NOVO: Inicializar PeerDiscovery
        peerDiscovery = new PeerDiscovery(this);
        List<PeerDiscovery.PeerInfo> bootstrapNodes = ConfigManager.obterBootstrapNodes();

        // Remove a si mesmo da lista de bootstrap
        bootstrapNodes.removeIf(p -> p.id.equals(this.id));

        peerDiscovery.iniciar(bootstrapNodes);
        System.out.println("[" + id + "] PeerDiscovery ativado");

        // Inicia minerador
        minerador = new Minerador(this);
        Thread threadMinerador = new Thread(minerador);
        threadMinerador.setName("Minerador-" + id);
        threadMinerador.start();
    }

    public void parar() {
        rodando = false;
        if (minerador != null) minerador.parar();
        if (peerDiscovery != null) peerDiscovery.parar();
        for (Peer p : peers) p.desconectar();

        try {
            if (servidor != null) servidor.close();
        } catch (IOException ignored) {}
    }

    // ============ CONEXÃO COM PEERS ============

    public void conectarPeer(String ipRemoto, int portaRemota, String idRemoto) {
        new Thread(() -> {
            try {
                System.out.println("[" + id + "] Tentando conectar em " + idRemoto + " (" + ipRemoto + ":" + portaRemota + ")");
                Socket socket = new Socket(ipRemoto, portaRemota);
                Peer peer = new Peer(idRemoto, socket, this);
                peers.add(peer);

                new Thread(peer).start();
                System.out.println("[" + id + "] ✓ Conectado a " + idRemoto);

                // Solicita blockchain atualizada
                peer.enviar(new MensagemP2P(TipoMensagem.REQUISITAR_BLOCKCHAIN, null, this.id));

            } catch (IOException e) {
                System.err.println("[" + id + "] ✗ Erro ao conectar em " + idRemoto + ": " + e.getMessage());
            }
        }).start();
    }

    private void aceitarConexoes() {
        while (rodando) {
            try {
                Socket socketCliente = servidor.accept();
                System.out.println("[" + id + "] Nova conexão recebida");

                Peer peer = new Peer("Remoto-" + System.nanoTime(), socketCliente, this);
                peers.add(peer);
                new Thread(peer).start();

            } catch (IOException e) {
                if (rodando)
                    System.err.println("[" + id + "] Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    // ============ BROADCAST ============

    public void broadcastTransacao(Transacao t) {
        MensagemP2P msg = new MensagemP2P(TipoMensagem.NOVA_TRANSACAO, t, id);
        for (Peer peer : peers)
            if (peer.isConectado()) peer.enviar(msg);
    }

    public void rebroadcastTransacao(Transacao t, String peerOrigem) {
        MensagemP2P msg = new MensagemP2P(TipoMensagem.NOVA_TRANSACAO, t, id);
        for (Peer peer : peers)
            if (peer.isConectado() && !peer.getId().equals(peerOrigem))
                peer.enviar(msg);
    }

    public void broadcastBloco(Bloco b) {
        MensagemP2P msg = new MensagemP2P(TipoMensagem.NOVO_BLOCO, b, id);
        for (Peer peer : peers)
            if (peer.isConectado()) peer.enviar(msg);
    }

    // ============ PROCESSAMENTO DE BLOCOS ============

    public synchronized void processarNovoBloco(Bloco blocoRecebido, String peerOrigem) {
        if (blocoRecebido == null) {
            System.out.println("[" + id + "] ✗ Bloco nulo recebido");
            return;
        }

        int meuTamanho = blockchain.getTamanho();
        int blocoIndice = blocoRecebido.getIndice();

        System.out.println("[" + id + "] Recebeu bloco " + blocoIndice +
                " de " + blocoRecebido.getMineradoPor() +
                " (meu tamanho: " + meuTamanho + ")");

        if (blocoIndice == meuTamanho) {
            // BLOCO CORRETO SEQUENCIAL
            if (blockchain.validarBloco(blocoRecebido)) {
                blockchain.adicionarBloco(blocoRecebido);
                blockchain.limparTransacoesProcessadas(blocoRecebido);
                minerador.parar();

                // Rebroadcast
                rebroadcastBloco(blocoRecebido, peerOrigem);
                System.out.println("[" + id + "] ✓ Bloco " + blocoIndice + " adicionado com sucesso");
            } else {
                System.out.println("[" + id + "] ✗ Bloco inválido rejeitado");
            }

        } else if (blocoIndice > meuTamanho) {
            // DESATUALIZADO
            System.out.println("[" + id + "] ⚠ Blockchain desatualizada, solicitando sincronização...");
            for (Peer peer : peers) {
                if (peer.getId().equals(peerOrigem) && peer.isConectado()) {
                    peer.enviar(new MensagemP2P(TipoMensagem.REQUISITAR_BLOCKCHAIN, null, id));
                    break;
                }
            }

        } else {
            // BLOCO ANTIGO (fork)
            System.out.println("[" + id + "] ⚠ Bloco antigo recebido (fork detectado). Mantendo minha cadeia.");
        }
    }

    private void rebroadcastBloco(Bloco b, String peerOrigem) {
        MensagemP2P msg = new MensagemP2P(TipoMensagem.NOVO_BLOCO, b, id);
        for (Peer peer : peers)
            if (peer.isConectado() && !peer.getId().equals(peerOrigem))
                peer.enviar(msg);
    }

    public synchronized void sincronizarBlockchain(List<Bloco> blocoRemoto) {
        if (blocoRemoto == null) return;
        if (blocoRemoto.size() > blockchain.getTamanho()) {
            System.out.println("[" + id + "] 🔄 Substituindo blockchain local por versão mais longa (" +
                    blocoRemoto.size() + " blocos)");
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

    public List<Peer> getPeers() {
        return peers;
    }

    public List<PeerDiscovery.PeerInfo> obterCatalogoPeers() {
        return peerDiscovery.getCatalogo();
    }

    public void atualizarCatalogoPeers(List<PeerDiscovery.PeerInfo> peers) {
        if (peerDiscovery != null) {
            peerDiscovery.atualizarCatalogo(peers);
        }
    }

    public String getStatus() {
        String discovery = peerDiscovery != null ?
                " | " + peerDiscovery.getStatusDiscovery() : "";

        return "[" + id + "] Blockchain: " + blockchain.getTamanho() +
                " blocos | Pool: " + blockchain.getPoolSize() +
                " | Peers: " + peers.size() + discovery;
    }
}
