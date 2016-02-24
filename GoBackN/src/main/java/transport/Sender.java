package transport;

/**
 * A network host which sends data to a receiver
 * using a reliable stop-and-wait transfer protocol.
 *
 * @author 153728
 */
public class Sender extends NetworkHost {

    /**
     * The maximum time to wait for a response after sending a packet.
     */
    private static final int TIMER_LENGTH = 50;

    /**
     * The current state of this finite-state machine. The actions
     * taken on network events depend on the current state.
     */
    private SenderEventHandler state;

    /**
     * {@inheritDoc}
     */
    public Sender(int entityName) {
        super(entityName);
    }

    /**
     * Callback function which initialises the state of the sender.
     * The sender initially waits for a message from the application
     * layer. The initial sequence number is zero.
     */
    @Override
    public void init() {
        state = new WaitForApplicationMessage(0);
    }

    /**
     * Callback function which is invoked when the application
     * requires reliable transport of a message through the
     * network.
     *
     * @param message the message to send
     */
    @Override
    public void output(Message message) {
        state = state.output(message);
    }

    /**
     * Callback function which is invoked when the sender
     * receives an acknowledgement packet from the receiver.
     *
     * @param packet the received packet
     */
    @Override
    public void input(Packet packet) {
        state = state.input(packet);
    }

    /**
     * Callback function which is invoked when the timer
     * expires. This means that there has been a timeout
     * in waiting for a response from the receiver, so
     * we need to resend the packet.
     */
    @Override
    public void timerInterrupt() {
        state = state.timerInterrupt();
    }

    /**
     * Describes the state where this host is awaiting a message
     * from the application layer.
     */
    private class WaitForApplicationMessage implements SenderEventHandler {

        /**
         * The current sequence number, which will be attached to the
         * next packet sent through the network.
         */
        private final int seq;

        /**
         * Creates a new state (event handler) which awaits a message
         * from the application layer and sends the message through the
         * network.
         *
         * @param seq the sequence number which will be used to send the packet.
         */
        public WaitForApplicationMessage(int seq) {
            this.seq = seq;
        }

        /**
         * Sends the given message through the unreliable transport medium.
         * In addition, this method starts a timer which waits for a certain
         * amount of time since the packet was sent before expiring.
         *
         * @param message the message to send
         * @return the state that waits for acknowledgement of this packet from the receiver
         */
        @Override
        public SenderEventHandler output(Message message) {

            //compute checksum and create new packet
            int check = Checksum.compute(seq, 0, message.getData());
            Packet packet = new Packet(seq, 0, check, message.getData());

            //send packet unreliably, and also start the timer now
            udtSend(packet);
            startTimer(TIMER_LENGTH);

            //transition to the next state, which waits until acknowledgement
            //is received from the client or until the above timer expires
            return new WaitForAcknowledgementPacket(seq, packet);
        }

    }

    /**
     * Describes the state where this host is awaiting an acknowledgement
     * packet from the receiver for a packet that this host has sent.
     */
    private class WaitForAcknowledgementPacket implements SenderEventHandler {

        /**
         * The sequence number of the packet sent. This should match the
         * ACK number from the receiver.
         */
        private final int seq;

        /**
         * The packet sent, for convenience - to resend through the network
         * upon timeout if required.
         */
        private final Packet packet;

        /**
         * Creates a new instance of this event handler to await receipt of a
         * sent packet from the receiver.
         *
         * @param seq the sequence number of the packet sent
         * @param packet the packet sent
         */
        public WaitForAcknowledgementPacket(int seq, Packet packet) {
            this.seq = seq;
            this.packet = packet;
        }

        /**
         * Handles an incoming packet from the receiver.
         *
         * @param packet the received packet
         * @return the state to transition into next - the same state if the packet
         * is wrong/corrupt, or the application message waiting state if the correct
         * acknowledgement is received.
         */
        @Override
        public SenderEventHandler input(Packet packet) {
            //check if received packet is 'wrong' - i.e. is corrupt or
            //is an acknowledgement of the wrong packet
            if (Checksum.corrupt(packet) || packet.getAcknum() != seq) {
                //if so, ignore and stay in same state - wait for another packet
                //or for the timer to expire
                return this;
            }
            //received a valid ACK - stop the timer and transition to the application
            //message waiting state (with an alternating sequence number 1 -> 0, 0 -> 1)
            stopTimer();
            return new WaitForApplicationMessage((seq + 1) % 2);
        }

        /**
         * Handles a timeout - if this is called it means that we have not received
         * a valid acknowledgement from the receiver for the current packet within
         * a certain time frame, and we need to resend the packet.
         *
         * @return the same state - continue to wait for the same packet
         */
        @Override
        public SenderEventHandler timerInterrupt() {
            udtSend(packet);
            startTimer(TIMER_LENGTH);
            return this;
        }

    }

    /**
     * Provides responses to network events. Acts as a state in a
     * finite-state machine, where each event handler can respond
     * to network events in different ways. The returned value from
     * each method is the state to transition to after handling the event.
     * By default, if an event handler is not implemented by a subclass,
     * the handler will do nothing and return the same state.
     */
    private interface SenderEventHandler {

        /**
         * Called upon receiving a message from the application layer.
         *
         * @param message the message to send through the network
         * @return the state to transition after handling the event
         */
        default SenderEventHandler output(Message message) {
            return this;
        }

        /**
         * Called upon receiving a packet from the network layer.
         *
         * @param packet the received packet
         * @return the state to transition after handling the event
         */
        default SenderEventHandler input(Packet packet) {
            return this;
        }

        /**
         * Called upon timer expiration.
         *
         * @see #startTimer(double)
         * @return the state to transition after handling the event
         */
        default SenderEventHandler timerInterrupt() {
            return this;
        }

    }

}
