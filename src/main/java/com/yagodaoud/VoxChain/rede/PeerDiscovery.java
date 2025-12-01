package com.yagodaoud.VoxChain.rede;

import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.utils.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PeerDiscovery {
    private final No noLocal;
    private final List<PeerInfo> peersCatalogo;
    private final ScheduledExecutorService executor;
    private volatile boolean rodando = false;

    public static class PeerInfo implements Serializable {
        public String id;
        public String ip;
        public int porta;
        public long ultimoContato;
        public boolean ativo;

        public PeerInfo(String id, String ip, int porta) {
            this.id = id;
            this.ip = ip;
            this.porta = porta;
            this.ultimoContato = System.currentTimeMillis();
            this.ativo = false;
        }

        @Override
        public String toString() {
            return id + " (" + ip + ":" + porta + ") " +
                    (ativo ? "✓ ATIVO" : "✗ INATIVO");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PeerInfo peerInfo = (PeerInfo) o;
            return porta == peerInfo.porta &&
                    Objects.equals(id, peerInfo.id) &&
                    Objects.equals(ip, peerInfo.ip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, ip, porta);
        }
    }

    public PeerDiscovery(No noLocal) {
        this.noLocal = noLocal;
        this.peersCatalogo = new CopyOnWriteArrayList<>();
        this.executor = Executors.newScheduledThreadPool(2);
    }

    public void iniciar(List<PeerInfo> bootstrapNodes) {
        if (rodando) return;
        rodando = true;

        // Adiciona bootstrap nodes ao catálogo
        peersCatalogo.addAll(bootstrapNodes);

        Logger.info(noLocal.getId(), "PeerDiscovery iniciado com " + bootstrapNodes.size() + " bootstrap nodes");

        // Task 1: Conectar nos bootstrap nodes periodicamente
        executor.scheduleAtFixedRate(
                this::conectarBootstrapNodes,
                0,           // delay inicial
                5,           // intervalo
                TimeUnit.SECONDS
        );

        // Task 2: Validar saúde dos peers (health check)
        executor.scheduleAtFixedRate(
                this::validarSaudePeers,
                3,           // delay inicial
                10,          // intervalo
                TimeUnit.SECONDS
        );

        // Task 3: Sincronizar lista de peers (gossip)
        executor.scheduleAtFixedRate(
                this::sincronizarCatalogoPeers,
                5,           // delay inicial
                15,          // intervalo
                TimeUnit.SECONDS
        );
    }

    public void parar() {
        rodando = false;
        executor.shutdown();
    }

    // ============ CONEXÃO COM BOOTSTRAP NODES ============

    private void conectarBootstrapNodes() {
        for (PeerInfo peer : peersCatalogo) {
            // Se não está conectado e bootstrapNode, tenta conectar
            if (!peer.ativo && estaDisponivel(peer)) {
                Logger.network(noLocal.getId(), "Tentando bootstrap: " + peer);
                noLocal.conectarPeer(peer.ip, peer.porta, peer.id);
                peer.ativo = true;
                peer.ultimoContato = System.currentTimeMillis();
            }
        }
    }

    // ============ HEALTH CHECK ============

    private void validarSaudePeers() {
        int peersDisco = 0;

        for (Peer peer : noLocal.getPeers()) {
            if (!peer.isConectado()) {
                Logger.error(noLocal.getId(), "Peer desconectado: " + peer.getId());
                marcarPeerComoInativo(peer.getId());
            } else {
                // Envia PING para validar
                peer.enviar(new MensagemP2P(TipoMensagem.PING, null, noLocal.getId()));
                peersDisco++;
            }
        }

        if (peersDisco == 0 && !peersCatalogo.isEmpty()) {
            Logger.error(noLocal.getId(), "Sem peers conectados. Tentando reconectar...");
        }
    }

    // ============ SINCRONIZAR CATÁLOGO (GOSSIP) ============

    private void sincronizarCatalogoPeers() {
        // Se temos peers conectados, pedimos a lista deles
        int conectados = 0;

        for (Peer peer : noLocal.getPeers()) {
            if (peer.isConectado()) {
                peer.enviar(new MensagemP2P(TipoMensagem.LISTAR_PEERS, null, noLocal.getId()));
                conectados++;
            }
        }

        if (conectados > 0) {
            Logger.network(noLocal.getId(), "Sincronizando catálogo com " + conectados + " peers");
        }
    }

    // ============ GERENCIAMENTO DE CATÁLOGO ============

    public void adicionarPeer(String id, String ip, int porta) {
        for (PeerInfo existing : peersCatalogo) {
            if (existing.id.equals(id)) {
                return; // Já existe
            }
        }
        PeerInfo novo = new PeerInfo(id, ip, porta);
        peersCatalogo.add(novo);
        Logger.network(noLocal.getId(), "Novo peer descoberto: " + novo);
    }

    public void atualizarCatalogo(List<PeerInfo> peersRecebidos) {
        for (PeerInfo peerRemoto : peersRecebidos) {
            adicionarPeer(peerRemoto.id, peerRemoto.ip, peerRemoto.porta);
        }
    }

    private void marcarPeerComoInativo(String idPeer) {
        for (PeerInfo peer : peersCatalogo) {
            if (peer.id.equals(idPeer)) {
                peer.ativo = false;
                break;
            }
        }
    }

    private boolean estaDisponivel(PeerInfo peer) {
        long tempoDesdeContato = System.currentTimeMillis() - peer.ultimoContato;
        return tempoDesdeContato > 2000; // Tenta novamente após 2s
    }

    // ============ GETTERS ============

    public List<PeerInfo> getCatalogo() {
        return new ArrayList<>(peersCatalogo);
    }

    public int getTotalPeersCatalogo() {
        return peersCatalogo.size();
    }

    public int getPeersAtivos() {
        return (int) peersCatalogo.stream().filter(p -> p.ativo).count();
    }

    public String getStatusDiscovery() {
        int conectados = (int) noLocal.getPeers().stream()
                .filter(Peer::isConectado).count();
        return "Catálogo: " + peersCatalogo.size() + " | Ativos: " + getPeersAtivos() +
                " | Conectados: " + conectados;
    }
}