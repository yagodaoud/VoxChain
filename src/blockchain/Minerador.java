package blockchain;

import rede.MensagemP2P;
import rede.TipoMensagem;
import modelo.Transacao;

public class Minerador implements Runnable {
    private No no;
    private volatile boolean minerando = false;
    private volatile boolean parar = false;

    public Minerador(No no) {
        this.no = no;
    }

    @Override
    public void run() {
        while (!parar) {
            if (!minerando && no.getBlockchain().temTransacoesPendentes()) {
                minerando = true;
                minerarBloco();
                minerando = false;
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void minerarBloco() {
        System.out.println("[" + no.getId() + "] Iniciando mineração...");

        Bloco bloco = no.getBlockchain().criarBlocoCandidato();
        if (bloco == null) return;

        long inicio = System.currentTimeMillis();
        bloco.minerarBloco(2); // dificuldade 2
        long duracao = System.currentTimeMillis() - inicio;

        // Verifica se ainda é válido (outro nó pode ter minerado)
        if (bloco.getIndice() == no.getBlockchain().getTamanho()) {
            System.out.println("[" + no.getId() + "] ✓ Bloco minerado em " + duracao + "ms");
            no.getBlockchain().adicionarBloco(bloco);
            no.broadcastBloco(bloco);
        } else {
            System.out.println("[" + no.getId() + "] ✗ Bloco descartado (outro nó foi mais rápido)");
        }
    }

    public void minerarAgora() {
        minerarBloco();
    }

    public void parar() {
        minerando = false;
    }
}