package transport;

/**
 * Represents a Packet sent in the (simulated) network. 
 * The network consists only of Sender A and Receiver B
 */
public class Packet {

    private int seqnum;     // sequence number
    private int acknum;     // acknowledgment number
    private int checksum;   // checksum
    private String payload; // packet payload

    public Packet(Packet p) {
        seqnum = p.getSeqnum();
        acknum = p.getAcknum();
        checksum = p.getChecksum();
        payload = p.getPayload();
    }
    
    Packet (int seq, int ack, int check) {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        payload = "";
    }
            
    Packet (int seq, int ack, int check, String pld) {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        payload = pld;
    }

    public int getSeqnum() {
        return seqnum;
    }

    public void setSeqnum(int seqnum) {
        this.seqnum = seqnum;
    }

    public int getAcknum() {
        return acknum;
    }

    public void setAcknum(int acknum) {
        this.acknum = acknum;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return ("seqnum: " + seqnum + "  acknum: " + acknum + "  checksum: " + checksum + "  payload: " + payload);
    }

}
