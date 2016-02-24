package transport;

/**
 * @author Sam Marsh
 */
public class Checksum {

    public static int compute(int seq, int ack) {
        return Checksum.compute(seq, ack, null);
    }

    public static int compute(int seq, int ack, String payload) {
        return (33 * seq) + (57 * ack) + (payload == null ? 0 : payload.chars().sum());
    }

    public static boolean corrupt(Packet packet) {
        return packet.getChecksum() != Checksum.compute(packet.getSeqnum(), packet.getAcknum(), packet.getPayload());
    }

}
