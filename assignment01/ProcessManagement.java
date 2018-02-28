/* Programming Assignment 1
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 01/02/2018
 */

import java.io.File;
import java.util.*;

public class ProcessManagement {
    // set the working directory and instructions file
    private static File workingDirectory = new File("test_folder/graph-file");
    private static File instructionSet = new File("graph-file");
    // set thread sleep duration in ms (for concurrency testing and better visualization)
    private static long sleepDuration = 0;

    // DO NOT EDIT VARIABLES BELOW THIS LINE
    // ========================================= //

    // set of threads to run
    private static Set<ProcessThread> threads = new HashSet<>();
    // set of threads that are done
    private static Set<ProcessThread> finishedThreads = new HashSet<>();

    public static void main(String[] args) {
        parseArgs(args);

        // parse the instruction file and construct a data structure, stored inside ProcessGraph class
        ParseFile.generateGraph(new File(workingDirectory + "/" + instructionSet));
        // print the graph
        ProcessGraph.printGraph();
        // ProcessGraph.printBasic();

        for (ProcessGraphNode node : ProcessGraph.nodes.values())
            threads.add(new ProcessThread(node, workingDirectory, sleepDuration));

        boolean success = manageThreads();
        if (!success) {
            // Program terminated pre-maturely
            System.out.println("Program terminating due to the above error.");
            return;
        }

        // Print success finish status
        System.out.println("All processes finished successfully.");
    }

    /**
     * Schedule the processes while all nodes are not done executing
     */
    private static boolean manageThreads() {
        while (!allNodesFinished()) {
            for (ProcessThread pThread : threads) {
                if (finishedThreads.contains(pThread)) {
                    continue;
                }

                ProcessGraphNode node = pThread.getNode();
                // set node to done if thread finished successfully
                if (pThread.getFinishStatus() == 0) {
                    node.setDone();
                    finishedThreads.add(pThread);
                    System.out.println("Process " + node.getNodeId() + " has finished execution.");
                }

                // exit manageThreads() if the finish status is -1
                if (pThread.getFinishStatus() == -1) {
                    return false;
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

        return true;
    }

    /**
     * Parse the command line arguments if they are provided
     *
     * @param args - the command line arguments
     */
    private static void parseArgs(String[] args) {
        if (args.length == 0)
            return;

        if (args.length != 2 && args.length != 3) {
            throw new IllegalArgumentException("Wrong number of arguments.");
        }

        // temporary working directory and instruction set to the arguments provided
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

        // set working directory and instruction set
        workingDirectory = tempWorkingDirectory;
        instructionSet = tempInstructionSet;

        // last argument is the sleep duration
        if (args.length == 3) {
            sleepDuration = Integer.parseInt(args[2]);
        }
    }

    /**
     * Check if all nodes have finished execution
     *
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