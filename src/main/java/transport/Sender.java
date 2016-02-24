package transport;

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
     *
     */
    private class WaitForAcknowledgementPacket implements SenderEventHandler {

        private final int seq;
        private final Packet packet;

        public WaitForAcknowledgementPacket(int seq, Packet packet) {
            this.seq = seq;
            this.packet = packet;
        }

        @Override
        public SenderEventHandler input(Packet packet) {
            if (Checksum.corrupt(packet) || packet.getAcknum() != seq) {
                return this;
            }
            stopTimer();
            return new WaitForApplicationMessage((seq + 1) % 2);
        }

        @Override
        public SenderEventHandler timerInterrupt() {
            udtSend(packet);
            startTimer(TIMER_LENGTH);
            return this;
        }

    }

    private interface SenderEventHandler {

        default SenderEventHandler output(Message message) {
            return this;
        }

        default SenderEventHandler input(Packet packet) {
            return this;
        }

        default SenderEventHandler timerInterrupt() {
            return this;
        }

    }

}
