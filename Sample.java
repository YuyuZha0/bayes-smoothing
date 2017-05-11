package default;

import java.io.Serializable;

/**
 * Created by zhaoyy on 2017/4/27.
 */
public final class Sample implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int rareEventsCount;
    private final int eventsCount;


    public Sample(int rareEventsCount,
                  int eventsCount) {
        this.rareEventsCount = rareEventsCount;
        this.eventsCount = eventsCount;
    }

    public int getRareEventsCount() {
        return rareEventsCount;
    }

    public int getEventsCount() {
        return eventsCount;
    }

    public double getCtr() {
        if (rareEventsCount == 0 || eventsCount == 0)
            return 0;
        return (rareEventsCount + 0.0) / eventsCount;
    }

    @Override
    public String toString() {
        return rareEventsCount + "/" + eventsCount;
    }
}
