import javax.xml.bind.DatatypeConverter;
import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.FileReader;
import java.security.*;


public class DigitalSignatureSolution {
    public static void main(String[] args) throws Exception {
        // Read the text file and save to String data
        String fileName = "lab06_encryption/files/smallSize.txt";
        StringBuilder data = new StringBuilder();
        String line;
        BufferedReader bufferedReader = new BufferedReader( new FileReader(fileName));
        while((line= bufferedReader.readLine())!=null){
            data.append("\n").append(line);
        }
        System.out.println("Original content: "+ data);

        // generate a RSA keypair, initialize as 1024 bits,
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        // get public key and private key from this keypair.
        KeyPair keyPair = keyGen.generateKeyPair();
        Key publicKey =  keyPair.getPublic();
        Key privateKey = keyPair.getPrivate();

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(data.toString().getBytes());

        System.out.println("Message digest (MD5): " + DatatypeConverter.printBase64Binary(digest));
        System.out.println();
        // TODO: compare the length of file smallSize.txt and largeSize.txt

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE,  privateKey);
        byte[] encryptedBytes = cipher.doFinal(digest);

        System.out.println("Encrypted digital signature: "
                + DatatypeConverter.printBase64Binary(encryptedBytes));
        // Create RSAcipher object and initialize it as decrypt mode, use PUBLIC key.
        Cipher dcipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        dcipher.init(Cipher.DECRYPT_MODE,  publicKey);

        byte[] decryptedBytes = dcipher.doFinal(encryptedBytes);
        System.out.println("Decrypted digital signature: "
                + DatatypeConverter.printBase64Binary(decryptedBytes));
        System.out.println( "Original digest byte length: " + decryptedBytes.length);
        System.out.println( "Signed digest byte length: " + encryptedBytes.length);


    }

}