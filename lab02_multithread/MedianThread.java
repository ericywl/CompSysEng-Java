import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;


public class MedianThread {

    public static void main(String[] args) throws InterruptedException, FileNotFoundException  {
        // TODO: read data from external file and store it in an array
        // Note: you should pass the file as a first command line argument at runtime.

        // define number of threads
        int NumOfThread = Integer.valueOf(args[1]);// this way, you can pass number of threads as
        // a second command line argument at runtime.

        // TODO: partition the array list into N subArrays, where N is the number of threads

        // TODO: start recording time

        // TODO: create N threads and assign subArrays to the threads so that each thread sorts
        // its repective subarray. For example,

        MedianMultiThread thread1 = new MedianMultiThread(subArray1);
        MedianMultiThread threadn = new MedianMultiThread(subArrayn);
        //Tip: you can't create big number of threads in the above way. So, create an array list of threads.

        // TODO: start each thread to execute your sorting algorithm defined under the run() method, for example,
        thread1.start(); //start thread1 on from run() function
        threadn.start();//start thread2 on from run() function

        thread1.join();//wait until thread1 terminates
        threadn.join();//wait until threadn terminates

        // TODO: use any merge algorithm to merge the sorted subarrays and store it to another array, e.g., sortedFullArray.

        //TODO: get median from sortedFullArray

        //e.g, computeMedian(sortedFullArray);

        // TODO: stop recording time and compute the elapsed time

        // TODO: printout the final sorted array

        // TODO: printout median
        System.out.println("The Median value is ...");
        System.out.println("Running time is " + runningTime + " milliseconds\n");

    }

    public static double computeMedian(ArrayList<Integer> inputArray) {
        //TODO: implement your function that computes median of values of an array
    }

}

// extend Thread
class MedianMultiThread extends Thread {
    private ArrayList<Integer> list;

    public ArrayList<Integer> getInternal() {
        return list;
    }

    MedianMultiThread(ArrayList<Integer> array) {
        list = array;
    }

    public void run() {
        // called by object.start()
        mergeSort(list);

    }

    // TODO: implement merge sort here, recursive algorithm
    public void mergeSort(ArrayList<Integer> array) {

    }
}
