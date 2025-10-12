package rede;

import blockchain.Bloco;
import blockchain.TransacaoTracker;
import modelo.Transacao;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Peer implements Runnable {
    private String id;
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private boolean conectado;
    private blockchain.No noLocal;

    public Peer(String id, Socket socket, blockchain.No noLocal) throws IOException {
        this.id = id;
        this.socket = socket;
        this.noLocal = noLocal;
        this.conectado = true;

        this.output = new ObjectOutputStream(socket.getOutputStream());
        this.output.flush();
        this.input = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (conectado && !Thread.currentThread().isInterrupted()) {
                MensagemP2P msg = (MensagemP2P) input.readObject();

                System.out.println("[" + noLocal.getId() + "] Mensagem recebida de " +
                        msg.getRemetente() + ": " + msg.getTipo());

                processarMensagem(msg);
            }
        } catch (EOFException e) {
            System.out.println("[" + noLocal.getId() + "] Peer " + id + " desconectado");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[" + noLocal.getId() + "] Erro ao receber mensagem: " + e.getMessage());
        } finally {
            desconectar();
        }
    }

    public void enviar(MensagemP2P msg) {
        try {
            synchronized (output) {
                output.writeObject(msg);
                output.flush();
                System.out.println("[" + noLocal.getId() + "] Mensagem enviada para " + id);
            }
        } catch (IOException e) {
            System.err.println("[" + noLocal.getId() + "] Erro ao enviar para " + id + ": " + e.getMessage());
            desconectar();
        }
    }

    private void processarMensagem(MensagemP2P msg) {
        switch (msg.getTipo()) {
            case NOVA_TRANSACAO:
                Transacao t = (Transacao) msg.getPayload();
                System.out.println("[DEBUG] NOVA_TRANSACAO recebida. ID: " + t.getId() +
                        " | Remetente: " + msg.getRemetente() +
                        " | Origem: " + this.id);

                if (!noLocal.getBlockchain().transacaoExiste(t)) {
                    System.out.println("[DEBUG] Transação NÃO existe no blockchain. Adicionando ao pool...");
                    noLocal.getBlockchain().adicionarAoPool(t);
                    TransacaoTracker.rastrearAdicao(noLocal.getId(), t, this.id);
                    System.out.println("[" + noLocal.getId() + "] ✓ Transação adicionada ao pool: " + t.getId());
                } else {
                    System.out.println("[DEBUG] Transação JÁ existe. Rejeitando.");
                    TransacaoTracker.rastrearAdicao(noLocal.getId(), t, "DUPLICATA-" + this.id);
                }
                break;

            case NOVO_BLOCO:
                Bloco bloco = (Bloco) msg.getPayload();
                noLocal.processarNovoBloco(bloco, this.id);
                break;

            case REQUISITAR_BLOCKCHAIN:
                enviar(new MensagemP2P(TipoMensagem.RESPOSTA_BLOCKCHAIN,
                        noLocal.getBlockchain().getBlocos(), msg.getRemetente()));
                break;

            case RESPOSTA_BLOCKCHAIN:
                noLocal.sincronizarBlockchain((java.util.List) msg.getPayload());
                break;

            case PING:
                System.out.println("[" + noLocal.getId() + "] 📍 PING recebido de " + msg.getRemetente());
                enviar(new MensagemP2P(TipoMensagem.PONG, null, noLocal.getId()));
                break;

            case PONG:
                System.out.println("[" + noLocal.getId() + "] 📍 PONG recebido de " + msg.getRemetente());
                break;

            case LISTAR_PEERS:
                System.out.println("[" + noLocal.getId() + "] 📋 Peer " + id + " pediu lista de peers");
                java.util.List<rede.PeerDiscovery.PeerInfo> catalogo = noLocal.obterCatalogoPeers();
                enviar(new MensagemP2P(TipoMensagem.RESPOSTA_PEERS, catalogo, noLocal.getId()));
                break;

            case RESPOSTA_PEERS:
                System.out.println("[" + noLocal.getId() + "] 📋 Recebeu lista de peers de " + msg.getRemetente());
                java.util.List<rede.PeerDiscovery.PeerInfo> novosCatalogo = (java.util.List) msg.getPayload();
                noLocal.atualizarCatalogoPeers(novosCatalogo);
                break;

            default:
                System.out.println("[" + noLocal.getId() + "] ⚠ Tipo de mensagem desconhecido: " + msg.getTipo());
        }
    }

    public void desconectar() {
        conectado = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            // ignored
        }
    }

    public String getId() { return id; }
    public boolean isConectado() { return conectado; }
}