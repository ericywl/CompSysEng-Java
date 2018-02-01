/* Programming Assignment 1
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 01/02/2018
 */

import java.io.File;
import java.util.*;

public class ProcessManagement {
    // set the working directory
    private static File workingDirectory = new File("/Users/ericyap/Desktop/test_folder/test3");
    // set the instructions file
    private static File instructionSet = new File("test3.txt");
    // set thread sleep duration (for concurrency testing)
    private static long sleepDuration = 1000;
    // mapping between all nodes and their respective threads
    private static Map<ProcessGraphNode, ProcessThread> threadsMap = new HashMap<>();

    public static void main(String[] args) {
        // parse the instruction file and construct a data structure, stored inside ProcessGraph class
        ParseFile.generateGraph(new File(workingDirectory + "/" + instructionSet));

        // print the graph
        ProcessGraph.printGraph();
        // ProcessGraph.printBasic();

        initThreads();
        manageProcess();
    }

    // initialize all the threads and create the node-thread mapping
    private static void initThreads() {
        try {
            for (ProcessGraphNode node : ProcessGraph.nodes.values())
                threadsMap.put(node, new ProcessThread(node, workingDirectory, sleepDuration));
            for (ProcessThread pThread : threadsMap.values())
                pThread.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // schedule the processes while all nodes are not done executing
    private static void manageProcess() {
        while (!allNodesFinished()) {
            for (ProcessGraphNode node : threadsMap.keySet()) {
                ProcessThread pThread = threadsMap.get(node);
                // set node to done if thread finished
                if (pThread.isFinished())
                    node.setDone();

                // set node to runnable if all parents finished execution
                // and node not already executed or is done
                if (node.allParentsDone() & !node.isExecuted() & !node.isDone())
                    node.setRunnable();

                // start the thread and set node to executed if it is runnable
                if (node.isRunnable()) {
                    pThread.start();
                    node.setExecuted();
                }
            }
        }

        System.out.println("All processes finished successfully.");
    }

    // check if all nodes have finished execution
    private static boolean allNodesFinished() {
        for (ProcessGraphNode node : ProcessGraph.nodes.values()) {
            if (!node.isDone())
                return false;
        }

        return true;
    }
}