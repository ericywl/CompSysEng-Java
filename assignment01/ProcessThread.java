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

        pb.command(node.getCommand().split(" "));
        pb.redirectErrorStream(true);
        pb.directory(workingDir);

        if (!node.getInputFile().toString().equalsIgnoreCase("stdin")) {
            File input = new File(pb.directory().getAbsolutePath() + "/" + node.getInputFile());
            pb.redirectInput(input);
        }

        if (!node.getOutputFile().toString().equalsIgnoreCase("stdout")) {
            File output = new File(pb.directory().getAbsolutePath() + "/" + node.getOutputFile());
            pb.redirectOutput(output);
        }
    }

    @Override
    public void run() {
        try {
            Thread.sleep(sleepDuration);
            Process p = pb.start();
            p.waitFor();

            System.out.println("Process " + node.getNodeId() + " has finished execution.");
            finished = true;

        } catch (IOException | InterruptedException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public boolean isFinished() {
        return finished;
    }

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
