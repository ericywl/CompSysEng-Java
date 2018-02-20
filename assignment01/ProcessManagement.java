/* Programming Assignment 1
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 01/02/2018
 */

import java.io.File;
import java.util.*;

public class ProcessManagement {
    // set the working directory and instructions file
    private static File workingDirectory = new File("/Users/ericyap/Desktop/test_folder/test1");
    private static File instructionSet = new File("test1.txt");
    // set thread sleep duration in ms (for concurrency testing)
    private static long sleepDuration = 500;
    // mapping between all nodes and their respective threads
    private static Map<ProcessGraphNode, ProcessThread> threadsMap = new HashMap<>();

    public static void main(String[] args) {
        // parse the instruction file and construct a data structure, stored inside ProcessGraph class
        ParseFile.generateGraph(new File(workingDirectory + "/" + instructionSet));
        // print the graph
        ProcessGraph.printGraph();
        // ProcessGraph.printBasic();

        for (ProcessGraphNode node : ProcessGraph.nodes.values())
            threadsMap.put(node, new ProcessThread(node, workingDirectory, sleepDuration));

        manageThreads();
    }

    // schedule the processes while all nodes are not done executing
    private static void manageThreads() {
        while (!allNodesFinished()) {
            for (ProcessGraphNode node : threadsMap.keySet()) {
                ProcessThread pThread = threadsMap.get(node);
                // set node to done if thread finished
                if (pThread.isFinished()) {
                    try {
                        pThread.join();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    node.setDone();
                }

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