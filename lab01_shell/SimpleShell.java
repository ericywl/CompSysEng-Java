import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("InfiniteLoopStatement")
public class SimpleShell {
    private final static int HISTORY_UPPER_LIMIT = 50;
    private static LinkedList<String> historyList = new LinkedList<>();

    private static File getNewDir(String commandDir, File currentDir) {
        if (currentDir == null) currentDir = new File("");
        int len = commandDir.length();
        String prefix = commandDir.split("/")[0];

        // various shorthands
        if (prefix.equalsIgnoreCase("~")) {
            String homeDirStr = new File(System.getProperty("user.home")).getAbsolutePath();
            commandDir = homeDirStr + commandDir.substring(1, len);
        } else if (prefix.equalsIgnoreCase(".")) {
            String currDirStr = currentDir.getAbsolutePath();
            commandDir = currDirStr + commandDir.substring(1, len);
        } else {
            String currDirStr = currentDir.getAbsolutePath();
            commandDir = currDirStr + "/" + commandDir;
        }

        return new File(commandDir);
    }

    private static boolean checkValidInt(String input) {
        try {
            int i = Integer.parseInt(input);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static void executeCommand(ProcessBuilder pb, String commandLine)
            throws IOException, InterruptedException {
        String[] commandList = commandLine.split(" ");
        String command = commandList[0];

        if (command.equalsIgnoreCase("cd")) {
            // change directory to home if empty param
            if (commandList.length == 1) {
                File homeDir = new File(System.getProperty("user.home"));
                pb.directory(homeDir);
                return;
            }

            File newDir = getNewDir(commandList[1], pb.directory());
            // check if new directory exists and is a directory
            if (!newDir.exists()) {
                System.out.println(commandList[1] + " does not exist.");
                return;
            } else if (!newDir.isDirectory()) {
                System.out.println(commandList[1] + " is not a directory.");
                return;
            }

            // change directory
            pb.directory(newDir);
            return;
        }

        pb.command(commandList);
        pb.redirectErrorStream(true);
        Process shellProcess = pb.start();

        String line;
        InputStreamReader inputSR = new InputStreamReader(shellProcess.getInputStream());
        BufferedReader processBR = new BufferedReader(inputSR);
        while ((line = processBR.readLine()) != null)
            System.out.println(line);

        shellProcess.waitFor();
    }

    private static void runShell() {
        String commandLine;
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        ProcessBuilder pb = new ProcessBuilder();

        while (true) {
            // read what the user entered
            try {
                System.out.print("jsh> ");
                commandLine = console.readLine().trim();

                // if the user entered a return, just loop again
                if (commandLine.equals("")) {
                    continue;
                }

                String[] commandList = commandLine.split(" ");
                String command = commandList[0];
                if (command.equalsIgnoreCase("history")) {
                    for (int i = 0, size = historyList.size(); i < size; i++) {
                        String index = String.format("%4d  ", i + 1);
                        System.out.println(index + historyList.get(i));
                    }

                } else if (command.equalsIgnoreCase("!!")) {
                    if (historyList.size() == 0) {
                        System.out.println("No commands in history list.");
                    }

                    String histCommandLine = historyList.get(0);
                    // execute command without adding to history
                    executeCommand(pb, histCommandLine);

                } else if (checkValidInt(command)) {
                    int commandIndex = Integer.parseInt(command);
                    int size = historyList.size();

                    if (commandIndex >= HISTORY_UPPER_LIMIT || commandIndex < 1) {
                        System.out.println("The index is not in range of 1-" + HISTORY_UPPER_LIMIT + ".");
                        continue;
                    }

                    if (size == 0) {
                        System.out.println("No commands in history list.");
                        continue;
                    }

                    if (commandIndex > size) {
                        System.out.println("There is no command with that index.");
                        continue;
                    }

                    String histCommandLine = historyList.get(commandIndex - 1);
                    // execute command without adding to history
                    executeCommand(pb, histCommandLine);

                } else {
                    // execute command and add to history
                    executeCommand(pb, commandLine);
                    String formattedCommandLine = String.join(" ", commandList);
                    historyList.addFirst(formattedCommandLine);
                }

                if (historyList.size() > HISTORY_UPPER_LIMIT) historyList.removeLast();
            } catch (IOException | InterruptedException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        runShell();
    }
}