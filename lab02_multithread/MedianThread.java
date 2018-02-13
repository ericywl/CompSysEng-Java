/* Lab 2 Multi-Thread
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 08/02/2017
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MedianThread {
    public static void main(String[] args) throws InterruptedException {
        // get input file path and number of threads from the command line arguments
        File inputFile = new File(args[0]);
        int numOfThreads = Integer.valueOf(args[1]);
        List<Integer> array = Parser.parseFile(inputFile);
        List<List<Integer>> subArrList = Parser.createSubArrays(array, numOfThreads);

        // if number of threads is less than 1 (invalid)
        if (numOfThreads < 1) {
            System.out.println("Invalid argument");
            return;
        }

        // record starting time
        long startTime = System.nanoTime();
        List<Integer> sortedFullArr;

        if (numOfThreads == 1) {
            Collections.sort(array);
            sortedFullArr = array;
        } else {
            // create and start the sorting threads
            List<SortThread> mmtList = new ArrayList<>();
            for (List<Integer> subArr : subArrList) {
                SortThread mmt = new SortThread(subArr);
                mmt.start();
                mmtList.add(mmt);
            }

            // join the sorting threads to mean and added the sorted sub-lists to another list
            List<List<Integer>> sortedSubArrList = new ArrayList<>();
            for (SortThread sortThread : mmtList) {
                sortThread.join();
                sortedSubArrList.add(sortThread.getInternal());
            }
            // use multi-threading to merge sorted list
            sortedFullArr = multiMerge(sortedSubArrList);
        }
        // get the median of a sorted list
        double globalMedian = computeMedian(sortedFullArr);

        // record ending time and compute total time elapsed
        long endTime = System.nanoTime();
        double elapsedTime = (endTime - startTime) / 1000000000.0;
        // print out global mean and elapsed time
        System.out.println("\nThe global median value is " + globalMedian + ".");
        System.out.println("Total time taken: " + elapsedTime + "s");
    }

    private static double computeMedian(List<Integer> sortedList) {
        if (sortedList == null || sortedList.isEmpty() ) {
            throw new IllegalArgumentException("List cannot be empty.");
        }

        int size = sortedList.size();
        if (size % 2 != 0) {
            return sortedList.get(size / 2);
        }

        int midRight = size / 2;
        int sum = sortedList.get(midRight) + sortedList.get(midRight - 1);
        return sum / 2.0;
    }

    private static List<Integer> multiMerge(List<List<Integer>> sortedSubArrList) throws InterruptedException {
        while (sortedSubArrList.size() != 1) {
            List<MergeThread> mtList = new ArrayList<>();
            int len = sortedSubArrList.size();
            // create and start the merge threads
            for (int i = 1; i < len; i += 2) {
                MergeThread mergeThread = new MergeThread(sortedSubArrList.get(i), sortedSubArrList.get(i - 1));
                mergeThread.start();
                mtList.add(mergeThread);
            }

            // add the 0th array first in the case of odd list size
            List<Integer> tempList = sortedSubArrList.get(len - 1);
            sortedSubArrList = new ArrayList<>();
            if (len % 2 != 0) {
                sortedSubArrList.add(tempList);
            }

            // join the merge threads to main and add the merged lists to another list
            for (MergeThread mt : mtList) {
                mt.join();
                sortedSubArrList.add(mt.getInternal());
            }
        }

        return sortedSubArrList.get(0);
    }
}

// extend Thread
class SortThread extends Thread {
    private List<Integer> numList;

    public List<Integer> getInternal() {
        return numList;
    }

    SortThread(List<Integer> numList) {
        this.numList = numList;
    }

    @Override
    public void run() {
        // use merge sort to sort the individual lists
        numList = Misc.mergeSort(numList);
    }
}

// thread class for merging two sorted lists
class MergeThread extends Thread {
    private List<Integer> mergedList = new ArrayList<>();
    private List<Integer> list1;
    private List<Integer> list2;

    public List<Integer> getInternal() {
        return mergedList;
    }

    MergeThread(List<Integer> list1, List<Integer> list2) {
        this.list1 = list1;
        this.list2 = list2;
    }

    @Override
    public void run() {
        mergedList = Misc.merge(list1, list2);
    }
}
