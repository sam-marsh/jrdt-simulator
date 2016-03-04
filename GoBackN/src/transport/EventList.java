package transport;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of simulated events scheduled for execution
 */
public class EventList {

    // the list of events
    private final List<Event> list;

    public EventList() {
        list = new ArrayList<>();
    }

    public boolean add(Event e) {
        return list.add(e);
    }

    /**
     * remove from the list of events the event that is scheduled for execution the soonest
     *
     * @return an Event or null if the list is empty
     */
    public Event removeNext() {
        if (list.isEmpty()) {
            return null;
        }

        int index = 0;

        double soonest = ((Event) list.get(index)).getTime();

        for (int i = 0; i < list.size(); i++) {
            if ((list.get(i)).getTime() < soonest) {
                soonest = (list.get(i)).getTime();
                index = i;
            }
        }

        Event next = (Event) list.get(index);
        list.remove(next);

        return next;
    }

    /**
     * Removes and returns the timerEvent event for the specified network entity (sender or receiver). 
     * Each NetworkHost (A or B) can have up to one timerEvent pending (1 TIMERINTERRUPT Event in the list).
     *
     * @param entity
     * @return
     */
    public Event removeTimer(int entity) {
        int timerIndex = -1;
        Event timerEvent = null;

        for (int i = 0; i < list.size(); i++) {
            if (((list.get(i)).getType() == EventType.TIMERINTERRUPT) && ((list.get(i)).getEntity() == entity)) {
                timerIndex = i;
                break;
            }
        }

        if (timerIndex != -1) {
            timerEvent = list.get(timerIndex);
            list.remove(timerEvent);
        }

        return timerEvent;

    }

    /**
     * Returns the scheduled time for the next packet arrival at the provided network entity.
     * No reordering takes place, so I can safely remove the first FROMNETWORK Event in the list.
     *
     * @param entityTo
     * @return
     */
    public double getLastPacketTime(int entityTo) {
        double time = 0.0;
        
        for (Event ev : list) {
            if ((ev).getType() == EventType.FROMNETWORK) {
                if ((ev).getEntity() == entityTo) {
                    time = (ev).getTime();
                }
            }
        }

        return time;
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
