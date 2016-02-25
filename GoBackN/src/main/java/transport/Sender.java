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

    private static final int N = 100;

    private int base;
    private int nextSeqNum;
    private Packet[] sendPackets;

    /**
     * {@inheritDoc}
     */
    public Sender(int entityName) {
        super(entityName);
    }

    @Override
    public void init() {
        base = 1;
        nextSeqNum = 1;
        sendPackets = new Packet[2 * N];
    }

    @Override
    public void output(Message message) {
        if (nextSeqNum < base + N) {
            int check = Checksum.compute(nextSeqNum, 0, message.getData());
            sendPackets[index(nextSeqNum)] = new Packet(nextSeqNum, 0, check, message.getData());
            udtSend(sendPackets[index(nextSeqNum)]);
            if (base == nextSeqNum) {
                startTimer(TIMER_LENGTH);
            }
            ++nextSeqNum;
        }
    }

    @Override
    public void input(Packet packet) {
        if (!Checksum.corrupt(packet)) {
            base = packet.getAcknum() + 1;
            if (base == nextSeqNum) {
                stopTimer();
            } else {
                startTimer(TIMER_LENGTH);
            }
        }
    }

    @Override
    public void timerInterrupt() {
        startTimer(TIMER_LENGTH);
        for (int i = base; i < nextSeqNum; ++i) {
            udtSend(sendPackets[index(i)]);
        }
    }

    private int index(int n) {
        return n % sendPackets.length;
    }

}
