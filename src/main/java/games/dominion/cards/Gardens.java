package games.dominion.cards;

import games.dominion.DominionGameState;

public class Gardens extends DominionCard {
    protected Gardens() {
        super(CardType.GARDENS);
    }

    @Override
    public int victoryPoints(int player, DominionGameState context) {
        return context.getTotalCards(player) / 10;
    }
}
