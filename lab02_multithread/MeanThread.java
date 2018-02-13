/* Lab 2 Multi-Thread
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 08/02/2017
 */

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MeanThread {
    public static void main(String[] args) throws InterruptedException {
        // get input file path and number of threads from the command line arguments
        File inputFile = new File(args[0]);
        int numOfThreads = Integer.valueOf(args[1]);
        // parse the input file to get an array of integers
        List<Integer> array = Parser.parseFile(inputFile);

        // if number of threads is less than 1 (invalid)
        if (numOfThreads < 1) {
            System.out.println("Invalid argument");
            return;
        }

        // split array into sub-arrays
        List<List<Integer>> subArrList = Parser.createSubArrays(array, numOfThreads);
        // record starting time
        long startTime = System.nanoTime();
        double globalSum, globalMean;
        // create and start the threads
        List<MeanMultiThread> mmtList = new ArrayList<>();
        for (List<Integer> subArr : subArrList) {
            MeanMultiThread mmt = new MeanMultiThread(subArr);
            mmt.start();
            mmtList.add(mmt);
        }

        // join the threads to main and add temporal mean to sum
        globalSum = 0;
        for (int i = 0; i < mmtList.size(); i++) {
            MeanMultiThread mmt = mmtList.get(i);
            mmt.join();
            globalSum += mmt.getMean();
            // print out temporal mean of each thread
            System.out.println("Temporal mean value of thread " + (i + 1) + " is " + mmt.getMean() + ".");
        }
        // compute global mean
        globalMean = globalSum / mmtList.size();

        // record ending time and compute total time elapsed
        long endTime = System.nanoTime();
        double elapsedTime = (endTime - startTime) / 1000000.0;
        // print out global mean and elapsed time
        System.out.println("\nThe global mean value is " + globalMean + ".");
        System.out.println("Total time taken: " + elapsedTime + "ms");
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

    @Override
    public void run() {
        mean = computeMean(numList);
    }

    private double computeMean(List<Integer> list) {
        return Misc.summation(list) / list.size();
    }
}