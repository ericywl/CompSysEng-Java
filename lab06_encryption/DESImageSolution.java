import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;
import java.nio.*;
import javax.crypto.*;


public class DESImageSolution {
    public static void main(String[] args) throws Exception {
        int image_width = 200;
        int image_length = 200;
        // read image file and save pixel value into int[][] imageArray
        BufferedImage img = ImageIO.read(new File("SUTD.bmp"));
        image_width = img.getWidth();
        image_length = img.getHeight();
        // byte[][] imageArray = new byte[image_width][image_length];
        int[][] imageArray = new int[image_width][image_length];
        for (int idx = 0; idx < image_width; idx++) {
            for (int idy = 0; idy < image_length; idy++) {
                int color = img.getRGB(idx, idy);
                imageArray[idx][idy] = color;
            }
        }

        // generate secret key using DES algorithm
        SecretKey key = KeyGenerator.getInstance("DES").generateKey();

        // TODO: you need to try both ECB and CBC mode, use PKCS5Padding padding method
        Cipher ecipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        ecipher.init(Cipher.ENCRYPT_MODE, key);

        // define output BufferedImage, set size and format
        BufferedImage outImage = new BufferedImage(image_width, image_length, BufferedImage.TYPE_3BYTE_BGR);

        for (int idx = 0; idx < image_width; idx++) {
            // convert each column int[] into a byte[] (each_width_pixel)
            byte[] each_width_pixel = new byte[4 * image_length];
            for (int idy = 0; idy < image_length; idy++) {
                ByteBuffer dbuf = ByteBuffer.allocate(4);
                dbuf.putInt(imageArray[idx][idy]);
                byte[] bytes = dbuf.array();
                System.arraycopy(bytes, 0, each_width_pixel, idy * 4, 4);
            }
            // encrypt each column or row bytes
            byte[] encryptedImageBytes = ecipher.doFinal(each_width_pixel);

            // convert the encrypted byte[] back into int[] and write to outImage (use setRGB)
            byte[] encrypted_pixel = new byte[4];
            for (int idy = 0; idy < image_length; idy++) {
                System.arraycopy(encryptedImageBytes, idy * 4, encrypted_pixel, 0, 4);
                ByteBuffer wrapped = ByteBuffer.wrap(encrypted_pixel);
                int newColor = wrapped.getInt();
                outImage.setRGB(idx, idy, newColor);
            }
        }
        // write outImage into file
        ImageIO.write(outImage, "BMP", new File("EnSUTD.bmp"));
    }
}