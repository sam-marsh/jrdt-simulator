package transport;

/**
 * A network host which receives data from a sender using a reliable stop-and-wait
 * transfer protocol.
 *
 * @author 153728
 */
public class Receiver extends NetworkHost {

    /**
     * The sequence number of the awaited packet.
     */
    private int expectedSeq;

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
        expectedSeq = 0;
    }

    /**
     * Callback function which is invoked when this host receives a
     * new packet from the sender. If the packet is not corrupt and
     * is the expected packet, the data is delivered to the application layer.
     * Otherwise, the expected packet is requested from the sender.
     *
     * @param packet the received packet
     */
    @Override
    public void input(Packet packet) {
        //check if packet is corrupt or not the right one - if so, send ACK
        //with other sequence number to get the expectedSeq packet to be resent
        if (Checksum.corrupt(packet) || packet.getSeqnum() != expectedSeq) {
            int ackPrev = (expectedSeq + 1) % 2;
            Packet prev = new Packet(0, ackPrev, Checksum.compute(0, ackPrev));
            udtSend(prev);
            //continue waiting in this state
            return;
        }

        //send data up to the application layer - if control flow reaches here
        //the data is most likely not corrupt
        deliverData(packet.getPayload());

        //send ACK back to sender for this packet
        Packet ackPacket = new Packet(0, expectedSeq, Checksum.compute(0, expectedSeq));
        udtSend(ackPacket);

        //switch into same state, but now waiting for next sequence number
        //(1 if currently 0, 0 if currently 1)
        expectedSeq = (expectedSeq + 1) % 2;
    }

}
