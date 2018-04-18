import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    private static final String CA_CERT_FILE = "files/CA.crt";
    private static final Integer SERVER_PORT = 4321;
    private static final Integer BLOCK_SIZE = 117;
    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private static Socket clientSocket;
    private static DataOutputStream toServer = null;
    private static DataInputStream fromServer = null;

    public static void main(String[] args) {
        String IPAddr = args[0];
        String fileLocation = args[1];
        String protocol = args[2];

        boolean validIP = checkIP(IPAddr);
        if (!validIP) {
            System.out.println("The specified IPv4 address is invalid.");
            return;
        }

        boolean fileExists = Files.exists(Paths.get(fileLocation));
        if (!fileExists) {
            System.out.println("The specified file does not exist.");
            return;
        }

        boolean isFile = Files.isRegularFile(Paths.get(fileLocation));
        if (!isFile) {
            System.out.println("The specified path is not a file.");
            return;
        }

        if (!protocol.equalsIgnoreCase("cp1") && !protocol.equalsIgnoreCase("cp2")) {
            System.out.println("Invalid protocol.");
            return;
        }

        try {
            connectToServer(IPAddr, SERVER_PORT);
            PublicKey serverKey = authenticateServerAndGetKey();
            if (serverKey == null)
                terminateConnection();

            long start = System.nanoTime();
            transferFile(fileLocation, serverKey, protocol);
            long end = System.nanoTime();
            long duration = end - start;
            System.out.println("Time taken for file transfer: " + duration + "ns.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            terminateConnection();
        }

    }

    private static boolean checkIP(String input) {
        if (input.equals("localhost")) return true;

        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(input);

        return matcher.matches();
    }

    /* FILE */
    private static void transferFile(String fileLocation, PublicKey publicKey, String protocol) {
        try {
            boolean transferReady = checkMessage(APConstants.TRANSFER_READY);
            if (!transferReady) {
                System.out.println("Server not ready to receive file.");
                return;
            }

            System.out.println("Transferring file...");
            File file = new File(fileLocation);

            /* SEND TRANSFER START MESSAGE AND INIT CIPHER */
            System.out.println(" > Sending the file over...");
            Key fileEncKey;
            Cipher cipher;
            if (protocol.equalsIgnoreCase("cp2")) {
                writeBytesToServer(APConstants.TRANSFER_START_CP2.getBytes());
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(128);
                fileEncKey = keyGen.generateKey();
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

                sendEncryptedAesKey(fileEncKey, publicKey);
            } else if (protocol.equalsIgnoreCase("cp1")) {
                writeBytesToServer(APConstants.TRANSFER_START_CP1.getBytes());
                fileEncKey = publicKey;
                cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            } else {
                throw new IllegalArgumentException("Invalid protocol.");
            }

            cipher.init(Cipher.ENCRYPT_MODE, fileEncKey);
            boolean transferAccept = checkMessage(APConstants.TRANSFER_ACCEPT);
            if (!transferAccept) {
                System.out.println("Error! Server rejected file transfer.");
                return;
            }

            /* SEND ENCRYPTED FILE NAME */
            sendEncryptedFileBytes(file.getName().getBytes(), cipher);
            writeBytesToServer(APConstants.TRANSFER_NAME_DONE.getBytes());
            boolean transferCont = checkMessage(APConstants.TRANSFER_CONTINUE);
            if (!transferCont) {
                System.out.println("Error! Server stopped the file transfer.");
                return;
            }

            /* SEND ENCRYPTED FILE and MESSAGE DIGEST */
            byte[] fileBytes = new byte[(int) file.length()];
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            int bytesRead = bis.read(fileBytes, 0, fileBytes.length);
            if (bytesRead < 0)
                throw new IOException("Input stream ended prematurely.");
            bis.close();

            sendEncryptedFileBytes(fileBytes, cipher);
            writeBytesToServer(APConstants.TRANSFER_FILE_DONE.getBytes());
            sendEncryptedMessageDigest(fileBytes, cipher);
            writeBytesToServer(APConstants.TRANSFER_MD_DONE.getBytes());

            int length = fromServer.readInt();
            byte[] serverMsg = new byte[length];
            fromServer.readFully(serverMsg);

            if (Arrays.equals(serverMsg, APConstants.FILE_PROBLEM.getBytes())) {
                System.out.println("Error! File is corrupted during transfer. Please try again.");
                return;
            }

            if (!Arrays.equals(serverMsg, APConstants.TRANSFER_RECEIVED.getBytes())) {
                System.out.println("Error! Server did not receive the file. Please try again.");
                return;
            }

            System.out.println(" >> File sent.");
            System.out.println("File transfer complete!");

        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private static void sendEncryptedAesKey(Key aesKey, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] aesKeyBytes = aesKey.getEncoded();
            byte[] encryptedAesKeyBytes = cipher.doFinal(aesKeyBytes);
            writeBytesToServer(encryptedAesKeyBytes);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Split the file bytes into blocks, encrypt them and send to server
     *
     * @param fileBytes the file bytes to be split
     */
    private static void sendEncryptedFileBytes(byte[] fileBytes, Cipher cipher) {
        try {
            for (int start = 0, end; start < fileBytes.length; start += BLOCK_SIZE) {
                end = Math.min(start + BLOCK_SIZE, fileBytes.length);
                byte[] tempBlockBuffer = cipher.doFinal(fileBytes, start, end - start);
                writeBytesToServer(tempBlockBuffer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Compute the message digest, encrypt it and send it to the server
     *
     * @param fileBytes the file bytes used to compute the digest
     */
    private static void sendEncryptedMessageDigest(byte[] fileBytes, Cipher cipher) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(fileBytes);
            byte[] digestBytes = md.digest();

            byte[] encryptedDigestBytes = cipher.doFinal(digestBytes);
            writeBytesToServer(encryptedDigestBytes);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* AUTH */
    private static PublicKey authenticateServerAndGetKey() throws IOException {
        System.out.println("Authenticating server...");
        /* SEND NONCE */
        System.out.println(" > Sending nonce...");
        byte[] clientNonce = generateNonce();
        byte[] encryptedNonceResponse = exchangeNonceWithServer(clientNonce);
        System.out.println(" >> Encrypted nonce response received.");

        /* REQUEST FOR SIGNED CERTIFICATE */
        System.out.println(" > Requesting signed certificate...");
        X509Certificate serverCert = retrieveSignedCert();
        if (serverCert == null)
            return null;
        System.out.println(" >> Signed certificate received.");
        PublicKey serverKey = serverCert.getPublicKey();

        /* VERIFY NONCE */
        byte[] decryptedNonceResponse = decryptNonceResponse(encryptedNonceResponse, serverKey);
        if (!Arrays.equals(clientNonce, decryptedNonceResponse)) {
            System.out.println("Error! Authentication failed - invalid nonce response.");
            return null;
        }

        /* VERIFY SERVER CERTIFICATE */
        boolean isVerifiedCert = verifyServerCert(serverCert);
        if (!isVerifiedCert) {
            System.out.println("Error! Authentication failed - invalid server certificate.");
            return null;
        }

        System.out.println("Server is authenticated!");
        writeBytesToServer(APConstants.AUTH_DONE.getBytes());
        return serverKey;
    }

    /**
     * Send the generated nonce to the server and receive an encrypted version back
     *
     * @param nonce the generated nonce
     * @return encrypted nonce
     */
    private static byte[] exchangeNonceWithServer(byte[] nonce) throws IOException {
        if (nonce == null)
            throw new NullPointerException("Nonce should not be null.");

        writeBytesToServer(nonce);

        int responseLength = fromServer.readInt();
        byte[] encryptedNonceResponse = new byte[responseLength];
        int bytesRead = fromServer.read(encryptedNonceResponse);
        if (bytesRead < 0)
            throw new IOException("Data stream ended prematurely.");

        return encryptedNonceResponse;
    }

    /**
     * Request the server to send it's signed certificate
     *
     * @return server's signed X509Certificate
     * @throws IOException if data stream ends prematurely
     */
    private static X509Certificate retrieveSignedCert() throws IOException {
        writeBytesToServer(APConstants.REQ_SIGNED_CERT.getBytes());

        int replySize = fromServer.readInt();
        byte[] serverReply = new byte[replySize];
        fromServer.readFully(serverReply);

        if (Arrays.equals(serverReply, APConstants.TERMINATION_MSG.getBytes())) {
            System.out.println("Error! Server terminated connection.");
            return null;
        }

        try {
            CertificateFactory certFac = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bins = new ByteArrayInputStream(serverReply);
            return (X509Certificate) certFac.generateCertificate(bins);
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Verify the server's certificate with the CA certificate
     *
     * @param serverCert the certificate to be verified
     * @return true if valid certificate, else false
     */
    private static boolean verifyServerCert(X509Certificate serverCert) {
        try {
            InputStream fileInputStream = new FileInputStream(CA_CERT_FILE);
            CertificateFactory certFac = CertificateFactory.getInstance("X.509");
            X509Certificate CACert = (X509Certificate) certFac.generateCertificate(fileInputStream);
            PublicKey CAKey = CACert.getPublicKey();
            CACert.checkValidity();

            serverCert.checkValidity();
            serverCert.verify(CAKey);
            return true;

        } catch (FileNotFoundException e) {
            System.out.println("CA certificate not found.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Decrypt the nonce response obtained from the server
     *
     * @param nonceResponse the bytes of the encrypted nonce response from server
     * @param serverKey     the server's public key obtained from the server's certificate
     * @return the decrypted nonce response
     */
    private static byte[] decryptNonceResponse(byte[] nonceResponse, PublicKey serverKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, serverKey);
            return cipher.doFinal(nonceResponse);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Generate a nonce for use in validating the server
     *
     * @return the bytes of nonce
     */
    private static byte[] generateNonce() {
        try {
            byte[] nonce = new byte[64];
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.nextBytes(nonce);
            return nonce;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Terminate the connection (invalid server or finished transfer)
     */
    private static void terminateConnection() {
        try {
            System.out.println("Terminating connection to server.");
            writeBytesToServer(APConstants.TERMINATION_MSG.getBytes());

            toServer.close();
            fromServer.close();
            clientSocket.close();

        } catch (SocketException ignored) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void connectToServer(String serverName, int serverPort) throws IOException {
        System.out.println("Connecting to " + serverName + ":" + serverPort + "...");

        clientSocket = new Socket(serverName, serverPort);
        toServer = new DataOutputStream(clientSocket.getOutputStream());
        fromServer = new DataInputStream(clientSocket.getInputStream());
    }

    /**
     * Send the byte array size and the byte to the server
     *
     * @param array the array of bytes to write
     */
    private static void writeBytesToServer(byte[] array) throws IOException {
        toServer.writeInt(array.length);
        toServer.flush();
        toServer.write(array);
        toServer.flush();
    }

    /**
     * Check that the received message is equal to the specified message
     *
     * @param message the message to be compared with
     * @return true if equal, else false
     */
    private static boolean checkMessage(String message) throws IOException {
        int length = fromServer.readInt();
        byte[] serverMsg = new byte[length];
        fromServer.readFully(serverMsg);

        return Arrays.equals(serverMsg, message.getBytes());
    }
}
