/* Programming Assignment 1
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 01/02/2018
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessThread extends Thread {
    private ProcessGraphNode node;
    private long sleepDuration;
    private ProcessBuilder pb = new ProcessBuilder();
    private int finishStatus = 1;

    public ProcessThread(ProcessGraphNode node, File workingDir, long sleepDuration) {
        this.node = node;
        this.sleepDuration = sleepDuration;

        // set the command and directory of ProcessBuilder
        String[] commandArr = node.getCommand().trim().split(" ");
        pb.command(commandArr);
        pb.directory(workingDir);

        // redirect the error stream to stdout
        pb.redirectErrorStream(true);
        redirectIO();
    }

    // redirecting the IO to respective files
    private void redirectIO() {
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
            int termVal = p.waitFor();
            // check for abnormal exit value
            if (termVal != 0) {
                System.out.println("Process " + node.getNodeId() + " terminated abnormally.");
                return;
            }

            debugPrint(p);

            // set finishStatus to 0 (normal)
            finishStatus = 0;

        } catch (IOException | InterruptedException ex) {
            // print error and set finishStatus to -1 (error)
            System.out.println("Process " + node.getNodeId() + " exited with an error: "
                    + ex.getMessage() + ".");
            finishStatus = -1;
        }
    }

    public ProcessGraphNode getNode() {
        return node;
    }

    public int getFinishStatus() {
        return finishStatus;
    }

    /**
     * Print the output of the process
     * (mainly for debugging purposes since the outputs are redirected)
     * @param p - the process to get information from
     */
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
