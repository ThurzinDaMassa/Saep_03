package oficina.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hashes PBKDF2 com salt aleatório; senhas em texto não são persistidas. */
public final class SenhaService {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hash(String password) throws Exception {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "210000$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, 210000));
    }

    public static byte[] derive(String password, byte[] salt, int iterations) throws Exception {
        var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean verify(String password, String stored) throws Exception {
        var p = stored.split("\\$");
        return MessageDigest.isEqual(
                Base64.getDecoder().decode(p[2]),
                derive(password, Base64.getDecoder().decode(p[1]), Integer.parseInt(p[0])));
    }
}
