package modelo;

import java.io.Serializable;
import java.security.MessageDigest;

public class Administrador implements Serializable {

    private String id;
    private String nome;
    private String senhaHash;
    private NivelAcesso nivel;
    private boolean ativo;

    public enum NivelAcesso {
        SUPER_ADMIN, // Pode tudo
        ADMIN_TSE, // Gerencia eleições
        OPERADOR // Apenas consulta
    }

    public Administrador(String id, String nome, String senha, NivelAcesso nivel) {
        this.id = id;
        this.nome = nome;
        this.senhaHash = hashSenha(senha);
        this.nivel = nivel;
        this.ativo = true;
    }

    private String hashSenha(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(senha.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verificarSenha(String senha) {
        return hashSenha(senha).equals(senhaHash);
    }
}
