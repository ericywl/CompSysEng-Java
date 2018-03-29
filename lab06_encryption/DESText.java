import java.io.BufferedReader;
import java.io.FileReader;
import javax.crypto.*;
import javax.xml.bind.DatatypeConverter;


public class DESText {
    public static void main(String[] args) throws Exception {
        String fileName = "lab06_encryption/files/largeSize.txt";
        StringBuilder data = new StringBuilder();
        String line;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
        while ((line = bufferedReader.readLine()) != null) {
            data.append("\n").append(line);
        }

        // Encryption
        SecretKey key = KeyGenerator.getInstance("DES").generateKey();
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        String plaintext = data.toString();
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        String base64format = DatatypeConverter.printBase64Binary(encryptedBytes);

        // Decryption
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        String decryptedString = new String(decryptedBytes);

        System.out.println("=== ORIGINAL CONTENT ===");
        System.out.println(plaintext);
        System.out.println();

        System.out.println("=== DECRYPTED CONTENT ===");
        System.out.println(decryptedString);
        System.out.println();

        String sameStr = decryptedString.equals(plaintext) ? "Yes" : "No";
        System.out.println("Is the decrypted text same as original?: " + sameStr);
        System.out.println("Cipher text: " + base64format);
        System.out.println("Cipher text length: " + encryptedBytes.length);
    }
}