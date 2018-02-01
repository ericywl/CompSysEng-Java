import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessThread extends Thread {
    private ProcessGraphNode node;
    private long sleepDuration;
    private ProcessBuilder pb = new ProcessBuilder();
    private boolean finished = false;

    public ProcessThread(ProcessGraphNode node, File workingDir, long sleepDuration) {
        this.node = node;
        this.sleepDuration = sleepDuration;

        // set the command and directory of ProcessBuilder
        pb.command(node.getCommand().split(" "));
        pb.directory(workingDir);
        // redirect the error stream to stdout
        pb.redirectErrorStream(true);

        // redirect input for non-stdin
        if (!node.getInputFile().toString().equalsIgnoreCase("stdin")) {
            File input = new File(pb.directory().getAbsolutePath() + "/" + node.getInputFile());
            pb.redirectInput(input);
        }

        // redirect output for non-stdout
        if (!node.getOutputFile().toString().equalsIgnoreCase("stdout")) {
            File output = new File(pb.directory().getAbsolutePath() + "/" + node.getOutputFile());
            pb.redirectOutput(output);
        }
    }

    @Override
    public void run() {
        try {
            // sleep the thread (for concurrency testing)
            Thread.sleep(sleepDuration);
            // start the process and wait for it to finish
            Process p = pb.start();
            p.waitFor();

            // print finish status and set finished to true
            System.out.println("Process " + node.getNodeId() + " has finished execution.");
            finished = true;

        } catch (IOException | InterruptedException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public boolean isFinished() {
        return finished;
    }

    // print process output to console or terminal for debugging
    private void debugPrint(Process p) {
        try {
            String line;
            InputStreamReader inputSR = new InputStreamReader(p.getInputStream());
            BufferedReader processBR = new BufferedReader(inputSR);
            while ((line = processBR.readLine()) != null)
                System.out.println(line);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
