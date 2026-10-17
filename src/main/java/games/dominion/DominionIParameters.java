package games.dominion;

import games.dominion.cards.CardType;

import java.util.List;

public class DominionIParameters extends DominionParameters {
    public DominionIParameters() {
        super(List.of(
                CardType.ARTISAN,
                CardType.CELLAR,
                CardType.MARKET,
                CardType.MERCHANT,
                CardType.MINE,
                CardType.MOAT,
                CardType.MONEYLENDER,
                CardType.POACHER,
                CardType.REMODEL,
                CardType.WITCH,
                CardType.CURSE
        ));
    }
}
