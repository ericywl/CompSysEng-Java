import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class TestFuzzer {
    private static final String alnum = "abcdefghijklmnopqrstuvwxyz1234567890";
    public static void main(String[] args) {
        StringBuilder strBld = new StringBuilder();
        for (int i = 0; i < 40000; i++) {
            for (int j = 0; j < 100; j++) {
                int num = new Random().nextInt(2);
                int index = new Random().nextInt(alnum.length());
                if (num == 0) {
                    strBld.append(alnum.substring(index, index + 1));
                } else {
                    strBld.append(alnum.substring(index, index + 1).toUpperCase());
                }
            }

            strBld.append("\n");
        }

        try {
            Files.write(Paths.get("files", "test_e.txt"), strBld.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
