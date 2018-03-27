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

    private static PublicKey serverKey = null;

    public static void main(String[] args) {
        try {
            connectToServer(SERVER_NAME, SERVER_PORT);
            boolean isAuthServer = authenticateServer();
            if (isAuthServer) {
                System.out.println(serverKey.getFormat());
            }

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

    private static boolean authenticateServer() throws IOException {
        System.out.println("Authenticating server...");

        /* SEND NONCE */
        System.out.println(" > Sending nonce...");
        byte[] clientNonce = generateNonce();
        byte[] nonceResponse = exchangeNonce(clientNonce);
        System.out.println(" > Encrypted nonce response received.");

        /* REQUEST FOR SIGNED CERTIFICATE */
        System.out.println(" > Requesting signed certificate...");
        byte[] serverSignedCert = retrieveSignedCert();
        System.out.println(" > Signed certificate received.");

        /* VERIFY SERVER CERTIFICATE AND GET PUBLIC KEY */
        X509Certificate serverCert = null;
        try {
            CertificateFactory certFac = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bins = new ByteArrayInputStream(serverSignedCert);
            serverCert = (X509Certificate) certFac.generateCertificate(bins);
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        if (serverCert == null)
            throw new NullPointerException("Server certificate should not be null.");
        boolean isVerifiedCert = verifyServerCert(serverCert);
        if (!isVerifiedCert) {
            System.out.println("Authentication failed - invalid server certificate.");
            terminate();
            return false;
        }

        /* VERIFY NONCE */
        serverKey = serverCert.getPublicKey();
        byte[] decryptedNonceResponse = decryptNonceResponse(nonceResponse, serverKey);
        if (!Arrays.equals(clientNonce, decryptedNonceResponse)) {
            System.out.println("Authentication failed - invalid nonce response.");
            terminate();
            return false;
        }

        System.out.println("Server is authenticated!");
        return true;
    }

    private static byte[] exchangeNonce(byte[] nonce) throws IOException {
        if (nonce == null)
            throw new NullPointerException("Nonce should not be null.");
        toServer.write(nonce);
        toServer.flush();

        int bytesRead;
        byte[] nonceResponse = new byte[64];
        bytesRead = fromServer.read(nonceResponse);
        if (bytesRead < 0)
            throw new IOException("Data stream ended prematurely.");

        return nonceResponse;
    }

    private static byte[] retrieveSignedCert() throws IOException {
        toServer.write(APConstants.REQ_SIGNED_CERT.getBytes());
        toServer.flush();

        int certSize = fromServer.readInt();
        byte[] serverSignedCert = new byte[certSize];
        int bytesRead = fromServer.read(serverSignedCert);
        if (bytesRead < 0)
            throw new IOException("Data stream ended prematurely.");

        return serverSignedCert;
    }

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

    private static void terminate() throws IOException {
        toServer.write(APConstants.TERMINATION_MSG.getBytes());
        toServer.flush();
        toServer.close();
        fromServer.close();
        clientSocket.close();
    }
}
