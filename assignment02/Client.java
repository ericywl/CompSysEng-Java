import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.*;
import java.util.Arrays;

public class Client {
    private static final String CA_CERT_FILE = "files/CA.crt";
    private static final String SERVER_NAME = "localhost";
    private static final Integer SERVER_PORT = 4321;

    private static Socket clientSocket;
    private static DataOutputStream toServer;
    private static DataInputStream fromServer;

    public static void main(String[] args) throws Exception {
        try {
            connectToServer(SERVER_NAME, SERVER_PORT);
            boolean isAuthServer = authenticateServer();
            if (!isAuthServer) {
                terminateConnection();
                return;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                terminateConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void connectToServer(String serverName, int serverPort) throws IOException {
        System.out.println("Connecting to " + serverName + ":" + serverPort + "...");

        clientSocket = new Socket(serverName, serverPort);
        toServer = new DataOutputStream(clientSocket.getOutputStream());
        fromServer = new DataInputStream(clientSocket.getInputStream());
    }

    private static boolean authenticateServer() throws IOException {
        System.out.println("Authenticating server...");

        /* SEND NONCE */
        System.out.println(" > Sending nonce...");
        byte[] clientNonce = generateNonce();
        byte[] encryptedNonceResponse = exchangeNonceWithServer(clientNonce);
        System.out.println(" >> Encrypted nonce response received.");

        /* REQUEST FOR SIGNED CERTIFICATE */
        System.out.println(" > Requesting signed certificate...");
        X509Certificate serverCert = retrieveSignedCert();
        if (serverCert == null) return false;
        System.out.println(" >> Signed certificate received.");

        /* VERIFY SERVER CERTIFICATE AND GET PUBLIC KEY */
        boolean isVerifiedCert = verifyServerCert(serverCert);
        if (!isVerifiedCert) {
            System.out.println("Authentication failed - invalid server certificate.");
            return false;
        }
        PublicKey serverKey = serverCert.getPublicKey();

        /* VERIFY NONCE */
        byte[] decryptedNonceResponse = decryptNonceResponse(encryptedNonceResponse, serverKey);
        if (!Arrays.equals(clientNonce, decryptedNonceResponse)) {
            System.out.println("Authentication failed - invalid nonce response.");
            return false;
        }

        /* AUTHENTICATION COMPLETE */
        toServer.writeInt(APConstants.AUTH_DONE.getBytes().length);
        toServer.flush();

        toServer.write(APConstants.AUTH_DONE.getBytes());
        toServer.flush();
        System.out.println("Server is authenticated!");
        return true;
    }

    private static byte[] exchangeNonceWithServer(byte[] nonce) throws IOException {
        if (nonce == null)
            throw new NullPointerException("Nonce should not be null.");

        toServer.writeInt(nonce.length);
        toServer.flush();

        toServer.write(nonce);
        toServer.flush();

        int responseLength = fromServer.readInt();
        byte[] encryptedNonceResponse = new byte[responseLength];
        int bytesRead = fromServer.read(encryptedNonceResponse);
        if (bytesRead < 0)
            throw new IOException("Data stream ended prematurely.");

        return encryptedNonceResponse;
    }

    /**
     * Request the server to send it's signed certificate
     * @return server's signed X509Certificate
     * @throws IOException if data stream ends prematurely
     */
    private static X509Certificate retrieveSignedCert() throws IOException {
        toServer.writeInt(APConstants.REQ_SIGNED_CERT.getBytes().length);
        toServer.flush();

        toServer.write(APConstants.REQ_SIGNED_CERT.getBytes());
        toServer.flush();

        int replySize = fromServer.readInt();
        byte[] serverReply = new byte[replySize];

        int bytesRead = fromServer.read(serverReply);
        if (bytesRead < 0)
            throw new IOException("Data stream ended prematurely.");

        if (Arrays.equals(serverReply, APConstants.TERMINATION_MSG.getBytes())) {
            System.out.println("Server terminated connection.");
            return null;
        }

        try {
            CertificateFactory certFac = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bins = new ByteArrayInputStream(serverReply);
            return (X509Certificate) certFac.generateCertificate(bins);
        } catch (CertificateException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verify the server's certificate with the CA certificate
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
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Decrypt the nonce response obtained from the server
     * @param nonceResponse the bytes of the encrypted nonce response from server
     * @param serverKey the server's public key obtained from the server's certificate
     * @return the decrypted nonce response
     */
    private static byte[] decryptNonceResponse(byte[] nonceResponse, PublicKey serverKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, serverKey);
            return cipher.doFinal(nonceResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate a nonce for use in validating the server
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
            return null;
        }
    }

    /**
     * Terminate the connection (invalid server or finished transfer)
     * @throws IOException if any of the statements throw IOException
     */
    private static void terminateConnection() throws IOException {
        toServer.writeInt(APConstants.TERMINATION_MSG.getBytes().length);
        toServer.flush();

        toServer.write(APConstants.TERMINATION_MSG.getBytes());
        toServer.flush();

        toServer.close();
        fromServer.close();
        clientSocket.close();
    }
}
