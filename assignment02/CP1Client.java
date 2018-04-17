import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.security.cert.*;
import java.util.Arrays;

public class CP1Client {
    private static final String CA_CERT_FILE = "files/CA.crt";
    private static final String SERVER_NAME = "localhost";
    private static final Integer SERVER_PORT = 4321;
    private static final Integer BLOCK_SIZE = 117;

    private static Socket clientSocket;
    private static DataOutputStream toServer = null;
    private static DataInputStream fromServer = null;

    public static void main(String[] args) {
        try {
            connectToServer(SERVER_NAME, SERVER_PORT);
            PublicKey serverKey = authenticateServerAndGetKey();
            if (serverKey == null)
                terminateConnection();

            transferEncryptedFile("files/rr.txt", serverKey);

        } finally {
            terminateConnection();
        }
    }

    private static void transferEncryptedFile(String fileLocation, PublicKey serverKey) {
        try {
            boolean transferReady = checkMessage(APConstants.TRANSFER_READY);
            if (!transferReady) {
                System.out.println("CP1Server not ready to receive file.");
                return;
            }

            System.out.println("Transferring file...");
            File file = new File(fileLocation);

            /* SEND TRANSFER START MESSAGE */
            System.out.println(" > Sending the file over...");
            writeBytesToServer(APConstants.TRANFER_START.getBytes());

            boolean transferAccept = checkMessage(APConstants.TRANSFER_ACCEPT);
            if (!transferAccept) {
                System.out.println("CP1Server rejected file transfer.");
                return;
            }

            /* SEND FILE NAME */
            toServer.writeUTF(file.getName());
            toServer.flush();

            boolean transferCont = checkMessage(APConstants.TRANSFER_CONTINUE);
            if (!transferCont) {
                System.out.println("CP1Server stopped the file transfer.");
                return;
            }

            /* SEND ENCRYPTED FILE and MESSAGE DIGEST */
            byte[] fileBytes = new byte[(int) file.length()];
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            int bytesRead = bis.read(fileBytes, 0, fileBytes.length);
            if (bytesRead < 0)
                throw new IOException("Input stream ended prematurely.");
            bis.close();

            sendEncryptedMessageDigest(fileBytes, serverKey);
            writeBytesToServer(APConstants.TRANSFER_MD_DONE.getBytes());
            sendEncryptedFileBytes(fileBytes, serverKey);
            writeBytesToServer(APConstants.TRANSFER_DONE.getBytes());

            int length = fromServer.readInt();
            byte[] serverMsg = new byte[length];
            fromServer.readFully(serverMsg);

            if (Arrays.equals(serverMsg, APConstants.FILE_PROBLEM.getBytes())) {
                System.out.println("File is corrupted during transfer. Please try again.");
                return;
            }

            if (!Arrays.equals(serverMsg, APConstants.TRANSFER_RECEIVED.getBytes())) {
                System.out.println("CP1Server did not receive the file. Please try again.");
                return;
            }

            System.out.println(" >> File sent.");
            System.out.println("File transfer complete!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendEncryptedFileBytes(byte[] fileBytes, PublicKey serverKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, serverKey);

            for (int start = 0, end; start < fileBytes.length; start += BLOCK_SIZE) {
                end = Math.min(start + BLOCK_SIZE, fileBytes.length);
                byte[] tempBlockBuffer = cipher.doFinal(fileBytes, start, end - start);
                writeBytesToServer(tempBlockBuffer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendEncryptedMessageDigest(byte[] fileBytes, PublicKey serverKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(fileBytes);
            byte[] digest = md.digest();

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, serverKey);
            byte[] encryptedDigest = cipher.doFinal(digest);
            writeBytesToServer(encryptedDigest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void connectToServer(String serverName, int serverPort) {
        System.out.println("Connecting to " + serverName + ":" + serverPort + "...");

        try {
            clientSocket = new Socket(serverName, serverPort);
            toServer = new DataOutputStream(clientSocket.getOutputStream());
            fromServer = new DataInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static PublicKey authenticateServerAndGetKey() {
        System.out.println("Authenticating server...");

        try {
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
                System.out.println("Authentication failed - invalid nonce response.");
                return null;
            }

            /* VERIFY SERVER CERTIFICATE */
            boolean isVerifiedCert = verifyServerCert(serverCert);
            if (!isVerifiedCert) {
                System.out.println("Authentication failed - invalid server certificate.");
                return null;
            }

            System.out.println("CP1Server is authenticated!");
            writeBytesToServer(APConstants.AUTH_DONE.getBytes());
            return serverKey;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
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
        int bytesRead = fromServer.read(serverReply);
        if (bytesRead < 0)
            throw new IOException("Data stream ended prematurely.");

        if (Arrays.equals(serverReply, APConstants.TERMINATION_MSG.getBytes())) {
            System.out.println("CP1Server terminated connection.");
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

    private static void writeBytesToServer(byte[] array) throws IOException {
        toServer.writeInt(array.length);
        toServer.flush();
        toServer.write(array);
        toServer.flush();
    }

    private static boolean checkMessage(String message) throws IOException {
        int length = fromServer.readInt();
        byte[] serverMsg = new byte[length];
        fromServer.readFully(serverMsg);

        return Arrays.equals(serverMsg, message.getBytes());
    }
}
