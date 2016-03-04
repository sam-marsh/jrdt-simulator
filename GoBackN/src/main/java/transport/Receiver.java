package transport;

/**
 * A network host which receives data from a sender using a reliable stop-and-wait transfer protocol.
 *
 * @author 153728
 */
public class Receiver extends NetworkHost {

    /**
     * The sequence number of the next expected packet. Initially 0.
     */
    private int expectedSeqNum;

    /**
     * The acknowledgement packet for the highest correctly-received packet with the highest
     * in-order sequence number.
     */
    private Packet sendPacket;

    /**
     * {@inheritDoc}
     */
    public Receiver(int entityName) {
        super(entityName);
    }

    @Override
    public void init() {
        //set the expected sequence number of the first packet received
        expectedSeqNum = 1;
        //also prepare the acknowledgement packet for this first packet received
        sendPacket = new Packet(0, expectedSeqNum - 1, Checksum.compute(0, expectedSeqNum - 1));
    }

    @Override
    public void input(Packet packet) {
        if (!Checksum.corrupt(packet) && packet.getSeqnum() == expectedSeqNum) {
            //packet is valid and expected, deliver to application layer and then send ACK
            // for this packet
            deliverData(packet.getPayload());
            sendPacket.setAcknum(expectedSeqNum);
            sendPacket.setChecksum(Checksum.compute(0, expectedSeqNum));
            udtSend(sendPacket);
            ++expectedSeqNum;
        } else {
            udtSend(sendPacket);
        }
    }

}
