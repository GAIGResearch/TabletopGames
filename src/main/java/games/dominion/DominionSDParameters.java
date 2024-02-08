package games.dominion;

import games.dominion.cards.CardType;

public class DominionSDParameters extends DominionParameters {
    public DominionSDParameters() {
        cardsUsed.add(CardType.ARTISAN);
        cardsUsed.add(CardType.BANDIT);
        cardsUsed.add(CardType.BUREAUCRAT);
        cardsUsed.add(CardType.CHAPEL);
        cardsUsed.add(CardType.FESTIVAL);
        cardsUsed.add(CardType.GARDENS);
        cardsUsed.add(CardType.SENTRY);
        cardsUsed.add(CardType.THRONE_ROOM);
        cardsUsed.add(CardType.WITCH);
        cardsUsed.add(CardType.CURSE);
        cardsUsed.add(CardType.WORKSHOP);
    }
}
