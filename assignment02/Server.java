import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
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
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
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
            int length = fromClient.readInt();
            byte[] clientMsg = new byte[length];
            fromClient.readFully(clientMsg);

            String protocol;
            Key fileDecKey;
            Cipher cipher;
            if (Arrays.equals(clientMsg, APConstants.TRANSFER_START_CP1.getBytes())) {
                protocol = "cp1";

                fileDecKey = privateKey;
                cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            } else if (Arrays.equals(clientMsg, APConstants.TRANSFER_START_CP2.getBytes())) {
                protocol = "cp2";

                length = fromClient.readInt();
                byte[] encryptedKeyBytes = new byte[length];
                fromClient.readFully(encryptedKeyBytes);

                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
                byte[] aesKeyBytes = rsaCipher.doFinal(encryptedKeyBytes);
                fileDecKey = new SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            } else {
                System.out.println("Thread " + Thread.currentThread().getId() +
                        " - Error! Client is not transferring file.");
                return;
            }

            cipher.init(Cipher.DECRYPT_MODE, fileDecKey);
            writeBytesToClient(APConstants.TRANSFER_ACCEPT.getBytes(), toClient);

            byte[] fileNameBytes = receiveAndDecryptBytes(fromClient, cipher);
            if (fileNameBytes == null)
                throw new NullPointerException("Decrypted file name should not be null.");
            String fileName = new String(fileNameBytes);
            writeBytesToClient(APConstants.TRANSFER_CONTINUE.getBytes(), toClient);

            byte[] fileBytes = receiveAndDecryptBytes(fromClient, cipher);
            if (fileBytes == null)
                throw new NullPointerException("Decrypted file should not be null.");

            byte[] messageDigest = receiveAndDecryptDigest(fromClient, cipher);
            if (messageDigest == null)
                throw new NullPointerException("Decrypted message digest should not be null.");
            boolean transferMDDone = checkMessage(APConstants.TRANSFER_MD_DONE, fromClient);
            if (!transferMDDone) {
                System.out.println("Thread " + Thread.currentThread().getId()
                        + " - Error! Client did not transfer message digest.");
                return;
            }

            boolean sameDigest = generateAndCompareDigest(fileBytes, messageDigest);
            if (!sameDigest) {
                System.out.println("Thread " + Thread.currentThread().getId() + " - Error! File corrupted.");
                writeBytesToClient(APConstants.FILE_PROBLEM.getBytes(), toClient);
                return;
            }

            try {
                Files.createDirectory(Paths.get(protocol + "server"));
                Files.createFile(Paths.get(protocol + "server", fileName));
            } catch (FileAlreadyExistsException ignored) {
            }

            Files.write(Paths.get(protocol + "server", fileName), fileBytes);

            writeBytesToClient(APConstants.TRANSFER_RECEIVED.getBytes(), toClient);
            System.out.println("Thread " + Thread.currentThread().getId() + " - File transfer complete!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Receive blocks, decrypt them and form the complete bytes
     *
     * @param fromClient the data input stream
     * @return the decrypted bytes
     */
    private static byte[] receiveAndDecryptBytes(DataInputStream fromClient, Cipher cipher) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesLen;
            byte[] tempBuf;
            while (true) {
                bytesLen = fromClient.readInt();
                tempBuf = new byte[bytesLen];
                fromClient.readFully(tempBuf);
                if (Arrays.equals(tempBuf, APConstants.TRANSFER_FILE_DONE.getBytes())
                        || Arrays.equals(tempBuf, APConstants.TRANSFER_NAME_DONE.getBytes())) {
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

    /**
     * Receive the encrypted digest and decrypt it
     *
     * @param fromClient the data input stream
     * @return the decrypted message digest bytes
     */
    private static byte[] receiveAndDecryptDigest(DataInputStream fromClient, Cipher cipher) {
        try {
            int length = fromClient.readInt();
            byte[] encryptedDigest = new byte[length];
            fromClient.readFully(encryptedDigest);

            return cipher.doFinal(encryptedDigest);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Generate the message digest individually and compare with the received
     *
     * @param fileBytes      the file bytes used to compute the digest
     * @param receivedDigest the received message digest
     * @return true if equals, else false
     */
    private static boolean generateAndCompareDigest(byte[] fileBytes, byte[] receivedDigest) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(fileBytes);
            byte[] digest = md.digest();
            return Arrays.equals(digest, receivedDigest);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return false;
    }

    /* AUTH */
    private static boolean startAuthProtocol(DataOutputStream toClient, DataInputStream fromClient,
                                             PrivateKey privateKey) throws IOException {
        /* RECEIVE CLIENT NONCE */
        int clientNonceLength = fromClient.readInt();
        byte[] clientNonce = new byte[clientNonceLength];
        fromClient.readFully(clientNonce);

        /* ENCRYPT NONCE AND SEND BACK */
        byte[] encryptedNonce = encryptNonce(clientNonce, privateKey);
        if (encryptedNonce == null)
            throw new NullPointerException("Encrypted nonce should not be null.");
        writeBytesToClient(encryptedNonce, toClient);

        /* LISTEN FOR SIGNED CERT REQUEST AND SEND SIGNED CERT */
        boolean isValidRequest = checkMessage(APConstants.REQ_SIGNED_CERT, fromClient);
        if (!isValidRequest) {
            System.out.println("Thread " + Thread.currentThread().getId() + " - Error! Invalid request.");
            return false;
        }
        sendSignedCert(toClient);

        /* LISTEN FOR PROCEED CALL */
        int replyLength = fromClient.readInt();
        byte[] reply = new byte[replyLength];
        fromClient.readFully(reply);

        if (!Arrays.equals(reply, APConstants.AUTH_DONE.getBytes())) {
            System.out.println("Thread " + Thread.currentThread().getId() +
                    " - Error! Client terminated connection.");
            return false;
        }

        System.out.println("Thread " + Thread.currentThread().getId() + " - Authentication complete!");
        writeBytesToClient(APConstants.TRANSFER_READY.getBytes(), toClient);
        return true;
    }

    /**
     * Encrypt the nonce sent by the client
     *
     * @param clientNonce the nonce to be encrypted
     * @param privateKey  the private key to use for encryption
     * @return the encrypted nonce
     */
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

    private static void sendSignedCert(DataOutputStream toClient) {
        try {
            byte[] serverCertBytes = Files.readAllBytes(Paths.get(SERVER_CERT_FILE));
            writeBytesToClient(serverCertBytes, toClient);
        } catch (FileNotFoundException ex) {
            System.out.println("Error! Server certificate not found.");
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
