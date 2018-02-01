import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ParseFile {
    // this method generates a ProcessGraph and store in ProcessGraph Class
    public static void generateGraph(File inputFile) {
        try {
            List<Pair<Integer, Integer>> edges = new ArrayList<>();
            Scanner fileIn = new Scanner(inputFile);
            int index = 0;
            while (fileIn.hasNext()) {
                String line = fileIn.nextLine();
                String[] quartiles = line.split(":");
                if (quartiles.length != 4) {
                    throw new IllegalArgumentException("Wrong input format.");
                }

                // add this node to the graph
                ProcessGraph.addNode(index);

                // add edges from this node to its children
                if (!quartiles[1].equals("none")) {
                    String[] childrenStringArray = quartiles[1].split(" ");
                    int[] childrenId = new int[childrenStringArray.length];
                    for (int i = 0; i < childrenId.length; i++) {
                        childrenId[i] = Integer.parseInt(childrenStringArray[i]);
                        ProcessGraph.addNode(childrenId[i]);
                        edges.add(new Pair<>(index, childrenId[i]));
                    }
                }
                // setup command, input and output
                ProcessGraph.nodes.get(index).setCommand(quartiles[0]);
                ProcessGraph.nodes.get(index).setInputFile(new File(quartiles[2]));
                ProcessGraph.nodes.get(index).setOutputFile(new File(quartiles[3]));

                index++;
            }

            // setup children
            for (Pair<Integer, Integer> edge : edges) {
                ProcessGraph.nodes.get(edge.getKey())
                        .addChild(ProcessGraph.nodes.get(edge.getValue()));
            }

            // setup parent
            for (ProcessGraphNode node : ProcessGraph.nodes.values()) {
                for (ProcessGraphNode childNode : node.getChildren()) {
                    ProcessGraph.nodes.get(childNode.getNodeId())
                            .addParent(ProcessGraph.nodes.get(node.getNodeId()));
                }
            }

            // mark initial runnable
            for (ProcessGraphNode node : ProcessGraph.nodes.values()) {
                if (node.getParents().isEmpty()) {
                    node.setRunnable();
                }
            }

        } catch (IOException | IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }
}