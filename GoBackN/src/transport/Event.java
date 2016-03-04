package transport;

/**
 * This class represents a "network event" in the network simulator
 */
public class Event {

    private double time;            // the time the event is scheduled for
    private EventType type;         // the type of event (see NetworkSimulator.java)
    private int entity;             // the entity at which the event will be executed (sender or received)
    private Packet packet;          // the packet involved in this simulated event (can be null)

    public Event(double t, EventType ty, int ent) {
        time = t;
        type = ty;
        entity = ent;
        packet = null;
    }

    public Event(double t, EventType ty, int ent, Packet p) {
        time = t;
        type = ty;
        entity = ent;
        packet = p;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public int getEntity() {
        return entity;
    }

    public void setEntity(int entity) {
        this.entity = entity;
    }

    public Packet getPacket() {
        return packet;
    }

    public void setPacket(Packet packet) {
        this.packet = packet;
    }

    @Override
    public String toString() {
        return ("time: " + time + "  type: " + type + "  entity: " + entity + "packet: " + packet);
    }

}
