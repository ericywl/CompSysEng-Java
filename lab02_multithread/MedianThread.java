import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MedianThread {

    public static void main(String[] args) throws InterruptedException, FileNotFoundException  {
        // get input file path and number of threads from the command line arguments
        File inputFile = new File(args[0]);
        int numOfThreads = Integer.valueOf(args[1]);
        List<List<Integer>> subArrList = Parser.createSubArrays(inputFile, numOfThreads);

        List<MedianMultiThread> mmtList = new ArrayList<>();
        for (List<Integer> subArr : subArrList) {
            MedianMultiThread mmt = new MedianMultiThread(subArr);
            mmt.start();
            mmtList.add(mmt);
        }

        for (MedianMultiThread mmt : mmtList) {

        }

        // TODO: use any merge algorithm to merge the sorted subarrays and store it to another array, e.g., sortedFullArray.

        //TODO: get median from sortedFullArray

        //e.g, computeMedian(sortedFullArray);

        // TODO: stop recording time and compute the elapsed time

        // TODO: printout the final sorted array

        // TODO: printout median
        System.out.println("The Median value is ...");

    }

    private static double computeMedian(ArrayList<Integer> inputArray) {
        //TODO: implement your function that computes median of values of an array
        return 0;
    }

}

// extend Thread
class MedianMultiThread extends Thread {
    private List<Integer> numList;

    public List<Integer> getInternal() {
        return numList;
    }

    MedianMultiThread(List<Integer> numList) {
        this.numList = numList;
    }

    public void run() {
        // called by object.start()
        Collections.sort(numList);
    }

    // TODO: implement merge sort here, recursive algorithm
}
