/* Programming Assignment 1
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 01/02/2018
 */

import java.io.File;
import java.util.ArrayList;

public class ProcessGraphNode {
    // pointers to all the parents and children
    private ArrayList<ProcessGraphNode> parents = new ArrayList<>();
    private ArrayList<ProcessGraphNode> children = new ArrayList<>();
    // properties of ProcessGraphNode
    private int nodeId;
    private File inputFile;
    private File outputFile;
    private String command;
    private String terminationMessage;

    private boolean runnable;
    private boolean executed;
    private boolean done;
    private boolean terminated;

    public ProcessGraphNode(int nodeId) {
        this.nodeId = nodeId;
        this.runnable = false;
        this.executed = false;
        this.done = false;
        this.terminated = false;
    }

    // check if all parent nodes are done ie. current node cleared dependencies
    public synchronized boolean allParentsDone() {
        for (ProcessGraphNode parent : this.getParents()) {
            if (!parent.isDone())
                return false;
        }

        return true;
    }

    /**
     * Set the process to terminated, disabling it from running again
     * @param msg - the error message
     */
    public void setTerminated(String msg) {
        this.terminated = true;
        this.runnable = false;
        this.executed = false;
        this.done = false;

        this.terminationMessage = msg;
    }

    /**
     * Set the process to executed, disable it from being runnable
     * Set done to false just in case
     */
    public void setExecuted() {
        this.executed = true;
        this.runnable = false;
        this.done = false;
    }

    /**
     * Set the process to done, disable it from being runnable
     * Set executed to true just in case
     */
    public void setDone() {
        this.done = true;
        this.executed = true;
        this.runnable = false;
    }

    /**
     * Set the process to runnable
     * Set executed and done to false just in case
     */
    public void setRunnable() {
        this.runnable = true;
        this.executed = false;
        this.done = false;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public boolean isRunnable() {
        return runnable;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isExecuted() {
        return executed;
    }

    public String getTerminationMsg() {
        return terminationMessage;
    }

    public void addChild(ProcessGraphNode child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }

    public void addParent(ProcessGraphNode parent) {
        if (!parents.contains(parent)) {
            parents.add(parent);
        }
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public File getInputFile() {
        return inputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public String getCommand() {
        return command;
    }

    public ArrayList<ProcessGraphNode> getParents() {
        return parents;
    }

    public ArrayList<ProcessGraphNode> getChildren() {
        return children;
    }

    public int getNodeId() {
        return nodeId;
    }
}