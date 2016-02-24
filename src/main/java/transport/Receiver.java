package transport;

public class Receiver extends NetworkHost {

    private ReceiverEventHandler state;

    public Receiver(int entityName) {
        super(entityName);
    }

    @Override
    public void init() {
        state = new WaitForPacket(0);
    }

    @Override
    public void input(Packet packet) {
        state = state.input(packet);
    }

    private class WaitForPacket implements ReceiverEventHandler {

        private final int expected;

        public WaitForPacket(int expected) {
            this.expected = expected;
        }

        @Override
        public ReceiverEventHandler input(Packet packet) {
            if (Checksum.corrupt(packet) || packet.getSeqnum() != expected) {
                Packet prev = new Packet(0, packet.getSeqnum(), Checksum.compute(0, packet.getSeqnum()));
                udtSend(prev);
                return this;
            }

            deliverData(packet.getPayload());

            Packet ackPacket = new Packet(0, expected, Checksum.compute(0, expected));
            udtSend(ackPacket);

            return new WaitForPacket((expected + 1) % 2);
        }

    }

    private interface ReceiverEventHandler {

        default ReceiverEventHandler input(Packet packet) {
            return this;
        }

    }

}
