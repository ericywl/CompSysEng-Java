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
    private boolean running;

    public ProcessGraphNode(int nodeId) {
        this.nodeId = nodeId;
        this.runnable = false;
        this.executed = false;
        this.running = false;
    }

    // check if all parents have finished execution ie. cleared dependencies
    public synchronized boolean allParentsExecuted() {
        for (ProcessGraphNode parent : this.getParents()) {
            if (!parent.isExecuted())
                return false;
        }

        return true;
    }

    // setting a process to run would disable it from being runnable
    public void setRunning() {
        this.running = true;
        this.runnable = false;
    }

    // setting a process to executed would stop it from running
    // and disable it from being runnable
    public void setExecuted() {
        this.executed = true;
        this.running = false;
        this.runnable = false;
    }

    public void setRunnable() {
        this.runnable = true;
    }

    public boolean isRunnable() {
        return runnable;
    }

    public boolean isExecuted() {
        return executed;
    }

    public boolean isRunning() {
        return running;
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