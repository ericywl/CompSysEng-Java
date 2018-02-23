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
    private static String treeBranch = "\u251c\u2500\u2500";
    private static String treeBranchEnd = "\u2514\u2500\u2500";
    private static int treePadding = 3;

    public static void main(String[] args) throws java.io.IOException {
        String commandLine;
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            // read what the user entered
            System.out.print("jsh> ");
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
                case "create":
                    // handle create
                    handleCreate(commandStr);
                    continue;

                case "delete":
                    // handle delete
                    handleDelete(commandStr);
                    continue;

                case "display":
                    // handle display
                    handleDisplay(commandStr);
                    continue;

                case "list":
                    // handle list
                    handleList(commandStr);
                    continue;

                case "find":
                    // handle find
                    handleFind(commandStr);
                    continue;

                case "tree":
                    // handle tree
                    handleTree(commandStr);
                    continue;
            }

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
                System.out.println(ex);
            }
        }
    }

    // 'create' command handler (support multiple creates in one line)
    private static void handleCreate(String[] commandStr) throws IOException {
        // check that arguments are provided for the command
        if (commandStr.length == 1) {
            System.out.println("Arguments needed for 'create' command.");
            return;
        }

        // loop through all arguments and javaCreate them
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
        boolean fileCreated = file.createNewFile();

        String message = (fileCreated) ? name + " created." : name + " already exists.";
        System.out.println(message);
    }

    // 'delete' command handler (support multiple deletes in one line)
    private static void handleDelete(String[] commandStr) {
        // check that arguments are provided for the command
        if (commandStr.length == 1) {
            System.out.println("Arguments needed for 'delete' command.");
            return;
        }

        // loop through all arguments and javaDelete them
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
        boolean fileDeleted = file.delete();

        String message = (fileDeleted) ? name + " deleted." : name + " does not exist.";
        System.out.println(message);
    }

    // 'display' command handler (support multiple displays in one line)
    private static void handleDisplay(String[] commandStr) throws IOException {
        // check that arguments are provided for the command
        if (commandStr.length == 1) {
            System.out.println("Arguments needed for 'display' command.");
            return;
        }

        // loop through all arguments and javaCat them
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
            System.out.println(name + " does not exist.");
        }
    }

    // 'list' command handler
    private static void handleList(String[] commandStr) {
        // if no arguments provided
        if (commandStr.length == 1) {
            javaLs(currentDirectory, "", "");
            return;
        }

        // if 1 argument provided
        if (commandStr.length == 2) {
            javaLs(currentDirectory, commandStr[1], "");
            return;
        }

        // if 2 arguments provided
        if (commandStr.length == 3) {
            javaLs(currentDirectory, commandStr[1], commandStr[2]);
            return;
        }

        // if more than 2 arguments provided
        System.out.println("Too many arguments for 'list' command. Only maximum of two arguments.");
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
        // check if any files exist in the working directory
        if (fileList == null || fileList.length == 0) {
            System.out.println("No files in this directory.");
            return;
        }

        // default display name only and sort by name
        if (displayMethod.equalsIgnoreCase("")) {
            fileList = sortFileList(fileList, "name");
            if (fileList != null) {
                for (File file : fileList)
                    System.out.println(file.getName());
            }

            return;
        }

        // if property given and no sort method, default sort method to name
        if (displayMethod.equalsIgnoreCase("property")) {
            if (sortMethod.equalsIgnoreCase(""))
                sortMethod = "name";
            // sort the file list according to the sort method and print the list of files
            fileList = sortFileList(fileList, sortMethod);
            if (fileList != null)
                printListWithProperty(fileList);

            return;
        }

        // if second argument is not 'property'
        System.out.println("Invalid second argument for 'list' command (property).");
    }

    // 'find' command handler
    private static void handleFind(String[] commandStr) {
        // if no arguments provided
        if (commandStr.length == 1) {
            System.out.println("Argument needed for 'find' command.");
            return;
        }

        // if more than 1 arguments provided
        if (commandStr.length > 2) {
            System.out.println("Too many arguments for 'find' command. Only one argument needed.");
            return;
        }

        // javaFind the argument, print message if no matching files found
        boolean found = javaFind(currentDirectory, commandStr[1]);
        if (!found) {
            System.out.println("No matching files found.");
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
        // check if any files exist in the working directory
        if (fileList == null || fileList.length == 0) {
            return false;
        }

        boolean flag = false;
        for (File file : fileList) {
            // recursively find the name in sub-directories
            if (file.isDirectory()) {
                flag = flag | javaFind(file, name);
                continue;
            }

            // print out the absolute file path if found
            String filePath = file.getAbsolutePath();
            if (filePath.contains(name)) {
                System.out.println(filePath);
                flag = true;
            }
        }

        return flag;
    }

    // 'tree' command handler
    private static void handleTree(String[] commandStr) {
        // if no arguments provided
        if (commandStr.length == 1) {
            javaTree(currentDirectory, -1, "name");
            return;
        }

        int depth;
        // check that the first argument is a positive integer
        try {
            depth = Integer.parseInt(commandStr[1]);
            if (depth < 1) {
                System.out.println("Second argument has to be greater than 0.");
                return;
            }
        } catch (NumberFormatException ex) {
            System.out.println("Invalid second argument. Only positive integers allowed.");
            return;
        }

        // if only 1 argument provided
        if (commandStr.length == 2) {
            javaTree(currentDirectory, depth, "name");
            return;
        }

        // if 2 arguments provided
        if (commandStr.length == 3) {
            javaTree(currentDirectory, depth, commandStr[2]);
            return;
        }

        // if more than 2 arguments provided
        System.out.println("Too many arguments for 'tree' command. Only maximum of two arguments");
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
        // check if any files exist in the working directory
        if (fileList == null || fileList.length == 0) {
            System.out.println("No files in this directory.");
            return;
        }

        // check for valid depth parameter (-1 for indefinite, >0 for finite)
        if (depth != -1 && depth < 1) {
            System.out.println("Invalid second argument. Depth has to be greater than 0.");
            return;
        }

        // print out the tree result
        StringBuilder strBld = new StringBuilder(".\n");
        strBld = javaTreeHelper(dir, 0, depth, sortMethod, strBld, new HashSet<>());
        System.out.println(strBld.toString());
    }

    /**
     * Helper function to append the output string of the directory tree to the string builder
     *
     * @param dir - current working directory
     * @param indent - indentation for the directory tree
     * @param depthLeft - the depth left to traverse
     * @param sortMethod - method to sort the directory
     * @param strBld - overall string builder
     * @param completeLevels - directory levels that are done traversing
     * @return strBld after appending all the strings
     */
    private static StringBuilder javaTreeHelper(File dir, int indent, int depthLeft, String sortMethod,
                                                StringBuilder strBld, Set<Integer> completeLevels) {
        if (!dir.isDirectory())
            throw new IllegalArgumentException("The file is not a directory.");

        if (depthLeft == 0)
            return strBld;

        File[] fileList = dir.listFiles();
        fileList = sortFileList(fileList, sortMethod);
        if (fileList == null)
            return strBld;

        for (int i = 0; i < fileList.length; i++) {
            File file = fileList[i];
            boolean newIsLast = (i == fileList.length - 1);
            if (newIsLast)
                completeLevels.add(indent);

            if (file.isDirectory()) {
                strBld = appendDirectory(file, indent, strBld, newIsLast, completeLevels);
                strBld = javaTreeHelper(file, indent + 1, depthLeft - 1, sortMethod, strBld, completeLevels);
            } else {
                strBld = appendFile(file, indent, strBld, newIsLast, completeLevels);
            }

            completeLevels.remove(indent);
        }

        return strBld;
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
        // sort the file list based on sort method (name, size or time)
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

    private static StringBuilder appendDirectory(File dir, int indent, StringBuilder strBld, boolean isLast,
                                                 Set<Integer> completeLevels) {
            strBld.append(createIndentStr(indent, completeLevels));
            strBld.append(isLast ? treeBranchEnd : treeBranch);
            strBld.append(dir.getName());
            strBld.append("/");
            strBld.append("\n");

        return strBld;
    }

    private static StringBuilder appendFile(File file, int indent, StringBuilder strBld, boolean isLast,
                                            Set<Integer> completeLevels) {
        strBld.append(createIndentStr(indent, completeLevels));
        strBld.append(isLast ? treeBranchEnd : treeBranch);
        strBld.append(file.getName());
        strBld.append("\n");

        return strBld;
    }

    /**
     * Function to create a string for indenting the tree output
     *
     * @param indent - the number of spaces to indent
     * @return the string of the indent
     */
    private static String createIndentStr(int indent, Set<Integer> completeLevels) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            String str;
            if (completeLevels.contains(i)) {
                str = " ";
            } else {
                str = "\u2502";
            }

            sb.append(String.format("%-" + (treePadding + 1) + "s", str));

        }

        return sb.toString();
    }

}