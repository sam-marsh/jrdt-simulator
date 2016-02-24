package transport;

/**
 * A network host which receives data from a sender
 * using a reliable stop-and-wait transfer protocol.
 *
 * @author 153728
 */
public class Receiver extends NetworkHost {

    private int expectedSeqNum;
    private Packet sendPacket;

    /**
     * {@inheritDoc}
     */
    public Receiver(int entityName) {
        super(entityName);
    }

    @Override
    public void init() {
        expectedSeqNum = 1;
        sendPacket = new Packet(0, expectedSeqNum, Checksum.compute(0, expectedSeqNum));
    }

    @Override
    public void input(Packet packet) {
        if (!Checksum.corrupt(packet) && packet.getSeqnum() == expectedSeqNum) {
            deliverData(packet.getPayload());
            sendPacket.setAcknum(expectedSeqNum);
            sendPacket.setChecksum(Checksum.compute(sendPacket.getSeqnum(), sendPacket.getAcknum()));
            udtSend(sendPacket);
            ++expectedSeqNum;
        } else {
            udtSend(sendPacket);
        }
    }

}
