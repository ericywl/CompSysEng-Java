import javax.xml.bind.DatatypeConverter;
import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.FileReader;
import java.security.*;


public class DigitalSignature {
    public static void main(String[] args) throws Exception {
        // Read the text file and save to String data
        String fileName = "lab06_encryption/files/largeSize.txt";
        StringBuilder data = new StringBuilder();
        String line;
        BufferedReader bufferedReader = new BufferedReader( new FileReader(fileName));
        while((line= bufferedReader.readLine())!=null){
            data.append("\n").append(line);
        }

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);

        KeyPair keyPair = keyGen.generateKeyPair();
        Key publicKey =  keyPair.getPublic();
        Key privateKey = keyPair.getPrivate();

        // Message digest
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data.toString().getBytes());
        byte[] digest = md.digest();

        // Encryption
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE,  privateKey);
        byte[] encryptedBytes = rsaCipher.doFinal(digest);

        // Decryption
        rsaCipher.init(Cipher.DECRYPT_MODE,  publicKey);
        byte[] decryptedBytes = rsaCipher.doFinal(encryptedBytes);

        System.out.println("=== ORIGINAL CONTENT ===");
        System.out.println(data);
        System.out.println();

        System.out.println("Message digest (MD5): " + DatatypeConverter.printBase64Binary(digest));
        System.out.println("Digest length: " + digest.length);
        System.out.println();

        System.out.println("Encrypted digital signature: "
                + DatatypeConverter.printBase64Binary(encryptedBytes));
        System.out.println("Decrypted digital signature: "
                + DatatypeConverter.printBase64Binary(decryptedBytes));
        System.out.println( "Original digest byte length: " + decryptedBytes.length);
        System.out.println( "Signed digest byte length: " + encryptedBytes.length);
    }

}