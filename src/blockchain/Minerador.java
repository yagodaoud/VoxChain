package blockchain;

public class Minerador implements Runnable {
    private No no;
    private volatile boolean minerando = false;
    private volatile boolean parar = false;
    private long ultimaMineracao = 0;
    private final long INTERVALO_MINIMO = 5000; // 5 segundos entre minerações

    public Minerador(No no) {
        this.no = no;
    }

    @Override
    public void run() {
        while (!parar) {
            try {
                // Verifica se pode minerar
                if (!minerando &&
                        no.getBlockchain().temTransacoesPendentes() &&
                        podeMinerar()) {

                    minerando = true;
                    minerarBloco();
                    minerando = false;
                    ultimaMineracao = System.currentTimeMillis();
                }

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ============ VERIFICAR SE PODE MINERAR ============
    private boolean podeMinerar() {
        long agora = System.currentTimeMillis();

        // Evita minerar muito rápido (deixa outros nós receberem o bloco anterior)
        if (agora - ultimaMineracao < INTERVALO_MINIMO) {
            return false;
        }

        // Só minera se tem transações
        if (!no.getBlockchain().temTransacoesPendentes()) {
            return false;
        }

        return true;
    }

    private void minerarBloco() {
        int tamanhoBlockchain = no.getBlockchain().getTamanho();
        int poolSize = no.getBlockchain().getPoolSize();

        System.out.println("[" + no.getId() + "] Iniciando mineração (blockchain: " +
                tamanhoBlockchain + " blocos, pool: " + poolSize + " transações)");

        Bloco bloco = no.getBlockchain().criarBlocoCandidato();
        if (bloco == null) {
            System.out.println("[" + no.getId() + "] Pool vazio, cancelando mineração");
            minerando = false;
            return;
        }

        long inicio = System.currentTimeMillis();
        bloco.minerarBloco(2); // dificuldade 2
        long duracao = System.currentTimeMillis() - inicio;

        // VERIFICAÇÃO CRÍTICA: O bloco ainda é válido?
        // (outro nó pode ter minerado antes)
        if (bloco.getIndice() != no.getBlockchain().getTamanho()) {
            System.out.println("[" + no.getId() + "] ✗ Bloco descartado (blockchain evoluiu, " +
                    "outro nó foi mais rápido)");
            return;
        }

        // Valida o próprio bloco antes de adicionar
        if (!no.getBlockchain().validarBloco(bloco)) {
            System.out.println("[" + no.getId() + "] ✗ Bloco inválido (validação falhou)");
            return;
        }

        System.out.println("[" + no.getId() + "] ✓ Bloco minerado em " + duracao + "ms");
        System.out.println("[" + no.getId() + "]   Índice: " + bloco.getIndice() +
                ", Hash: " + bloco.getHashTruncado(16) +
                ", Transações: " + bloco.getTransacoes().size());

        // Adiciona na própria cadeia
        no.getBlockchain().adicionarBloco(bloco);

        // Broadcast para a rede
        no.broadcastBloco(bloco);
    }

    public void minerarAgora() {
        if (!minerando && no.getBlockchain().temTransacoesPendentes()) {
            minerando = true;
            minerarBloco();
            minerando = false;
            ultimaMineracao = System.currentTimeMillis();
        }
    }

    public void parar() {
        minerando = false;
    }
}