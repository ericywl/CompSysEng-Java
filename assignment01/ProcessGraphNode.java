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
    private boolean runnable;
    private boolean executed;
    private boolean done;

    public ProcessGraphNode(int nodeId) {
        this.nodeId = nodeId;
        this.runnable = false;
        this.executed = false;
        this.done = false;
    }

    // check if all parent nodes are done ie. current node cleared dependencies
    public synchronized boolean allParentsDone() {
        for (ProcessGraphNode parent : this.getParents()) {
            if (!parent.isDone())
                return false;
        }

        return true;
    }

    // setting a process to executed would disable it from being runnable
    public void setExecuted() {
        this.executed = true;
        this.runnable = false;
    }

    // setting a process to done would disable it from being runnable
    // also sets executed to true just in case
    public void setDone() {
        this.done = true;
        this.executed = true;
        this.runnable = false;
    }

    public void setRunnable() {
        this.runnable = true;
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