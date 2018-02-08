import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MedianThread {

    public static void main(String[] args) throws InterruptedException, FileNotFoundException  {
        // get input file path and number of threads from the command line arguments
        File inputFile = new File(args[0]);
        int numOfThreads = Integer.valueOf(args[1]);
        List<Integer> array = Parser.parseFile(inputFile);
        List<List<Integer>> subArrList = Parser.createSubArrays(array, numOfThreads);

        List<MedianMultiThread> mmtList = new ArrayList<>();
        for (List<Integer> subArr : subArrList) {
            MedianMultiThread mmt = new MedianMultiThread(subArr);
            mmt.start();
            mmtList.add(mmt);
        }

        for (MedianMultiThread mmt : mmtList) {
            mmt.join();
        }
    }

    private static double computeMedian(List<Integer> numList) {
        int size = numList.size();
        if (size % 2 != 0) {
            return numList.get(size / 2);
        }

        int midRight = size / 2;
        int sum = numList.get(midRight) + numList.get(midRight - 1);
        return sum / 2.0;
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
