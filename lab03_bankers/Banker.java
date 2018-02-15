import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

// package Week3;

public class Banker {
    private int numOfCustomers;    // the number of customers
    private int numOfResources;    // the number of resources

    private int[] available;    // the available amount of each resource
    private int[][] maximum;    // the maximum demand of each customer
    private int[][] allocation;    // the amount currently allocated
    private int[][] need;        // the remaining needs of each customer

    /**
     * Constructor for the Banker class.
     *
     * @param resources         An array of the available count for each resource.
     * @param numberOfCustomers The number of customers.
     */
    public Banker(int[] resources, int numberOfCustomers) {
        this.numOfResources = resources.length;
        this.numOfCustomers = numberOfCustomers;
        this.available = resources;

        this.maximum = new int[numOfCustomers][numOfResources];
        this.allocation = new int[numOfCustomers][numOfResources];
        this.need = new int[numOfCustomers][numOfResources];
    }

    /**
     * Sets the maximum number of demand of each resource for a customer.
     *
     * @param customerIndex The customer's index (0-indexed).
     * @param maximumDemand An array of the maximum demanded count for each resource.
     */
    public void setMaximumDemand(int customerIndex, int[] maximumDemand) {
        if (maximumDemand.length != numOfResources)
            throw new IllegalArgumentException("Wrong number of maximum demand resources.");

        for (int i = 0; i < numOfResources; i++) {
            maximum[customerIndex][i] = maximumDemand[i];
            need[customerIndex][i] = maximumDemand[i];
        }
    }

    /**
     * Prints the current state of the bank.
     */
    public void printState() {
        System.out.println("\nAvailable:\n" + Arrays.toString(available));
        System.out.println("\nMaximum:\n" + stringArray(maximum));
        System.out.println("\nAllocation:\n" + stringArray(allocation));
        System.out.println("\nNeed:\n" + stringArray(need) + "\n");
    }

    /**
     * Creates a string from a 2D-array
     */
    private String stringArray(int[][] array) {
        StringBuilder strBld = new StringBuilder();

        for (int i = 0; i < array.length; i++) {
            strBld.append(Arrays.toString(array[i]));
            if (i < array.length - 1)
                strBld.append("\n");
        }

        return strBld.toString();
    }

    /**
     * Requests resources for a customer loan.
     * If the request leave the bank in a safe state, it is carried out.
     *
     * @param customerIndex The customer's index (0-indexed).
     * @param request       An array of the requested count for each resource.
     * @return true if the requested resources can be loaned, else false.
     */
    public synchronized boolean requestResources(int customerIndex, int[] request) {
        if (request.length != numOfResources)
            throw new IllegalArgumentException("Wrong number of request resources.");

        System.out.println("Customer " + customerIndex + " requesting:\n" + Arrays.toString(request));
        // check if each amount requested is greater than the respective need amount
        if (greaterThanArray(request, need[customerIndex]))
            return false;

        // check if each amount requested is greater than the respective available amount
        if (greaterThanArray(request, available))
            return false;

        // allocate the resources as per the request
        for (int k = 0; k < request.length; k++) {
            available[k] -= request[k];
            allocation[customerIndex][k] += request[k];
            need[customerIndex][k] -= request[k];
        }

        // check for safe state
        if (!checkSafe(customerIndex, request)) {
            for (int i = 0; i < request.length; i++) {
                available[i] += request[i];
                allocation[customerIndex][i] -= request[i];
                need[customerIndex][i] += request[i];
            }
            return false;
        }

        return true;
    }

    /**
     * Releases resources borrowed by a customer. Assume release is valid for simplicity.
     *
     * @param customerIndex The customer's index (0-indexed).
     * @param release       An array of the release count for each resource.
     */
    public synchronized void releaseResources(int customerIndex, int[] release) {
        if (release.length != numOfResources)
            throw new IllegalArgumentException("Wrong number of release resources.");

        System.out.println("Customer " + customerIndex + " releasing:\n" + Arrays.toString(release));
        for (int i = 0; i < numOfResources; i++) {
            available[i] += release[i];
            allocation[customerIndex][i] -= release[i];
            need[customerIndex][i] += release[i];
        }
    }

