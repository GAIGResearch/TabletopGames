package games.skitgubbe;

import core.components.FrenchCard;

public class SkitgubbeUtils {

    private SkitgubbeUtils() {
    }

    /**
     * @return whether card beats top in phase two, with the given trump suit
     */
    public static boolean beats(FrenchCard card, FrenchCard top, FrenchCard.Suite trump) {
        if (card.suite == top.suite)
            return card.number > top.number;
        return card.suite == trump;
    }
}
