package rede;

import blockchain.No;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkMonitor {
    private final No noLocal;
    private final ScheduledExecutorService executor;
    private volatile boolean rodando = false;
    private long transacoesEnviadas = 0;
    private long blocosEnviados = 0;
    private long pingsEnviados = 0;
    private long bytesEnviados = 0;

    public NetworkMonitor(No noLocal) {
        this.noLocal = noLocal;
        this.executor = Executors.newScheduledThreadPool(1);
    }

    public void iniciar(int intervaloSegundos) {
        if (rodando) return;
        rodando = true;

        executor.scheduleAtFixedRate(
                this::exibirDashboard,
                2,                  // delay inicial
                intervaloSegundos,  // intervalo
                TimeUnit.SECONDS
        );
    }

    public void parar() {
        rodando = false;
        executor.shutdown();
    }

    private void exibirDashboard() {
        limparTela();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timestamp = LocalDateTime.now().format(fmt);

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          🌐 REDE P2P - BLOCKCHAIN GOVERNAMENTAL 🌐          ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");

        // Info do Nó Local
        System.out.println("║ 📍 NÓ LOCAL: " + formatarInfo(noLocal.getId(), 13));
        System.out.println("║    • Blockchain: " + noLocal.getBlockchain().getTamanho() +
                " blocos | Pool: " + noLocal.getBlockchain().getPoolSize() + " transações");

        // Status da Rede
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ 🔗 STATUS DA REDE");

        int totalPeers = noLocal.getNumPeers();
        int peersConectados = (int) noLocal.getPeers().stream()
                .filter(Peer::isConectado).count();

        System.out.println("║    • Peers Conectados: " + peersConectados + "/" + totalPeers);
        System.out.println("║    • Catálogo de Peers: " +
                (noLocal.obterCatalogoPeers() != null ?
                        noLocal.obterCatalogoPeers().size() : "N/A"));

        // Lista de Peers
        if (totalPeers > 0) {
            System.out.println("╠════════════════════════════════════════════════════════════╣");
            System.out.println("║ 👥 PEERS CONECTADOS");

            int contador = 1;
            for (Peer peer : noLocal.getPeers()) {
                String status = peer.isConectado() ? "✓ ONLINE" : "✗ OFFLINE";
                String linha = "║    [" + contador + "] " + String.format("%-20s", peer.getId()) +
                        " " + status;
                System.out.println(linha);
                contador++;
            }
        }

        // Estatísticas
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ 📊 ESTATÍSTICAS");
        System.out.println("║    • Transações Sincronizadas: " + transacoesEnviadas);
        System.out.println("║    • Blocos Propagados: " + blocosEnviados);
        System.out.println("║    • Pings Enviados: " + pingsEnviados);

        // Timestamp
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ ⏰ " + timestamp + " | Atualizado a cada 10s              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
    }

    private String formatarInfo(String texto, int tamanho) {
        if (texto.length() > tamanho) {
            return texto.substring(0, tamanho);
        }
        return String.format("%-" + tamanho + "s", texto);
    }

    private void limparTela() {
        // Funciona em Linux/Mac
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            // Se falhar, apenas imprime quebras de linha
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    // ============ RASTREADORES ============

    public void registrarTransacao() {
        transacoesEnviadas++;
    }

    public void registrarBloco() {
        blocosEnviados++;
    }

    public void registrarPing() {
        pingsEnviados++;
    }

    public void registrarBytes(long bytes) {
        bytesEnviados += bytes;
    }

    // ============ GETTERS ============

    public long getTransacoesEnviadas() {
        return transacoesEnviadas;
    }

    public long getBlocosEnviados() {
        return blocosEnviados;
    }

    public long getPingsEnviados() {
        return pingsEnviados;
    }

    public long getBytesEnviados() {
        return bytesEnviados;
    }
}