    /**
     * Checks if the request will leave the bank in a safe state.
     *
     * @param customerIndex The customer's index (0-indexed).
     * @param request       An array of the requested count for each resource.
     * @return true if the requested resources will leave the bank in a
     * safe state, else false
     */
    private synchronized boolean checkSafe(int customerIndex, int[] request) {
        if (request.length != numOfResources)
            throw new IllegalArgumentException("Wrong number of request resources.");

        int[] work = Arrays.copyOf(available, available.length);
        boolean[] finish = new boolean[numOfResources];
        for (int i = 0; i < finish.length; i++)
            finish[i] = false;

        for (int i = 0; i < finish.length; i++) {
            if (!finish[i] && !greaterThanArray(need[i], work)) {
                for (int j = 0; j < work.length; j++)
                    work[j] += allocation[i][j];

                finish[i] = true;
                i = -1;
            }
        }

        return true;
    }

    private boolean greaterThanArray(int[] array1, int[] array2) {
        if (array1.length != array2.length)
            throw new IllegalArgumentException("Arrays do not have same length.");

        for (int i = 0; i < array1.length; i++) {
            if (array1[i] <= array2[i])
                return false;
        }

        return true;
    }

    /**
     * Parses and runs the file simulating a series of resource request and releases.
     * Provided for your convenience.
     *
     * @param filename The name of the file.
     */
    public static void runFile(String filename) {
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(filename));
            String line;
            String[] tokens;
            int[] resources;
            int n, m;

            try {
                n = Integer.parseInt(fileReader.readLine().split(",")[1]);
            } catch (Exception e) {
                System.out.println("Error parsing n on line 1.");
                fileReader.close();
                return;
            }

            try {
                m = Integer.parseInt(fileReader.readLine().split(",")[1]);
            } catch (Exception e) {
                System.out.println("Error parsing n on line 2.");
                fileReader.close();
                return;
            }

            try {
                tokens = fileReader.readLine().split(",")[1].split(" ");
                resources = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++)
                    resources[i] = Integer.parseInt(tokens[i]);
            } catch (Exception e) {
                System.out.println("Error parsing resources on line 3.");
                fileReader.close();
                return;
            }

            Banker theBank = new Banker(resources, n);
            int lineNumber = 4;

            try {
                while ((line = fileReader.readLine()) != null) {
                    tokens = line.split(",");
                    switch (tokens[0]) {
                        case "c": {
                            int customerIndex = Integer.parseInt(tokens[1]);
                            tokens = tokens[2].split(" ");
                            resources = new int[tokens.length];
                            for (int i = 0; i < tokens.length; i++)
                                resources[i] = Integer.parseInt(tokens[i]);
                            theBank.setMaximumDemand(customerIndex, resources);

                            break;
                        }

                        case "r": {
                            int customerIndex = Integer.parseInt(tokens[1]);
                            tokens = tokens[2].split(" ");
                            resources = new int[tokens.length];
                            for (int i = 0; i < tokens.length; i++)
                                resources[i] = Integer.parseInt(tokens[i]);
                            theBank.requestResources(customerIndex, resources);

                            break;
                        }

                        case "f": {
                            int customerIndex = Integer.parseInt(tokens[1]);
                            tokens = tokens[2].split(" ");
                            resources = new int[tokens.length];
                            for (int i = 0; i < tokens.length; i++)
                                resources[i] = Integer.parseInt(tokens[i]);
                            theBank.releaseResources(customerIndex, resources);

                            break;
                        }

                        case "p":
                            theBank.printState();
                            break;
                    }
                }
            } catch (Exception e) {
                System.out.println("Error parsing resources on line " + lineNumber + ".");
                e.printStackTrace();
            } finally {
                fileReader.close();
            }
        } catch (IOException e) {
            System.out.println("Error opening: " + filename);
        }

    }

    /**
     * Main function
     *
     * @param args The command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            runFile(args[0]);
        }
    }

}
