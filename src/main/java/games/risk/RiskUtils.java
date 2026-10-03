package games.risk;

import games.risk.components.RiskCard;

import java.util.*;

public final class RiskUtils {

    private RiskUtils() {
    }

    /**
     * The armies each side loses in one roll of a battle.
     *
     * @param attackDice the attacker's dice, in any order
     * @param defendDice the defender's dice, in any order
     * @return {armies the attacker loses, armies the defender loses}
     */
    public static int[] battleLosses(int[] attackDice, int[] defendDice) {
        int[] attack = descending(attackDice);
        int[] defend = descending(defendDice);
        int[] losses = new int[2];
        // the highest dice are compared, then the second highest, as many pairs as the side with fewer dice rolled;
        // the lower die of a pair loses one army, and the defender wins a tie
        for (int i = 0; i < Math.min(attack.length, defend.length); i++) {
            if (attack[i] > defend[i])
                losses[1]++;
            else
                losses[0]++;
        }
        return losses;
    }

    /**
     * The canonical order of RISK cards: territory cards by territory index, then wild cards.
     */
    public static final Comparator<RiskCard> CARD_ORDER = Comparator
            .comparingInt((RiskCard c) -> c.isWild() ? Integer.MAX_VALUE : c.territory.index());

    /**
     * Whether the 3 cards make a set: 3 of the same symbol, one of each of Infantry, Cavalry and Artillery, or any
     * 2 with a wild card.
     */
    public static boolean isSet(List<RiskCard> cards) {
        if (cards.size() != 3)
            return false;
        Set<RiskCard.Symbol> symbols = EnumSet.noneOf(RiskCard.Symbol.class);
        for (RiskCard c : cards)
            symbols.add(c.symbol);
        if (symbols.contains(RiskCard.Symbol.WILD))
            return true;
        // all the same, or all different
        return symbols.size() == 1 || symbols.size() == 3;
    }

    /**
     * Every distinct set of 3 cards that can be made from the hand, each in CARD_ORDER. Sets that differ only in
     * which wild card they use are the same set.
     */
    public static List<List<RiskCard>> sets(List<RiskCard> hand) {
        List<RiskCard> cards = new ArrayList<>(hand);
        cards.sort(CARD_ORDER);
        // wild cards are equal values, so a set made with either is the same list
        Set<List<RiskCard>> sets = new LinkedHashSet<>();
        for (int i = 0; i < cards.size(); i++)
            for (int j = i + 1; j < cards.size(); j++)
                for (int k = j + 1; k < cards.size(); k++) {
                    List<RiskCard> set = List.of(cards.get(i), cards.get(j), cards.get(k));
                    if (isSet(set))
                        sets.add(set);
                }
        return new ArrayList<>(sets);
    }

    private static int[] descending(int[] dice) {
        int[] sorted = dice.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < sorted.length / 2; i++) {
            int tmp = sorted[i];
            sorted[i] = sorted[sorted.length - 1 - i];
            sorted[sorted.length - 1 - i] = tmp;
        }
        return sorted;
    }
}
