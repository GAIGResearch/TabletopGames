package games.dominion.cards;

import games.dominion.DominionGameState;

public class Gardens extends DominionCard {

    public final int CARDS_PER_VICTORY_POINT = 10;

    protected Gardens() {
        super(CardType.GARDENS);
    }

    @Override
    public int victoryPoints(int player, DominionGameState context) {
        return context.getTotalCards(player) / CARDS_PER_VICTORY_POINT;
    }
}
