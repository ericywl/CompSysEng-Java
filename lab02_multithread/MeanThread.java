/* Lab 2
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 08/02/2017
 */

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MeanThread {
    public static void main(String[] args) {
        // get input file path and number of threads from the command line arguments
        File inputFile = new File(args[0]);
        int numOfThreads = Integer.valueOf(args[1]);
        List<List<Integer>> subArrList = createSubArrays(inputFile, numOfThreads);

        // record starting time
        long startTime = System.nanoTime();

        // create and start the threads
        List<MeanMultiThread> mmtList = new ArrayList<>();
        for (List<Integer> subArr : subArrList) {
            MeanMultiThread mmt = new MeanMultiThread(subArr);
            mmt.start();
            mmtList.add(mmt);
        }

        // join the threads to main and add to temporary mean list
        List<Double> temporalMeans = new ArrayList<>();
        for (int i = 0; i < mmtList.size(); i++) {
            MeanMultiThread mmt = mmtList.get(i);
            try {
                mmt.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // print out temporal mean of each thread
            temporalMeans.add(mmt.getMean());
            System.out.println("Temporal mean value of thread " + (i + 1) + " is " + mmt.getMean() + ".");
        }
        // compute global mean
        double globalSum = temporalMeans.stream().mapToDouble(Double::doubleValue).sum();
        double globalMean = globalSum / numOfThreads;

        // record ending time and compute total time elapsed
        long endTime = System.nanoTime();
        double elapsedTime = (endTime - startTime) / 1000000000.0;
        // print out global mean and elapsed time
        System.out.println("\nThe global mean value is " + globalMean + ".");
        System.out.println("Total time taken: " + elapsedTime + "s");
    }

    private static List<List<Integer>> createSubArrays(File inputFile, int numOfThreads) {
        // parse input file into an array of integers
        List<Integer> array = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                for (String numStr : line.split(" ")) {
                    if (!numStr.isEmpty())
                        array.add(Integer.valueOf(numStr.trim()));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

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

// extend the Thread class
class MeanMultiThread extends Thread {
    private List<Integer> numList;
    private double mean;
    MeanMultiThread(List<Integer> numList) {
        this.numList = numList;
    }

    public double getMean() {
        return mean;
    }

    public void run() {
        mean = computeMean(numList);
    }

    // use reduce to compute list mean
    private double computeMean(List<Integer> list) {
        double sum = list.stream().mapToInt(Integer::intValue).sum();

        return sum / list.size();
    }
}