package games.scopa;

import core.components.TarotCard;

import java.util.ArrayList;
import java.util.List;

public class ScopaUtils {

    private ScopaUtils() {
    }

    /**
     * Each set of table cards the card can capture; empty if it captures nothing.
     */
    public static List<List<TarotCard>> captures(TarotCard card, List<TarotCard> table) {
        // a table card of the same rank is taken on its own; only when there is none can the card take a set of two
        // or more cards whose capture values add up to its own
        List<List<TarotCard>> captures = new ArrayList<>();
        for (TarotCard c : table)
            if (c.number == card.number)
                captures.add(List.of(c));
        if (captures.isEmpty())
            addSums(table, 0, ScopaParameters.captureValue(card), new ArrayList<>(), captures);
        return captures;
    }

    public static int primiera(List<TarotCard> pile) {
        // the best card of each suit counts; a pile without every suit has no primiera
        int total = 0;
        for (TarotCard.Suit suit : ScopaParameters.SUITS) {
            int best = 0;
            for (TarotCard card : pile)
                if (card.suit == suit)
                    best = Math.max(best, ScopaParameters.primieraValue(card));
            if (best == 0)
                return 0;
            total += best;
        }
        return total;
    }

    public static int coins(List<TarotCard> pile) {
        return (int) pile.stream().filter(c -> c.suit == TarotCard.Suit.Coins).count();
    }

    /**
     * Adds to `captures` every set of two or more cards from table[from..] that, with `chosen`, sums to `remaining`.
     */
    private static void addSums(List<TarotCard> table, int from, int remaining, List<TarotCard> chosen,
                                List<List<TarotCard>> captures) {
        if (remaining == 0) {
            if (chosen.size() >= 2)
                captures.add(List.copyOf(chosen));
            return;
        }
        for (int i = from; i < table.size(); i++) {
            int value = ScopaParameters.captureValue(table.get(i));
            if (value > remaining)
                continue;
            chosen.add(table.get(i));
            addSums(table, i + 1, remaining - value, chosen, captures);
            chosen.remove(chosen.size() - 1);
        }
    }
}
