package transport;

/**
 * A network host which receives data from a sender
 * using a reliable stop-and-wait transfer protocol.
 *
 * @author 153728
 */
public class Receiver extends NetworkHost {

    /**
     * The current state of this finite-state machine. The actions
     * taken on network events depend on the current state.
     */
    private ReceiverEventHandler state;

    /**
     * {@inheritDoc}
     */
    public Receiver(int entityName) {
        super(entityName);
    }

    /**
     * Callback function which initialises the state of the receiver.
     * For this stop-and-wait protocol, the receiver has one state which
     * awaits a packet from the sender. The initial expected sequence
     * number is zero.
     */
    @Override
    public void init() {
        state = new WaitForPacket(0);
    }

    /**
     * Callback function which is invoked when this host receives a
     * new packet from the sender. This event is delegated to the
     * current state's event handler.
     *
     * @param packet the received packet
     */
    @Override
    public void input(Packet packet) {
        state = state.input(packet);
    }

    /**
     * Describes the state in which this receiver awaits
     * a packet from the sender.
     */
    private class WaitForPacket implements ReceiverEventHandler {

        /**
         * The awaited packet's sequence number.
         */
        private final int expected;

        /**
         * Creates a new instance of this event handler which
         * waits for the packet from the sender with a given sequence number.
         *
         * @param expected the sequence number of the packet to await
         */
        public WaitForPacket(int expected) {
            this.expected = expected;
        }

        /**
         * Called when a new packet is received. If the packet is not corrupt
         * and has the expected sequence number, an acknowledgement packet is
         * sent back to the sender. Otherwise, an acknowledgement packet for
         * the previous packet is sent, causing the sender to (re-)send the awaited
         * packet.
         *
         * @param packet the packet received from the network
         * @return the state to transition into - either the same state, or a new instance
         * of the same with the next sequence number (0 -> 1, 1 -> 0).
         */
        @Override
        public ReceiverEventHandler input(Packet packet) {
            //check if packet is corrupt or not the right one - if so, send ACK
            //with other sequence number to get the expected packet to be resent
            if (Checksum.corrupt(packet) || packet.getSeqnum() != expected) {
                int ackPrev = (expected + 1) % 2;
                Packet prev = new Packet(0, ackPrev, Checksum.compute(0, ackPrev));
                udtSend(prev);
                //continue waiting in this state
                return this;
            }

            //send data up to the application layer - if control flow reaches here
            //the data is most likely not corrupt
            deliverData(packet.getPayload());

            //send ACK back to sender for this packet
            Packet ackPacket = new Packet(0, expected, Checksum.compute(0, expected));
            udtSend(ackPacket);

            //switch into same state, but now waiting for next sequence number
            //(1 if currently 0, 0 if currently 1)
            return new WaitForPacket((expected + 1) % 2);
        }

    }

    /**
     * Provides responses to network events. Acts as a state in a
     * finite-state machine, where each event handler can respond
     * to network events in different ways. The returned value from
     * the method is the state to transition to after handling the event.
     * By default, if an event handler is not implemented by a subclass,
     * the handler will do nothing and return the same state.
     */
    private interface ReceiverEventHandler {

        /**
         * Called upon receiving a packet from the network layer.
         *
         * @param packet the received packet
         * @return the state to transition after handling the event
         */
        default ReceiverEventHandler input(Packet packet) {
            return this;
        }

    }

}
