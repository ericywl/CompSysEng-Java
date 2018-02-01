import java.util.HashMap;
import java.util.Map;

public class ProcessGraph {
    // Map of index to ProcessGraphNode, contains all the nodes in the graph
    public static Map<Integer, ProcessGraphNode> nodes = new HashMap<>();

    // add node to graph if not yet created
    public static void addNode(int index) {
        nodes.putIfAbsent(index, new ProcessGraphNode(index));
    }

    // print the information of ProcessGraph
    public static void printGraph() {
        System.out.println();
        System.out.println("Graph info:");
        try {
            for (ProcessGraphNode node : nodes.values()) {
                System.out.print("Node " + node.getNodeId() + ": \nParent: ");
                if (node.getParents().isEmpty()) System.out.print("none");
                for (ProcessGraphNode parentNode : node.getParents()) {
                    System.out.print(parentNode.getNodeId() + " ");
                }

                System.out.print(" \nChildren: ");
                if (node.getChildren().isEmpty()) System.out.print("none");
                for (ProcessGraphNode childNode : node.getChildren()) {
                    System.out.print(childNode.getNodeId() + " ");
                }

                System.out.print("\nCommand: " + node.getCommand() + "    ");
                System.out.print("\nInput File: " + node.getInputFile() + "    ");
                System.out.println("\nOutput File: " + node.getOutputFile() + "    ");
                System.out.println("Runnable: " + node.isRunnable());
                System.out.println("Executed: " + node.isDone());
                System.out.println("\n");
            }
        } catch (Exception e) {
            System.out.println("Exception!");
        }
    }

    // print basic information of ProcessGraph
    public static void printBasic() {
        System.out.println("Basic info:");
        for (ProcessGraphNode node : nodes.values()) {
            System.out.println("Node: " + node.getNodeId() + " Runnable: " + node.isRunnable()
                    + " Executed: " + node.isDone());
        }
    }


}