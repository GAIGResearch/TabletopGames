package games.dominion;

import games.dominion.cards.CardType;

public class DominionIParameters extends DominionParameters {
    public DominionIParameters() {
        cardsUsed.add(CardType.ARTISAN);
        cardsUsed.add(CardType.CELLAR);
        cardsUsed.add(CardType.MARKET);
        cardsUsed.add(CardType.MERCHANT);
        cardsUsed.add(CardType.MINE);
        cardsUsed.add(CardType.MOAT);
        cardsUsed.add(CardType.MONEYLENDER);
        cardsUsed.add(CardType.POACHER);
        cardsUsed.add(CardType.REMODEL);
        cardsUsed.add(CardType.WITCH);
        cardsUsed.add(CardType.CURSE);
    }
}
