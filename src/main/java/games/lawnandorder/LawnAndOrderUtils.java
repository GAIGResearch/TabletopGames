package games.lawnandorder;

import games.lawnandorder.components.LawnCard;

import java.util.List;

public class LawnAndOrderUtils {

    private LawnAndOrderUtils() {
    }

    /**
     * The points the cards score on the track of the category.
     */
    public static int categoryScore(List<LawnCard> cards, LawnCard.Category category, LawnAndOrderParameters params) {
        // each attribute of the category scores its group of cards independently
        int total = 0;
        for (LawnCard.Attribute attribute : LawnCard.Attribute.of(category)) {
            int n = 0;
            for (LawnCard card : cards)
                if (card.has(attribute))
                    n++;
            total += params.groupPoints(n);
        }
        return total;
    }
}
