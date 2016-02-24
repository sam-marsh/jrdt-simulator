package transport;

public class NetworkHost {

    public static final int MAXDATASIZE = 20;   // This constant controls the maximum size of the buffer in a Message and in a Packet

    private final int entity;                   // can be A or B; i.e. the sender or the receiver

    // Default versions of methods to implement. 
    public void init() {
        throw new UnsupportedOperationException("Init method called for entity " + entity + " but not implemented.");
    }
    
    public void output(Message message) {
        throw new UnsupportedOperationException("Output method called for entity " + entity + " but not implemented.");
    }

    public void input(Packet packet) {
        throw new UnsupportedOperationException("Input method called for entity " + entity + " but not implemented.");
    }

    public void timerInterrupt() {
        throw new UnsupportedOperationException("TimerInterupt method called for entity " + entity + " but not implemented.");
    }

    // initialise NetworkHost to either A or B - the sender or the receiver
    public NetworkHost(int entityName) {
        entity = entityName;
    }

    // Start the timer for this NetworkHost (A or B)
    // Only a single timer is supported for each NetworkHost
    // Attempting to start a second timer will result in just removing and adding the same timer in the EventList
    public void startTimer(double increment) {
        System.out.println("startTimer: starting timer at " + NetworkSimulator.getInstance().getTime());
        
        Event t = NetworkSimulator.getInstance().getEventList().removeTimer(entity);

        if (t != null) {
            System.out.println("startTimer: Warning: Attempting to start a timer that is already running");
            NetworkSimulator.getInstance().getEventList().add(t);
        } else {
            Event timer = new Event(NetworkSimulator.getInstance().getTime() + increment, EventType.TIMERINTERRUPT, entity);
            NetworkSimulator.getInstance().getEventList().add(timer);
        }
    }
    
    // Stop the timer for this NetworkHost (A or B)
    public void stopTimer() {
        System.out.println("stopTimer: stopping timer at " + NetworkSimulator.getInstance().getTime());

        Event timer = NetworkSimulator.getInstance().getEventList().removeTimer(entity);
        if (timer == null) {
            System.out.println("stopTimer: Warning: Unable to cancel your timer, which is not set.");
        }
    }

    protected final void udtSend(Packet p) {
        int destination;
        double arrivalTime;
        
        // Use a copy of the supplied packet at this method may corrupt its data.
        // We want to keep the original copy for retransmission purposes
        Packet packet = new Packet(p);

        System.out.println("udtSend: " + packet);

        // Set destination to be the 'other side' of the network; B if we are A or vice versa.
        switch (entity) {
            case NetworkSimulator.A:
                destination = NetworkSimulator.B;
                break;
            case NetworkSimulator.B:
                destination = NetworkSimulator.A;
                break;
            default:
                System.out.println("udtSend: Warning: invalid packet sender");
                return;
        }

        // Simulate losses by doing nothing
        if (NetworkSimulator.getInstance().getRand().nextDouble() < NetworkSimulator.getInstance().getLossProb()) {
            System.out.println("udtSend: simulating packet being lost");
            return;
        }

        // Simulate corruption
        if (NetworkSimulator.getInstance().getRand().nextDouble() < NetworkSimulator.getInstance().getCorruptProb()) {
            System.out.println("udtSend: packet being corrupted");

            double x = NetworkSimulator.getInstance().getRand().nextDouble();
            if (x < 0.75) {
                // corrupt the payload - by changing the first character
                String payload = packet.getPayload();

                if (payload.length() < 2) {
                    payload = "=";
                } else {
                    //payload = "?" + payload.substring(payload.length() - 1);
                    payload = "=" + payload.substring(1);

                }
                packet.setPayload(payload);
            } else if (x < 0.875) {
                // corrupt the sequence number
                packet.setSeqnum(Math.abs(NetworkSimulator.getInstance().getRand().nextInt()));
            } else {
                // corrupt the acknowledgment number
                packet.setAcknum(Math.abs(NetworkSimulator.getInstance().getRand().nextInt()));
            }
        }

        // Decide when the packet will arrive.  Since the medium cannot reorder, the packet will arrive 1 to 10 time units after the last packet sent by this sender
        arrivalTime = NetworkSimulator.getInstance().getEventList().getLastPacketTime(destination);

        if (arrivalTime <= 0.0) {
            arrivalTime = NetworkSimulator.getInstance().getTime();
        }

        //arrivalTime = arrivalTime + 1.0 + (rand.nextDouble() * 19.0);
        arrivalTime = arrivalTime + 1.0 + Math.abs(5.0 * NetworkSimulator.getInstance().getRand().nextGaussian() + 9.0);

        // Finally, create and schedule this event
        System.out.println("udtSend: Scheduling arrival on other side");
        
        Event arrival = new Event(arrivalTime, EventType.FROMNETWORK, destination, packet);
        NetworkSimulator.getInstance().getEventList().add(arrival);
    }

    public void deliverData(String dataSent) {
        System.out.print("deliverData: data received at " + entity + ":");
        System.out.println(dataSent);
    }
}
