package modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Eleitor {
    private String tituloHash;
    private int zona;
    private int secao;


    public Eleitor(String tituloDeEleitor, int zona, int secao) {
        this.tituloHash = hashTitulo(tituloDeEleitor);
        this.zona = zona;
        this.secao = secao;
    }

    public String getTituloDeEleitorHash() {
        return tituloHash;
    }

    public int getZona() {
        return zona;
    }

    public int getSecao() {
        return secao;
    }


    private String hashTitulo(String titulo) {
        try {
            String salt = "ELeicao2025"; // Pode vir de config global
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + titulo).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
