/* Programming Assignment 1
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 01/02/2018
 */

import java.io.File;
import java.util.*;

public class ProcessManagement {
    // set the working directory
    private static File currentDirectory = new File("/Users/ericyap/Desktop/test_folder/test1");
    // set the instructions file
    private static File instructionSet = new File("test1.txt");
    // set thread sleep duration (for concurrency testing)
    private static long sleepDuration = 1000;

    private static Map<ProcessGraphNode, ProcessThread> runningThreads = new HashMap<>();
    public static Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {
        // parse the instruction file and construct a data structure, stored inside ProcessGraph class
        ParseFile.generateGraph(new File(currentDirectory + "/" + instructionSet));
        // print the graph
        ProcessGraph.printGraph();
        // ProcessGraph.printBasic();
        manageProcess();
    }

    private static void manageProcess() {
        // while all nodes have not finished execution
        while (!allNodesExecuted()) {
            // check for running threads that just finished
            for (ProcessGraphNode runningNode : runningThreads.keySet()) {
                ProcessThread pThread = runningThreads.get(runningNode);
                if (pThread.isFinished()) {
                    try {
                        pThread.join();
                        runningNode.setExecuted();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            for (ProcessGraphNode node : ProcessGraph.nodes.values()) {
                if (node.allParentsExecuted() & !node.isRunning() & !node.isExecuted()) {
                    node.setRunnable();
                }

                if (node.isRunnable()) {
                    ProcessThread pThread = new ProcessThread(node, currentDirectory, sleepDuration);
                    pThread.start();
                    node.setRunning();
                    runningThreads.put(node, pThread);
                }
            }
        }

        System.out.println("All processes finished successfully.");
    }

    // check if all nodes have finished execution
    private static boolean allNodesExecuted() {
        for (ProcessGraphNode node : ProcessGraph.nodes.values()) {
            if (!node.isExecuted())
                return false;
        }

        return true;
    }
}