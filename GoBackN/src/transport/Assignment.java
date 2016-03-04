package transport;

import java.io.*;

public class Assignment {

    public final static void main(String[] argv) throws IOException {
        NetworkSimulator simulator;

        String buffer;

        int nMsgSim;
        double loss;
        double corrupt;
        double delay;
        long seed;

        // initialise a BufferedReader to read from the standard input (keyboard)
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Network Simulator");

        System.out.print("Enter number of messages to simulate (> 0): ");
        buffer = stdIn.readLine();
        nMsgSim = Integer.parseInt(buffer);
        if (nMsgSim <= 0) {
            System.err.println("Number of Messages must be > 0");
            System.exit(-1);
        }

        System.out.print("Enter the packet loss probability (0.0 for no " + "loss): ");
        buffer = stdIn.readLine();
        loss = Double.valueOf(buffer);
        if ((loss < 0) || (loss > 1)) {
            System.err.println("packet loss probability must be > 0.0 and < 1.0");
            System.exit(-1);
        }

        System.out.print("Enter the packet corruption probability (0.0 " + "for no corruption): ");
        buffer = stdIn.readLine();
        corrupt = Double.valueOf(buffer);
        if ((corrupt < 0) || (corrupt > 1)) {
            System.err.println("packet corruption probability must be > 0.0 and < 1.0");
            System.exit(-1);
        }

        System.out.print("Enter the average time between messages from the sender's application layer (> 0.0): ");
        buffer = stdIn.readLine();
        delay = Double.valueOf(buffer);
        if (delay < 0) {
            System.err.println("Number of Messages must be > 0.0");
            System.exit(-1);
        }

        System.out.print("Enter random seed: ");
        buffer = stdIn.readLine();
        seed = Long.valueOf(buffer);

        // Instantiate the single instance of the NetworkSimulator
        simulator = NetworkSimulator.getInstance();

        // Initialise the simulator
        simulator.initSimulator(nMsgSim, loss, corrupt, delay, seed);

        // Run the simulator
        simulator.runSimulator();
    }
}
