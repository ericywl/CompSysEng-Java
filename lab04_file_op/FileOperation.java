import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class FileOperation {
    private static File currentDirectory = new File(System.getProperty("user.dir"));
    private static int listPadding = 4;
    private static String treeBranch = "|-";
    private static int treePadding = 2;

    public static void main(String[] args) throws java.io.IOException {
        String commandLine;
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            // read what the user entered
            System.out.print("jsh>");
            commandLine = console.readLine();
            // clear the space before and after the command line
            commandLine = commandLine.trim();

            // if the user entered a return, just loop again
            if (commandLine.equals("")) {
                continue;
            }

            // if exit or quit
            if (commandLine.equalsIgnoreCase("exit") | commandLine.equalsIgnoreCase("quit")) {
                System.exit(0);
                return;
            }

            // check the command line, separate the words
            String[] commandStr = commandLine.split("\\s");
            ArrayList<String> command = new ArrayList<>(Arrays.asList(commandStr));

            switch (commandStr[0].toLowerCase()) {
                // handle create
                case "create":
                    handleCreate(commandStr);
                    continue;

                    // handle delete
                case "delete":
                    handleDelete(commandStr);
                    continue;

                    // handle display
                case "display":
                    handleDisplay(commandStr);
                    continue;

                    // handle list
                case "list":
                    handleList(commandStr);
                    continue;

                    // handle find
                case "find":
                    handleFind(commandStr);
                    continue;

                    // handle tree
                case "tree":
                    handleTree(commandStr);
                    continue;
            }

            // TODO: implement code to handle tree here

            // other commands
            ProcessBuilder pBuilder = new ProcessBuilder(command);
            pBuilder.directory(currentDirectory);
            try {
                Process process = pBuilder.start();
                // obtain the input stream
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                // read what is returned by the command
                String line;
                while ((line = br.readLine()) != null)
                    System.out.println(line);

                // close BufferedReader
                br.close();
            }
            // catch the IOException and resume waiting for commands
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void handleCreate(String[] commandStr) throws IOException {
        if (commandStr.length == 1) {
            System.out.println("Arguments needed.");
            return;
        }

        for (int i = 1; i < commandStr.length; i++)
            javaCreate(currentDirectory, commandStr[i]);
    }

    /**
     * Create a file
     *
     * @param dir  - current working directory
     * @param name - name of the file to be created
     */
    private static void javaCreate(File dir, String name) throws IOException {
        File file = new File(dir, name);
        boolean newFile = file.createNewFile();
        if (!newFile)
            System.out.println("File is not created.");
    }

    private static void handleDelete(String[] commandStr) {
        if (commandStr.length == 1) {
            System.out.println("Arguments needed.");
            return;
        }

        for (int i = 1; i < commandStr.length; i++)
            javaDelete(currentDirectory, commandStr[i]);
    }

    /**
     * Delete a file
     *
     * @param dir  - current working directory
     * @param name - name of the file to be deleted
     */
    private static void javaDelete(File dir, String name) {
        File file = new File(dir, name);
        boolean deleted = file.delete();
        if (!deleted)
            System.out.println("File is not deleted.");
    }

    private static void handleDisplay(String[] commandStr) throws IOException {
        if (commandStr.length == 1) {
            System.out.println("Arguments needed.");
            return;
        }

        for (int i = 1; i < commandStr.length; i++)
            javaCat(currentDirectory, commandStr[i]);
    }

    /**
     * Display the file
     *
     * @param dir  - current working directory
     * @param name - name of the file to be displayed
     */
    private static void javaCat(File dir, String name) throws IOException {
        try {
            File file = new File(dir, name);
            FileReader fileReader = new FileReader(file);
            BufferedReader in = new BufferedReader(fileReader);
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            in.close();

        } catch (FileNotFoundException ex) {
            System.out.println("File is not found");
        }
    }

    private static void handleList(String[] commandStr) {
        if (commandStr.length == 1) {
            javaLs(currentDirectory, "", "");
            return;
        }

        if (commandStr.length == 2) {
            javaLs(currentDirectory, commandStr[1], "");
            return;
        }

        if (commandStr.length == 3) {
            javaLs(currentDirectory, commandStr[1], commandStr[2]);
            return;
        }

        System.out.println("Too many arguments for list. Only maximum of two arguments.");
    }

    /**
     * List the files under directory
     *
     * @param dir           - current directory
     * @param displayMethod - control the list type
     * @param sortMethod    - control the sort type
     */
    private static void javaLs(File dir, String displayMethod, String sortMethod) {
        File[] fileList = dir.listFiles();
        if (fileList == null || fileList.length == 0) {
            System.out.println("No files in this directory.");
            return;
        }

        if (displayMethod.equalsIgnoreCase("")) {
            fileList = sortFileList(fileList, "name");
            if (fileList != null) {
                for (File file : fileList)
                    System.out.println(file.getName());
            }

            return;
        }

        if (displayMethod.equalsIgnoreCase("property")) {
            if (sortMethod.equalsIgnoreCase(""))
                sortMethod = "name";

            fileList = sortFileList(fileList, sortMethod);
            if (fileList != null)
                printListWithProperty(fileList);

            return;
        }

        System.out.println("Invalid second argument (property).");
    }

    private static void handleFind(String[] commandStr) {
        if (commandStr.length == 1) {
            System.out.println("Argument needed.");
            return;
        }

        if (commandStr.length > 2) {
            System.out.println("Too many arguments for find. Only one argument needed.");
            return;
        }

        boolean found = javaFind(currentDirectory, commandStr[1]);
        if (!found) {
            System.out.println("No files found.");
        }
    }

    /**
     * Find files based on input string
     *
     * @param dir  - current working directory
     * @param name - input string to find in file's name
     * @return flag - whether the input string is found in this directory and its subdirectories
     */
    private static boolean javaFind(File dir, String name) {
        File[] fileList = dir.listFiles();
        if (fileList == null || fileList.length == 0) {
            return false;
        }

        boolean flag = false;
        for (File file : fileList) {
            if (file.isDirectory()) {
                flag = flag | javaFind(file, name);
                continue;
            }

            String filePath = file.getAbsolutePath();
            if (filePath.contains(name)) {
                System.out.println(filePath);
                flag = true;
            }
        }

        return flag;
    }

    private static void handleTree(String[] commandStr) {
        if (commandStr.length == 1) {
            javaTree(currentDirectory, -1, "name");
            return;
        }

        try {
            int depth = Integer.parseInt(commandStr[1]);
            if (depth < 1) {
                System.out.println("Second argument has to greater than 0.");
                return;
            }

            if (commandStr.length == 2) {
                javaTree(currentDirectory, depth, "name");
                return;
            }

            if (commandStr.length == 3) {
                javaTree(currentDirectory, depth, commandStr[2]);
                return;
            }

            System.out.println("Too many arguments for tree. Only maximum of two arguments");

        } catch (NumberFormatException ex) {
            System.out.println("Invalid second argument. Only positive integers allowed.");
        }
    }

    /**
     * Print file structure under current directory in a tree structure
     *
     * @param dir        - current working directory
     * @param depth      - maximum sub-level file to be displayed
     * @param sortMethod - control the sort type
     */
    private static void javaTree(File dir, int depth, String sortMethod) {
        File[] fileList = dir.listFiles();
        if (fileList == null || fileList.length == 0) {
            System.out.println("No files in this directory.");
            return;
        }

        if (depth != -1 && depth < 1) {
            System.out.println("Invalid second argument. Depth has to be greater than 0.");
            return;
        }

        String result = javaTreeHelper(dir, 1, depth, sortMethod);
        if (result != null)
            System.out.println(result);
    }

    private static String javaTreeHelper(File dir, int currDepth, int maxDepth, String sortMethod) {
        File[] fileList = dir.listFiles();
        if (fileList == null || fileList.length == 0) {
            return null;
        }

        if (maxDepth != -1 && currDepth > maxDepth)
            return null;

        int padding = (currDepth < 2) ? 0 : (treePadding + treeBranch.length()) * (currDepth - 1);
        StringBuilder strBld = new StringBuilder();
        fileList = sortFileList(fileList, sortMethod);
        if (fileList == null)
            return null;

        for (File file : fileList) {
            if (padding > 0) {
                strBld.append(String.format("%" + padding + "s", treeBranch));
            }

            strBld.append(file.getName()).append("\n");
            if (file.isDirectory()) {
                String fileDirStr = javaTreeHelper(file, currDepth + 1, maxDepth, sortMethod);
                if (fileDirStr != null) {
                    strBld.append(fileDirStr);
                }
            }
        }

        return strBld.toString();
    }


    // *************************************//
    // UTILITY FUNCTIONS                    //
    // *************************************//

    /**
     * Function to get the length of longest property string in the list
     *
     * @param list - file list to go through
     * @param type - the type of property
     * @return length of longest filename in the list
     */
    private static int findLongestStrLen(File[] list, String type) {
        // get longest string length of name property
        if (type.equalsIgnoreCase("name")) {
            int nameLength = list[0].getName().length();
            for (int i = 1; i < list.length; i++) {
                int newLen = list[i].getName().length();
                if (newLen > nameLength)
                    nameLength = newLen;
            }

            return nameLength;
        }

        // get longest string length of size property
        if (type.equalsIgnoreCase("size")) {
            long fileSize = list[0].length();
            int sizeLength = String.valueOf(fileSize).length();
            for (int i = 1; i < list.length; i++) {
                long newFileSize = list[i].length();
                int newSizeLength = String.valueOf(newFileSize).length();
                if (newSizeLength > sizeLength)
                    sizeLength = newSizeLength;
            }

            return sizeLength;
        }

        throw new IllegalArgumentException(type + " is not a supported type.");
    }

    /**
     * Format the properties of the files in the list in a neat string and print them
     *
     * @param list - file list to go through
     */
    private static void printListWithProperty(File[] list) {
        int nameLen = findLongestStrLen(list, "name") + listPadding;
        int sizeLen = findLongestStrLen(list, "size") + listPadding;

        for (File file : list) {
            String strBld = String.format("%-" + nameLen + "s", file.getName()) +
                    "Size: " +
                    String.format("%-" + sizeLen + "d", file.length()) +
                    "Last Modified: " +
                    new Date(file.lastModified());

            System.out.println(strBld);
        }
    }

    /**
     * Function to sort the file list
     *
     * @param list       - file list to be sorted
     * @param sortMethod - control the sort type
     * @return sorted list - the sorted file list
     */
    private static File[] sortFileList(File[] list, String sortMethod) {
        // sort the file list based on sort_method
        // if sort based on name
        if (sortMethod.equalsIgnoreCase("name")) {
            Arrays.sort(list, Comparator.comparing(f -> (f.getName())));
        } else if (sortMethod.equalsIgnoreCase("size")) {
            Arrays.sort(list, Comparator.comparingLong(File::length));
        } else if (sortMethod.equalsIgnoreCase("time")) {
            Arrays.sort(list, Comparator.comparingLong(File::lastModified));
        } else {
            System.out.println("Invalid third argument (name, size, time).");
            return null;
        }

        return list;
    }

}