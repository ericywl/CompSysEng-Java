import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.crypto.*;
import javax.xml.bind.DatatypeConverter;


public class DESSolution {
    public static void main(String[] args) throws Exception {
        String fileName = "lab06_encryption/files/smallSize.txt";
        StringBuilder data = new StringBuilder();
        String line;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
        while ((line = bufferedReader.readLine()) != null) {
            data.append("\n").append(line);
        }
        System.out.println("Original content: " + data);
        System.out.println();

        // generate secret key using DES algorithm
        SecretKey key = KeyGenerator.getInstance("DES").generateKey();
        // create cipher object, initialize the ciphers with the given key,
        // choose encryption mode as DES
        Cipher ecipher = Cipher.getInstance("DES");
        ecipher.init(Cipher.ENCRYPT_MODE, key);

        // do encryption, by calling method Cipher.doFinal().
        String plaintext = data.toString();
        byte[] encryptedBytes = ecipher.doFinal(plaintext.getBytes());

        String base64format = DatatypeConverter.printBase64Binary(encryptedBytes);
        System.out.println("Cipher text: " + base64format);
        System.out.println("Cipher text length: " + encryptedBytes.length);
        // TODO: compare the length of file smallSize.txt and largeSize.txt
        System.out.println();

        // create cipher object, initialize the ciphers with the given key,
        // choose decryption mode as DES
        Cipher dcipher = Cipher.getInstance("DES");
        dcipher.init(Cipher.DECRYPT_MODE, key);
        // do decryption, by calling method Cipher.doFinal().
        byte[] decryptedBytes = dcipher.doFinal(encryptedBytes);
        System.out.println("Decrypted text is: " + new String(decryptedBytes));

    }
}