/* Lab 2
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 08/02/2017
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    public static List<Integer> parseFile(File inputFile) {
        // parse input file into an array of integers
        List<Integer> array = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                for (String numStr : line.split("\\s")) {
                    if (!numStr.isEmpty())
                        array.add(Integer.valueOf(numStr.trim()));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return array;
    }

    public static List<List<Integer>> createSubArrays(List<Integer> array, int numOfThreads) {
        // split the huge array of integers into sub arrays
        List<List<Integer>> subArrList = new ArrayList<>();
        int arraySize = array.size();
        int subArrLen = (numOfThreads == 1) ? arraySize : (arraySize / numOfThreads) + 1;
        for (int i = 0; i < numOfThreads; i++) {
            List<Integer> tempList = array.subList(i * subArrLen, Math.min((i + 1) * subArrLen, arraySize));
            subArrList.add(tempList);
        }

        return subArrList;
    }
}
