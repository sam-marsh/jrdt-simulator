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

    private SenderState state;
    private int seq;
    private Packet sendPacket;

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
        state = SenderState.WAIT_MSG;
        seq = 0;
        sendPacket = null;
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
        if (state != SenderState.WAIT_MSG) {
            throw new IllegalStateException(); //TODO
        }
        //compute checksum and create new packet
        int check = Checksum.compute(seq, 0, message.getData());
        sendPacket = new Packet(seq, 0, check, message.getData());

        //send packet unreliably, and also start the timer now
        udtSend(sendPacket);
        startTimer(TIMER_LENGTH);

        //transition to the next state, which waits until acknowledgement
        //is received from the client or until the above timer expires
        state = SenderState.WAIT_ACK;
    }

    /**
     * Callback function which is invoked when the sender
     * receives an acknowledgement packet from the receiver.
     *
     * @param packet the received packet
     */
    @Override
    public void input(Packet packet) {
        if (state != SenderState.WAIT_ACK) {
            return; //TODO check
        }
        //check if received packet is 'wrong' - i.e. is corrupt or
        //is an acknowledgement of the wrong packet
        if (Checksum.corrupt(packet) || packet.getAcknum() != seq) {
            //if so, ignore and stay in same state - wait for another packet
            //or for the timer to expire
            return;
        }
        //received a valid ACK - stop the timer and transition to the application
        //message waiting state (with an alternating sequence number 1 -> 0, 0 -> 1)
        stopTimer();
        state = SenderState.WAIT_MSG;
        seq = (seq + 1) % 2;
    }

    /**
     * Callback function which is invoked when the timer
     * expires. This means that there has been a timeout
     * in waiting for a response from the receiver, so
     * we need to resend the packet.
     */
    @Override
    public void timerInterrupt() {
        udtSend(sendPacket);
        startTimer(TIMER_LENGTH);
    }

    private enum SenderState {
        WAIT_MSG,
        WAIT_ACK
    }

}
