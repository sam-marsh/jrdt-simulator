package transport;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Queue;

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
    private static final int TIMER_LENGTH = 100;

    /**
     * The number of consecutive unacknowledged packets allowed.
     */
    private static final int WINDOW_SIZE = 2;

    /**
     * The index of the start of the window.
     */
    private int base;

    /**
     * The sequence number of the next packet to be sent through to the receiver.
     */
    private int nextSeqNum;

    /**
     * A buffer for temporarily storing messages received from the application
     * layer while the window is full so no messages can currently be sent.
     */
    private Queue<Message> buffer;

    /**
     * The packets that are currently unacknowledged as sent, awaiting a response
     * from the receiver.
     */
    private Packet[] inFlight;

    /**
     * {@inheritDoc}
     */
    public Sender(int entityName) {
        super(entityName);
    }

    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }

    @Override
    public void init() {

        try {
            setFinalStatic(System.class.getDeclaredField("out"), new PrintStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException {

                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }

        base = nextSeqNum = 0;
        buffer = new ArrayDeque<>();
        inFlight = new Packet[WINDOW_SIZE];
    }

    @Override
    public void output(Message message) {
        if (nextSeqNum < base + WINDOW_SIZE) {
            sendMessage(message);
        } else {
            buffer.offer(message);
            System.out.flush();
            System.err.println("SENDER: Buffering packet " + message.getData());
            System.err.flush();
        }
    }

    @Override
    public void input(Packet packet) {
        if (Checksum.corrupt(packet))
            return;

        System.out.flush();
        System.err.println("SENDER: Received ACK " + packet.getAcknum());
        System.err.flush();

        base = packet.getAcknum() + 1;

        if (nextSeqNum == base) {
            stopTimer();
        }

        while (!buffer.isEmpty() && nextSeqNum < base + WINDOW_SIZE) {
            sendMessage(buffer.poll());
        }

    }

    @Override
    public void timerInterrupt() {
        startTimer(TIMER_LENGTH);
        System.out.flush();
        System.err.println("SENDER: TIMEOUT: Resending " + base + " to " + (nextSeqNum - 1));
        for (int i = base; i < nextSeqNum; ++i) {
            udtSend(inFlight[index(i)]);
        }
    }

    private void sendMessage(Message message) {
        inFlight[index(nextSeqNum)] = makePacket(nextSeqNum, message.getData());
        System.out.flush();
        System.err.println("SENDER: Sending packet " + nextSeqNum);
        System.err.flush();
        udtSend(inFlight[index(nextSeqNum)]);
        if (nextSeqNum == base) {
            startTimer(TIMER_LENGTH);
        }
        ++nextSeqNum;
    }

    private int index(int i) {
        return i % inFlight.length;
    }

    private Packet makePacket(int seq, String payload) {
        int checksum = Checksum.compute(seq, 0, payload);
        return new Packet(seq, 0, checksum, payload);
    }

}
