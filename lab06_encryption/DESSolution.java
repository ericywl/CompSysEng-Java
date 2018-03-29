import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.crypto.*;
import javax.xml.bind.DatatypeConverter;


public class DESSolution {
    public static void main(String[] args) throws Exception {
        String fileName = "smallSize.txt";
        StringBuilder data = new StringBuilder();
        String line;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
        while ((line = bufferedReader.readLine()) != null) {
            data.append("\n").append(line);
        }
        System.out.println("Original content: " + data);

        // generate secret key using DES algorithm
        SecretKey key = KeyGenerator.getInstance("DES").generateKey();
        // create cipher object, initialize the ciphers with the given key,
        // choose encryption mode as DES
        Cipher ecipher = Cipher.getInstance("DES");
        ecipher.init(Cipher.ENCRYPT_MODE, key);

        String plaintext = data.toString();
        // do encryption, by calling method Cipher.doFinal().
        byte[] encryptedBytes = ecipher.doFinal(plaintext.getBytes());
        System.out.println(encryptedBytes.length);
        // TODO: compare the length of file smallSize.txt and largeSize.txt

        String base64format = DatatypeConverter.printBase64Binary(encryptedBytes);
        System.out.println("Cipher text: " + base64format);
        // create cipher object, initialize the ciphers with the given key,
        // choose decryption mode as DES
        Cipher dcipher = Cipher.getInstance("DES");
        dcipher.init(Cipher.DECRYPT_MODE, key);
        // do decryption, by calling method Cipher.doFinal().
        byte[] decryptedBytes = dcipher.doFinal(encryptedBytes);
        System.out.println("Decrypted text is: " + new String(decryptedBytes));

    }
}