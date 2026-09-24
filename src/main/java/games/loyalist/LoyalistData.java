package games.loyalist;

import java.util.ArrayList;
import java.util.List;

/** Immutable rulebook card catalogue. Resource indexes are population, wealth, military. */
public final class LoyalistData {
    private LoyalistData() {}

    public record Event(
            int id,
            String name,
            int resources,
            int threshold,
            int destination,
            int multiplier,
            int additional,
            boolean destroyed,
            boolean halfDestroyed) {
        public boolean allows(int resource) {
            return (resources & (1 << resource)) != 0;
        }
    }

    public static final Event[] EVENTS = {
        null,
        new Event(1, "Spring drought", 1, 3, 0, 1, 0, false, false),
        new Event(2, "Market dispute", 2, 3, 1, 1, 0, false, false),
        new Event(3, "Border bandits", 4, 3, 2, 1, 0, false, false),
        new Event(4, "Epidemic", 3, 4, -1, 1, 0, false, false),
        new Event(5, "Scholars' petition", 1, 3, 0, 2, 0, false, false),
        new Event(6, "Northern invasion", 6, 7, 2, 1, 0, false, false),
        new Event(7, "Great crop failure", 3, 6, -1, 1, 0, true, false),
        new Event(8, "Factional conflict", 5, 6, -1, 1, 2, false, false),
        new Event(9, "Coastal raiders", 4, 7, 2, 1, 0, false, false),
        new Event(10, "Treasury depletion", 2, 8, 1, 3, 0, false, false),
        new Event(11, "Tribute demand", 7, 10, -1, 1, 0, true, false),
        new Event(12, "Conspiracy uncovered", 5, 9, -1, 1, 3, false, false),
        new Event(13, "Full-scale war", 6, 11, 2, 1, 0, false, false),
        new Event(14, "Popular uprising", 3, 10, 0, 2, 0, false, false),
        new Event(15, "Usurpation plot", 7, 12, -1, 1, 0, false, false),
        new Event(16, "Southern famine", 7, 11, -1, 1, 2, false, false),
        new Event(17, "Foreign invasion", 4, 12, 2, 1, 0, false, true)
    };
    public static final String[] REFORMS = {
        "None",
        "Land reform",
        "Three-army command",
        "Daedong tax",
        "Border council",
        "Impartiality",
        "Centralisation",
        "Royal inspector",
        "Informants",
        "Treasury audit",
        "Forced levy",
        "Sogo militia"
    };

    public static List<Integer> powerCards() {
        List<Integer> cards = new ArrayList<>();
        for (int sign : new int[] {1, -1}) {
            for (int i = 0; i < 10; i++) cards.add(sign);
            for (int i = 0; i < 6; i++) cards.add(2 * sign);
            for (int i = 0; i < 3; i++) cards.add(3 * sign);
        }
        cards.add(0);
        cards.add(0);
        return cards;
    }
}
