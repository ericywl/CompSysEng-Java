import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;
import java.nio.*;
import java.util.Arrays;
import javax.crypto.*;

public class DESImage {
    private static final String inFileName = "triangle.bmp";
    private static final String encryptionMode = "CBC";

    public static void main(String[] args) throws Exception {
        int imageWidth = 200;
        int imageLength = 200;
        // read image file and save pixel value into int[][] imageArray
        File imgFile = new File("lab06_encryption/files/" + inFileName);
        BufferedImage img = ImageIO.read(imgFile);
        System.out.println("Reading " + imgFile.getName() + "...");
        imageWidth = img.getWidth();
        imageLength = img.getHeight();
        // byte[][] imageArray = new byte[image_width][image_length];
        int[][] imageArray = new int[imageWidth][imageLength];
        for (int idx = 0; idx < imageWidth; idx++) {
            for (int idy = 0; idy < imageLength; idy++) {
                int color = img.getRGB(idx, idy);
                imageArray[idx][idy] = color;
            }
        }

        // generate secret key using DES algorithm
        System.out.println("Encrypting " + imgFile.getName() + "...");
        SecretKey key = KeyGenerator.getInstance("DES").generateKey();
        // TODO: you need to try both ECB and CBC mode, use PKCS5Padding padding method
        Cipher cipher = Cipher.getInstance("DES/" + encryptionMode + "/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        // define output BufferedImage, set size and format
        BufferedImage outImage = new BufferedImage(imageWidth, imageLength, BufferedImage.TYPE_3BYTE_BGR);

        for (int idx = 0; idx < imageWidth; idx++) {
            // convert each column int[] into a byte[] (each_width_pixel)
            byte[] eachWidthPixel = new byte[4 * imageLength];
            for (int idy = 0; idy < imageLength; idy++) {
                ByteBuffer dbuf = ByteBuffer.allocate(4);
                dbuf.putInt(imageArray[idx][idy]);
                byte[] bytes = dbuf.array();
                System.arraycopy(bytes, 0, eachWidthPixel, idy * 4, 4);
            }

            // encrypt each column or row bytes
            byte[] encryptedByteArr = cipher.doFinal(eachWidthPixel);
            // convert the encrypted byte[] back into int[] and write to outImage (use setRGB)
            for (int idy = 0; idy < imageLength; idy++) {
                byte[] buf = Arrays.copyOfRange(encryptedByteArr, idy * 4, idy * 4 + 4);
                ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
                int newColor = byteBuffer.getInt();
                outImage.setRGB(idx, idy, newColor);
            }
        }

        // write outImage into file
        File outImgFile = new File("lab06_encryption/files/" + encryptionMode + "_" + "En" + inFileName);
        ImageIO.write(outImage, "BMP", outImgFile);
        System.out.println(imgFile.getName() + " encrypted to " + outImgFile.getName() + ".");
    }
}