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
        expectedSeqNum = 0;
        sendPacket = new Packet(0, expectedSeqNum, Checksum.compute(0, expectedSeqNum));
    }

    @Override
    public void input(Packet packet) {
        if (!Checksum.corrupt(packet) && packet.getSeqnum() == expectedSeqNum) {
            System.out.flush();
            System.err.println("RECEIVER: GOOD: Received packet " + packet.getSeqnum());
            System.err.flush();
            System.out.flush();
            System.err.println("RECEIVER: " + packet.getPayload());
            System.err.flush();
            deliverData(packet.getPayload());
            sendPacket.setAcknum(expectedSeqNum);
            sendPacket.setChecksum(Checksum.compute(sendPacket.getSeqnum(), sendPacket.getAcknum()));
            udtSend(sendPacket);
            ++expectedSeqNum;
        } else {
            System.out.flush();
            System.err.println("RECEIVER: BAD: Received packet " + packet.getSeqnum());
            System.err.flush();
            udtSend(sendPacket);
        }
    }

}
