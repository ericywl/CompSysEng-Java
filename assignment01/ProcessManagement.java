/* Programming Assignment 1
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 01/02/2018
 */

import java.io.File;
import java.util.*;

public class ProcessManagement {
    // set the working directory and instructions file
    private static File workingDirectory = new File("");
    private static File instructionSet = new File("test2.txt");
    // set thread sleep duration in ms (for concurrency testing)
    private static long sleepDuration = 500;
    // mapping between all nodes and their respective threads
    private static Map<ProcessGraphNode, ProcessThread> threadsMap = new HashMap<>();

    public static void main(String[] args) {
        parseArgs(args);

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
                    node.setDone();
                }

                /* set node to runnable if all parents finished execution
                and node not already executed or is done */
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

    private static void parseArgs(String[] args) {
        if (args.length == 0)
            return;

        if (args.length != 2 && args.length != 3) {
            throw new IllegalArgumentException("Wrong number of arguments.");
        }

        // set working directory and instruction set to the arguments provided
        File tempWorkingDirectory = new File(args[0]);
        File tempInstructionSet = new File(args[1]);
        // provided path is not a directory
        if (!tempWorkingDirectory.isDirectory()) {
            throw new IllegalArgumentException(tempWorkingDirectory + " is not a directory.");
        }

        File file = new File(tempWorkingDirectory + "/" + tempInstructionSet);
        // file does not exist
        if (!file.exists()) {
            throw new IllegalArgumentException("The file provided does not exist.");
        }
        // path given is not a file
        if (!file.isFile()) {
            throw new IllegalArgumentException("The path provided is not a file.");
        }

        workingDirectory = tempWorkingDirectory;
        instructionSet = tempInstructionSet;
    }

    /**
     * Check if all nodes have finished execution
     * @return true if all nodes finished, else false
     */
    private static boolean allNodesFinished() {
        for (ProcessGraphNode node : ProcessGraph.nodes.values()) {
            if (!node.isDone())
                return false;
        }

        return true;
    }
}