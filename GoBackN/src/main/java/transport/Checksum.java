package transport;

/**
 * A utility class for computing and verifying a packet's checksum.
 *
 * @author 153728
 */
public class Checksum {

    private Checksum() {
        throw new AssertionError("instantiating utility class");
    }

    /**
     * Calculates the checksum for a given sequence and acknowledgement
     * number, for a packet with no payload.
     *
     * @param seq the sequence number of the packet
     * @param ack the acknowledgement number of the packet
     * @return the checksum of the packet
     */
    public static int compute(int seq, int ack) {
        return Checksum.compute(seq, ack, null);
    }

    /**
     * Calculates the checksum for a given sequence number, acknowledgement number
     * and payload.
     *
     * @param seq the sequence number of the packet
     * @param ack the acknowledgement number of the packet
     * @param payload the payload of the packet
     * @return the checksum of the packet
     */
    public static int compute(int seq, int ack, String payload) {
        int total = seq + ack;

        if (payload != null) {
            //sum over integer value of characters in payload
            char[] chars = payload.toCharArray();
            for (char c : chars) {
                total += c;
            }
        }

        return ~total;
    }

    /**
     * Checks if a packet is corrupted by comparing the packet contents to the checksum.
     *
     * @param packet the packet to check
     * @return true if the packet is corrupt, otherwise false
     */
    public static boolean corrupt(Packet packet) {
        return packet.getChecksum() != Checksum.compute(packet.getSeqnum(), packet.getAcknum(), packet.getPayload());
    }

}
