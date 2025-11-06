package com.yagodaoud.VoxChain.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.yagodaoud.VoxChain.modelo.enums.NivelAcesso;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class SecurityUtils {
    private static final String JWT_SECRET = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET")
            : "secret";

    private static final Algorithm JWT_ALGORITHM = Algorithm.HMAC256(JWT_SECRET);


    public static String hash(String text, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + text).getBytes(StandardCharsets.UTF_8));
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

    /**
     * Gera hash seguro de senha usando BCrypt
     * @param senha Senha em texto plano
     * @return Hash BCrypt da senha
     */
    public static String hashSenhaSegura(String senha) {
        return BCrypt.hashpw(senha, BCrypt.gensalt(12));
    }

    /**
     * Verifica se uma senha corresponde ao hash BCrypt
     * @param senha Senha em texto plano
     * @param hash Hash BCrypt armazenado
     * @return true se a senha corresponde ao hash
     */
    public static boolean verificarSenha(String senha, String hash) {
        return BCrypt.checkpw(senha, hash);
    }

    public static String gerarToken(String cpfHash, NivelAcesso nivelAcesso) {
        Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
        return JWT.create()
                .withIssuer("voxchain")
                .withSubject(cpfHash)
                .withClaim("nivelAcesso", nivelAcesso.name())
                .withExpiresAt(new Date(System.currentTimeMillis() + 86400000))
                .sign(algorithm);
    }

    /**
     * Verifica e decodifica o token JWT
     */
    public static DecodedJWT verificarToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(JWT_ALGORITHM)
                .withIssuer("voxchain")
                .build();

        return verifier.verify(token);
    }
}
