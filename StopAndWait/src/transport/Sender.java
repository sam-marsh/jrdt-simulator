package transport;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A network host which sends data to a receiver using a reliable stop-and-wait transfer protocol.
 *
 * @author 153728
 */
public class Sender extends NetworkHost {

    /**
     * The maximum time to wait for a response after sending a packet.
     */
    private static final int TIMER_LENGTH = 40;

    /**
     * The current state of this finite-state machine. The two possible states are waiting for a message from the
     * application layer, and waiting for an acknowledgement packet from the receiver host.
     */
    private SenderState state;

    /**
     * The sequence number of the packet currently being sent through the network.
     */
    private int seq;

    /**
     * The packet currently being sent through the network. Stored in a field so that upon timeout the packet can be
     * re-sent to the receiver.
     */
    private Packet sendPacket;

    /**
     * {@inheritDoc}
     */
    public Sender(int entityName) {
        super(entityName);
    }

    /**
     * Callback function which initialises the state of the sender. The sender initially waits for a message from the
     * application layer. The initial sequence number is zero.
     */
    @Override
    public void init() {
        state = SenderState.WAIT_MSG;
        seq = 0;
        sendPacket = null;
    }

    /**
     * Handles reliable transport of an application message through the network to a receiving host. Note: this
     * implementation ignores application messages while awaiting acknowledgement from the receiver for the last
     * packet sent.
     *
     * @param message the message to send
     */
    @Override
    public void output(Message message) {
        if (state != SenderState.WAIT_MSG) {
            //if we're currently in the middle of sending another packet, warn and drop the message
            Logger.getLogger(Sender.class.getName()).log(Level.WARNING, "dropped message: {0}", message);
            return;
        }

        //compute checksum and create new packet
        int check = Checksum.compute(seq, 0, message.getData());
        sendPacket = new Packet(seq, 0, check, message.getData());

        //send packet unreliably, and also start the timer now
        udtSend(sendPacket);
        startTimer(TIMER_LENGTH);

        //transition to the next state, which waits until acknowledgement is received from the client or until the
        // above timer expires
        state = SenderState.WAIT_ACK;
    }

    /**
     * Handles a new incoming packet. The packet is ignored if currently in the {@link SenderState#WAIT_MSG} state or
     * if the packet is corrupt/is an acknowledgement for a previous packet. Otherwise, this sender will prepare for
     * the next message from the application layer.
     *
     * @param packet the received packet
     */
    @Override
    public void input(Packet packet) {
        if (state != SenderState.WAIT_ACK) {
            //event only handled when waiting for acknowledgement - ignore otherwise
            return;
        }

        //check if received packet is 'wrong' - i.e. is corrupt or is an acknowledgement of the wrong packet
        if (Checksum.corrupt(packet) || packet.getAcknum() != seq) {
            //if so, ignore and stay in same state - wait for another packet or for the timer to expire
            return;
        }

        //received a valid ACK - stop the timer and transition to the application message waiting state (with an
        // alternating sequence number 1 -> 0, 0 -> 1)
        stopTimer();
        state = SenderState.WAIT_MSG;
        seq = (seq + 1) % 2;
    }

    /**
     * Callback function which is invoked when the timer expires. This means that there has been a timeout in waiting
     * for a response from the receiver, so the packet is re-sent.
     */
    @Override
    public void timerInterrupt() {
        if (state != SenderState.WAIT_ACK) {
            //event only handled when in the acknowledgment waiting state - ignore otherwise
            return;
        }

        //resend the packet and restart the timer
        udtSend(sendPacket);
        startTimer(TIMER_LENGTH);
    }

    /**
     * Holds the possible states of this finite-state machine.
     */
    private enum SenderState {

        /**
         * Represents the state in which this network host waits
         * for a message from the application layer.
         */
        WAIT_MSG,

        /**
         * Represents the state in which this network host waits
         * for an acknowledgement packet from the receiver for a
         * data packet that this host has sent.
         */
        WAIT_ACK

    }

}
