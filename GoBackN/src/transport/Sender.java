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
    private static final int TIMER_LENGTH = 50;

    /**
     * The number of consecutive unacknowledged packets allowed to be in transit at one time.
     */
    private static final int WINDOW_SIZE = 8;

    /**
     * The size of the packet buffer. When {@link #WINDOW_SIZE} packets are waiting for acknowledgement
     * from the receiver, there are a further available 50 slots available for buffering messages 
     * from the application layer. After these slots are filled, any further messages from the application 
     * layer will cause an exception to be thrown.
     */
    private static final int BUFFER_SIZE = WINDOW_SIZE + 50 + 1;
    
    /**
     * The index of the start of the window in the packet buffer.
     */
    private int base;

    /**
     * The sequence number of the next packet to be sent through to the receiver.
     */
    private int nextSeqNum;

    /**
     * The packets that are currently unacknowledged as sent, awaiting a response from the receiver.
     */
    private Packet[] buffer;

    /**
     * {@inheritDoc}
     */
    public Sender(int entityName) {
        super(entityName);
    }

    /**
     * Callback function which initialises the state of the sender. The sender initially waits for a message from the
     * application layer. The initial sequence number is 1.
     */
    @Override
    public void init() {
        //set up the initial sequence number - must be same as receiver side expected value
        base = nextSeqNum = 1;
        buffer = new Packet[BUFFER_SIZE];
    }

    /**
     * Handles reliable transport of an application message through the network to a receiving host. Note: this
     * implementation buffers messages if there are more than {@link #WINDOW_SIZE} packets currently waiting
     * for acknowledgement from the receiver. The buffer size is set by {@link #BUFFER_SIZE}, and once this buffer
     * is full the sender will drop the packets and print a warning.
     *
     * @param message the message to send
     */
    @Override
    public void output(Message message) {
        //if the index of the next sequence number is directly below the window start, it means
        // that we have run out of buffer space.
        if (index(nextSeqNum) == index(base - 1)) {
            Logger.getLogger(Sender.class.getName()).log(Level.WARNING, "dropped message: {0}", message);
            return;
        }

        //free space in the buffer, so store it
        buffer[index(nextSeqNum)] = makePacket(nextSeqNum, message.getData());

        if (nextSeqNum < base + WINDOW_SIZE) {
            //window has free spots - send it now as well
            udtSend(buffer[index(nextSeqNum)]);
            if (nextSeqNum == base) {
                startTimer(TIMER_LENGTH);
            }
        }

        ++nextSeqNum;
    }

    /**
     * Handles a new incoming packet from the receiver. If this causes buffered packets to enter
     * the sending window, they will be sent to the receiver.
     *
     * @param packet the received packet
     */
    @Override
    public void input(Packet packet) {
        if (Checksum.corrupt(packet))
            return;

        int newBase = packet.getAcknum() + 1;

        //only consider packets where the ACK is inside the current window, otherwise
        // ignore and just wait for the timer to run out
        if (base < newBase) {

            //received a packet which will update the state, so stop the timer - may turn
            // it on again, see below
            stopTimer();

            //step the window along one by one until we reach the new position - check for
            // buffered packets entering the window and send them
            while (base < newBase) {
                Packet curr = buffer[index(base + WINDOW_SIZE)];
                //check if a packet exists at this slot and that the sequence number is valid
                if (curr != null && curr.getSeqnum() == base) {
                    udtSend(curr);
                }
                ++base;
            }

            //unless there are no packets in transit, restart the timer
            if (base != nextSeqNum) {
                startTimer(TIMER_LENGTH);
            }
        }
    }

    /**
     * Callback function which is invoked when the timer expires. This means that there has been a timeout in waiting
     * for a response from the receiver, so all packets in the window are sent again.
     */
    @Override
    public void timerInterrupt() {
        //restart the timer and resend ALL packets in the window
        startTimer(TIMER_LENGTH);
        for (int i = base; i < nextSeqNum && i < base + WINDOW_SIZE; ++i) {
            udtSend(buffer[index(i)]);
        }
    }

    /**
     * Finds the index in the buffer given a sequence number.
     *
     * @param seq the sequence number of the packet
     * @return the sequence number's index in the buffer
     */
    private int index(int seq) {
        //perform POSITIVE modulo arithmetic in case a negative number could be passed - e.g.
        // when calling #index(someSeq - 1)
        return (seq % buffer.length + buffer.length) % buffer.length;
    }

    /**
     * Creates a new packet and computes the checksum.
     *
     * @param seq the sequence number of the packet
     * @param payload the payload of the packet
     * @return a packet instance, with checksum calculated
     */
    private Packet makePacket(int seq, String payload) {
        int checksum = Checksum.compute(seq, 0, payload);
        return new Packet(seq, 0, checksum, payload);
    }

}
