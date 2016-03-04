package transport;

import java.util.Random;

public class NetworkSimulator {

    // This constant controls the maximum size of the buffer in a Message and in a Packet
    public static final int MAXDATASIZE = 20;

    // These constants represent our sender (A) and receiver (B)
    public static final int A = 12345;
    public static final int B = 67890;

    // The simulator supports only a single sender host (A)
    private Sender sender;
    
    // The simulator supports only a single receiver host (B)
    private Receiver receiver;

    private int maxMessages;            // the maximum number of messages the sender application is allowed to send
    
    private double lossProb;            // the probability that a packet will be "lost"
    private double corruptProb;         // the probability that a packet will be "corrupted"
    
    private double avgMessageDelay;     // the average message delay between messages sent by the application at the sender side
        
    private Random rand;                // A random number generator

    private int nMsgSim;                // number of simulated messages
    private double time;                // the simulated time
    
    private EventList eventList;        // The list of events that the simulator needs to process

    // NetworkSimulator is a Singleton Class. It can only be instantiated once, through the getInstance() method.
    // Accessing members of the NetworkSimulator class (from any other class) is done through calling getInstance().
    private static NetworkSimulator instance = null;

    private NetworkSimulator() {
        // Exists only to defeat instantiation.
        // It is private and can only be called by getIstance() below
    }

    // the only way to instantiate and, later, get member values of the NetworkSimulation singleton object
    // Can only instantiate once
    public static NetworkSimulator getInstance() {
        if (instance == null) {
            instance = new NetworkSimulator();
        }
        return instance;
    }

    // initialise the NetworkSimulator
    public void initSimulator(int maxMsgs, double loss, double corrupt, double delay, long seed) {
        maxMessages = maxMsgs;
        
        lossProb = loss;
        corruptProb = corrupt;
        
        avgMessageDelay = delay;

        rand = new Random(seed);        // instantiate Random number generator with provided seed

        nMsgSim = 0;                    // initialise number of simulated messages to 0
        time = 0.0;                     // initialise simulation time to 0

        eventList = new EventList();    // instantiate event list (initially empty)
        
        sender = new Sender(A);         // initialise sender entity (the respective class)

        receiver = new Receiver(B);     // initialise receiver entity (the respective class)
    }

    // The main simulator loop - everything happens here!
    // When the method exits, the simulator is over and the Java program stops
    public void runSimulator() {
        Event next;                     // the next Event to process

        sender.init();                  // Students: You will override this method (defined in the NetworkHost) to initialise your Sender.
        receiver.init();                // Students: You will override this method (defined in the NetworkHost) to initialise your Receiver.

        // Start the whole thing off by scheduling a new message from the simulated application
        // Calling this method will add a new FROMAPP Event. 
        // When this Event is executed, your code will be invoked so that the message can be sent to the Receiver
        generateNextArrival();

        // Begin the main simulation loop
        while (true) {
            // Get the next (with respect to the scheduled time) event in the list
            next = eventList.removeNext();

            if (next == null) {
                // we run out of events - exit - end of simulation
                break;
            }

            System.out.println();
            System.out.print("EVENT time: " + next.getTime());
            System.out.print("  type: " + next.getType());
            System.out.println("  entity: " + next.getEntity());

            // Advance the simulator's time to be the scheduled time of the next event
            time = next.getTime();

            // Perform the appropriate action based on the event 
            switch (next.getType()) {
                case TIMERINTERRUPT:
                    if (next.getEntity() == A) {
                        sender.timerInterrupt();
                    } else {
                        System.out.println("INTERNAL PANIC: Timeout for invalid entity");
                    }
                    break;
                case FROMNETWORK:
                    switch (next.getEntity()) {
                        case A:
                            sender.input(next.getPacket());
                            break;
                        case B:
                            receiver.input(next.getPacket());
                            break;
                        default:
                            System.out.println("INTERNAL PANIC: Packet has " + "arrived for unknown entity");
                            break;
                    }
                    break;
                case FROMAPP:                 
                    char[] nextMessage = new char[MAXDATASIZE];

                    // Now, let's generate the contents of this message
                    char j = (char) (((nMsgSim - 1) % 26) + 97);
                    for (int i = 0; i < MAXDATASIZE; i++) {
                        nextMessage[i] = j;
                    }

                    // Let the student handle the new message
                    sender.output(new Message(new String(nextMessage)));
                    
                    // If a message has arrived from sending process, we need to schedule the arrival of the next message
                    // If we've reached the maximum message count, exit the main loop
                    if (nMsgSim < maxMessages) {
                        generateNextArrival();
                    } else {
                        // do not schedule more FROMAPP events from the application layer if we reached the maximum number of messages
                    }
                    
                    break;
                default:
                    System.out.println("INTERNAL PANIC: Unknown event type");
            }
        }

    }

    // Generate the next arrival and add it to the event list
    private void generateNextArrival() {
        System.out.println("generateNextArrival(): called");

        // arrival time 'x' is uniform on [0, 2 * avgMessageDelay] having mean of avgMessageDelay.
        // rand is used to provide the required uniformness
        double x = 0.5 * avgMessageDelay + avgMessageDelay * rand.nextDouble();

        // Instantiate a new FROMAPP Event
        Event next = new Event(time + x, EventType.FROMAPP, A);

        // Add the newly instantiated Event to the EventList
        eventList.add(next);
        
        // Increment the message counter
        nMsgSim++;
        
        System.out.println("generateNextArrival(): time is " + time);
        System.out.println("generateNextArrival(): future time for " + "event " + next.getType() + " at entity " + next.getEntity() + " will be " + next.getTime());

    }

    public double getLossProb() {
        return lossProb;
    }

    public double getCorruptProb() {
        return corruptProb;
    }

    public double getAvgMessageDelay() {
        return avgMessageDelay;
    }

    public EventList getEventList() {
        return eventList;
    }

    public Random getRand() {
        return rand;
    }

    public int getnSim() {
        return nMsgSim;
    }
    
    public double getTime() {
        return time;
    }

}
