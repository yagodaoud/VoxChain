package com.yagodaoud.VoxChain.rede;

import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.blockchain.TransacaoTracker;
import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.utils.Logger;

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
    private No noLocal;

    public Peer(String id, Socket socket, No noLocal) throws IOException {
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

                Logger.network(noLocal.getId(), "Mensagem recebida de " +
                        msg.getRemetente() + ": " + msg.getTipo());

                processarMensagem(msg);
            }
        } catch (EOFException e) {
            Logger.network(noLocal.getId(), "Peer " + id + " desconectado");
        } catch (IOException | ClassNotFoundException e) {
            Logger.error(noLocal.getId(), "Erro ao receber mensagem: " + e.getMessage());
        } finally {
            desconectar();
        }
    }

    public void enviar(MensagemP2P msg) {
        try {
            synchronized (output) {
                output.writeObject(msg);
                output.flush();
                Logger.network(noLocal.getId(), "Mensagem enviada para " + id);
            }
        } catch (IOException e) {
            Logger.error(noLocal.getId(), "Erro ao enviar para " + id + ": " + e.getMessage());
            desconectar();
        }
    }

    private void processarMensagem(MensagemP2P msg) {
        switch (msg.getTipo()) {
            case NOVA_TRANSACAO:
                Transacao t = (Transacao) msg.getPayload();
                Logger.debug(noLocal.getId(), "NOVA_TRANSACAO recebida. ID: " + t.getId() +
                        " | Remetente: " + msg.getRemetente() +
                        " | Origem: " + this.id);

                if (!noLocal.getBlockchain().transacaoExiste(t)) {
                    Logger.debug(noLocal.getId(), "Transação NÃO existe no blockchain. Adicionando ao pool...");
                    noLocal.getBlockchain().adicionarAoPool(t);
                    TransacaoTracker.rastrearAdicao(noLocal.getId(), t, this.id);
                    Logger.info(noLocal.getId(), "Transação adicionada ao pool: " + t.getId());
                } else {
                    Logger.debug(noLocal.getId(), "Transação JÁ existe. Rejeitando.");
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
                Logger.network(noLocal.getId(), "PING recebido de " + msg.getRemetente());
                enviar(new MensagemP2P(TipoMensagem.PONG, null, noLocal.getId()));
                break;

            case PONG:
                Logger.network(noLocal.getId(), "PONG recebido de " + msg.getRemetente());
                break;

            case LISTAR_PEERS:
                Logger.network(noLocal.getId(), "Peer " + id + " pediu lista de peers");
                java.util.List<com.yagodaoud.VoxChain.rede.PeerDiscovery.PeerInfo> catalogo = noLocal.obterCatalogoPeers();
                enviar(new MensagemP2P(TipoMensagem.RESPOSTA_PEERS, catalogo, noLocal.getId()));
                break;

            case RESPOSTA_PEERS:
                Logger.network(noLocal.getId(), "Recebeu lista de peers de " + msg.getRemetente());
                java.util.List<com.yagodaoud.VoxChain.rede.PeerDiscovery.PeerInfo> novosCatalogo = (java.util.List) msg.getPayload();
                noLocal.atualizarCatalogoPeers(novosCatalogo);
                break;

            default:
                Logger.error(noLocal.getId(), "Tipo de mensagem desconhecido: " + msg.getTipo());
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