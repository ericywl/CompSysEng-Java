import javax.crypto.Cipher;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

public class Server {
    private static final String PRIVATE_DER_FILE = "files/privateServer.der";
    private static final String SERVER_CERT_FILE = "files/server.crt";
    private static final Integer SERVER_PORT = 4321;

    private static ServerSocket serverSocket;
    private static Socket clientSocket = null;
    private static DataOutputStream toClient;
    private static DataInputStream fromClient;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            while (clientSocket == null) {
                acceptConnection();
                boolean authDone = startAuthProtocol();
                if (!authDone) {
                    terminateConnection();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void acceptConnection() throws IOException {
        System.out.println("Awaiting client to connect...");

        clientSocket = serverSocket.accept();
        toClient = new DataOutputStream(clientSocket.getOutputStream());
        fromClient = new DataInputStream(clientSocket.getInputStream());
    }

    private static boolean startAuthProtocol() throws IOException {
        System.out.println("Client connected. Starting authentication protocol...");

        /* RECEIVE CLIENT NONCE */
        System.out.println(" > Receiving nonce...");
        int clientNonceLength = fromClient.readInt();
        byte[] clientNonce = new byte[clientNonceLength];
        int bytesRead = fromClient.read(clientNonce);
        if (bytesRead < 0)
            throw new IOException("Data stream ended prematurely.");
        System.out.println(" >> Client nonce received.");

        /* ENCRYPT NONCE AND SEND BACK */
        System.out.println(" > Encrypting client nonce...");
        byte[] encryptedNonce = encryptNonce(clientNonce);
        if (encryptedNonce == null)
            throw new NullPointerException("Encrypted nonce should not be null.");
        toClient.writeInt(encryptedNonce.length);
        toClient.flush();

        toClient.write(encryptedNonce);
        toClient.flush();
        System.out.println(" >> Sent encrypted nonce back to client.");

        /* LISTEN FOR SIGNED CERT REQUEST AND SEND SIGNED CERT */
        System.out.println(" > Waiting to send signed certificate...");
        boolean isValidRequest = readRequest();
        if (!isValidRequest) return false;
        sendSignedCert();
        System.out.println(" >> Signed certificate sent.");

        /* LISTEN FOR PROCEED CALL */
        int replyLength = fromClient.readInt();
        byte[] reply = new byte[replyLength];
        bytesRead = fromClient.read(reply);
        if (bytesRead < 0)
            throw new IOException("Data stream ended prematurely.");

        if (!Arrays.equals(reply, APConstants.AUTH_DONE.getBytes())) {
            System.out.println("Client terminated connection.");
            return false;
        }

        System.out.println("Authentication complete.");
        return true;
    }

    private static byte[] encryptNonce(byte[] clientNonce) {
        try {
            byte[] privateKeyByteArray = Files.readAllBytes(Paths.get(PRIVATE_DER_FILE));
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyByteArray);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(clientNonce);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean readRequest() throws IOException {
        int byteLength = fromClient.readInt();
        byte[] request = new byte[byteLength];

        int bytesRead = fromClient.read(request);
        if (bytesRead < 0)
            throw new IOException("Data stream ended prematurely.");

        if (!Arrays.equals(request, APConstants.REQ_SIGNED_CERT.getBytes())) {
            System.out.println("Invalid request. Terminating connection.");
            return false;
        }

        return true;
    }

    private static void sendSignedCert() {
        try {
            File serverCertFile = new File(SERVER_CERT_FILE);
            byte[] serverCertBytes = new byte[(int) serverCertFile.length()];

            BufferedInputStream certFileInput = new BufferedInputStream(new FileInputStream(serverCertFile));
            int bytesRead = certFileInput.read(serverCertBytes, 0, serverCertBytes.length);
            certFileInput.close();
            if (bytesRead < 0)
                throw new IOException("Input stream ended prematurely.");

            toClient.writeInt(serverCertBytes.length);
            toClient.flush();

            toClient.write(serverCertBytes);
            toClient.flush();
        } catch (FileNotFoundException ex) {
            System.out.println("Server certificate not found.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void terminateConnection() throws IOException {
        toClient.writeInt(APConstants.TERMINATION_MSG.getBytes().length);
        toClient.flush();

        toClient.write(APConstants.TERMINATION_MSG.getBytes());
        toClient.flush();

        toClient.close();
        fromClient.close();
        clientSocket.close();
    }
}
