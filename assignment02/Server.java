
import javax.crypto.Cipher;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final String PRIVATE_DER_FILE = "files/privateServer.der";
    private static final String SERVER_CERT_FILE = "files/server.crt";
    private static final Integer SERVER_PORT = 4321;

    private static final ExecutorService pool = Executors.newCachedThreadPool();

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            byte[] privateKeyByteArray = Files.readAllBytes(Paths.get(PRIVATE_DER_FILE));
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyByteArray);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            while (true) {
                System.out.println("Awaiting new client to connect...");
                Socket clientSocket = serverSocket.accept();
                // start new thread to do auth protocol
                System.out.println("One client connected. Starting new thread to handle client.");
                pool.execute(() -> {
                    DataOutputStream toClient = null;
                    DataInputStream fromClient = null;
                    try {
                        toClient = new DataOutputStream(clientSocket.getOutputStream());
                        fromClient = new DataInputStream(clientSocket.getInputStream());

                        boolean authDone = startAuthProtocol(toClient, fromClient, privateKey);
                        if (!authDone)
                            terminateConnection(toClient, fromClient, clientSocket);

                        startFileTransfer(toClient, fromClient, privateKey);

                    } catch (IOException e) {
                        System.out.println("Thread " + Thread.currentThread() + ": " + e.getMessage());
                    } finally {
                        terminateConnection(toClient, fromClient, clientSocket);
                    }
                });
            }

        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /* FILE */
    private static void startFileTransfer(DataOutputStream toClient, DataInputStream fromClient,
                                          PrivateKey privateKey) {
        try {
            // System.out.println("Thread " + Thread.currentThread().getId() + " > Receiving file...");
            boolean transferStart = checkMessage(APConstants.TRANFER_START, fromClient);
            if (!transferStart) {
                System.out.println("Client is not transferring file.");
            }

            writeBytesToClient(APConstants.TRANSFER_ACCEPT.getBytes(), toClient);

            String fileName = fromClient.readUTF();
            byte[] fileBytes = receiveAndDecryptBytes(toClient, fromClient, privateKey);
            if (fileBytes == null)
                throw new NullPointerException("Decrypted file bytes should not be null.");
            // System.out.println("Thread " + Thread.currentThread().getId() + " >> File received.");

            // System.out.println("Thread " + Thread.currentThread().getId() + " > Writing file.");
            Files.write(Paths.get("server", fileName), fileBytes);
            // System.out.println("Thread " + Thread.currentThread().getId() + " >> File written.");

            writeBytesToClient(APConstants.TRANSFER_RECEIVED.getBytes(), toClient);
            System.out.println("Thread " + Thread.currentThread().getId() + " - File transfer complete!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] receiveAndDecryptBytes(DataOutputStream toClient, DataInputStream fromClient,
                                                 PrivateKey privateKey) {
        try {
            writeBytesToClient(APConstants.TRANSFER_CONTINUE.getBytes(), toClient);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesLen;
            byte[] tempBuf;
            while (true) {
                bytesLen = fromClient.readInt();
                tempBuf = new byte[bytesLen];
                fromClient.readFully(tempBuf);
                if (Arrays.equals(tempBuf, APConstants.TRANSFER_DONE.getBytes())) {
                    break;
                }

                tempBuf = cipher.doFinal(tempBuf);
                baos.write(tempBuf, 0, tempBuf.length);
            }

            byte[] fileBytes = baos.toByteArray();
            baos.close();
            return fileBytes;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /* AUTH */
    private static boolean startAuthProtocol(DataOutputStream toClient, DataInputStream fromClient,
                                             PrivateKey privateKey) throws IOException {
        /* RECEIVE CLIENT NONCE */
        // System.out.println("Thread " + Thread.currentThread().getId() + " > Receiving nonce...");
        int clientNonceLength = fromClient.readInt();
        byte[] clientNonce = new byte[clientNonceLength];
        fromClient.readFully(clientNonce);
        // System.out.println("Thread " + Thread.currentThread().getId() + " >> Client nonce received.");

        /* ENCRYPT NONCE AND SEND BACK */
        // System.out.println("Thread " + Thread.currentThread().getId() + " > Encrypting client nonce...");
        byte[] encryptedNonce = encryptNonce(clientNonce, privateKey);
        if (encryptedNonce == null)
            throw new NullPointerException("Encrypted nonce should not be null.");
        writeBytesToClient(encryptedNonce, toClient);
        // System.out.println("Thread " + Thread.currentThread().getId() + " >> Sent encrypted nonce back to client.");

        /* LISTEN FOR SIGNED CERT REQUEST AND SEND SIGNED CERT */
        // System.out.println("Thread " + Thread.currentThread().getId() + " > Waiting to send signed certificate...");
        boolean isValidRequest = readRequest(fromClient);
        if (!isValidRequest) return false;
        sendSignedCert(toClient);
        // System.out.println("Thread " + Thread.currentThread().getId() + " >> Signed certificate sent.");

        /* LISTEN FOR PROCEED CALL */
        int replyLength = fromClient.readInt();
        byte[] reply = new byte[replyLength];
        fromClient.readFully(reply);

        if (!Arrays.equals(reply, APConstants.AUTH_DONE.getBytes())) {
            System.out.println("Thread " + Thread.currentThread().getId() +
                    " - Client terminated connection.");
            return false;
        }

        System.out.println("Thread " + Thread.currentThread().getId() + " - Authentication complete!");
        writeBytesToClient(APConstants.TRANSFER_READY.getBytes(), toClient);
        return true;
    }

    private static byte[] encryptNonce(byte[] clientNonce, PrivateKey privateKey) {
        try {

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(clientNonce);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean readRequest(DataInputStream fromClient) throws IOException {
        int byteLength = fromClient.readInt();
        byte[] request = new byte[byteLength];
        fromClient.readFully(request);

        if (!Arrays.equals(request, APConstants.REQ_SIGNED_CERT.getBytes())) {
            System.out.println("Invalid request. Terminating connection.");
            return false;
        }

        return true;
    }

    private static void sendSignedCert(DataOutputStream toClient) {
        try {
            byte[] serverCertBytes = Files.readAllBytes(Paths.get(SERVER_CERT_FILE));
            writeBytesToClient(serverCertBytes, toClient);
        } catch (FileNotFoundException ex) {
            System.out.println("Server certificate not found.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void terminateConnection(DataOutputStream toClient, DataInputStream fromClient,
                                            Socket clientSocket) {
        try {
            System.out.println("Thread " + Thread.currentThread().getId() + " - Terminating connection to client.");
            if (toClient != null) {
                writeBytesToClient(APConstants.TERMINATION_MSG.getBytes(), toClient);
                toClient.close();
            }

            if (fromClient != null)
                fromClient.close();

            clientSocket.close();
        } catch (SocketException ignored) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeBytesToClient(byte[] array, DataOutputStream toClient)
            throws IOException {
        toClient.writeInt(array.length);
        toClient.flush();
        toClient.write(array);
        toClient.flush();
    }

    private static boolean checkMessage(String message, DataInputStream fromClient)
            throws IOException {
        int length = fromClient.readInt();
        byte[] clientMsg = new byte[length];
        fromClient.readFully(clientMsg);

        return Arrays.equals(clientMsg, message.getBytes());
    }
}
