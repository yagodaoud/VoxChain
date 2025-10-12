import blockchain.No;
import rede.ApiServidor;

public class Main {
    public static void main(String[] args) {
        String id = args.length > 0 ? args[0] : "NO-DEFAULT";
        int portaNo = args.length > 1 ? Integer.parseInt(args[1]) : 8001;
        int portaApi = args.length > 2 ? Integer.parseInt(args[2]) : 8080;

        No no = new No(id, "localhost", portaNo);
        no.iniciar();

        // Inicializa API passando o nó
        ApiServidor api = new ApiServidor(no);
        api.iniciar(portaApi);

        System.out.println("[" + id + "] Nó iniciado. API na porta " + portaApi);

        // Conectar em peers conhecidos (opcional)
        if (args.length > 3) {
            for (int i = 3; i < args.length; i += 2) {
                String ipPeer = args[i];
                int portaPeer = Integer.parseInt(args[i+1]);
                no.conectarPeer(ipPeer, portaPeer, "PEER-" + i/2);
            }
        }
    }
}
