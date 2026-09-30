package games.dominion;

import core.AbstractParameters;
import core.Game;
import games.GameType;
import games.dominion.cards.CardType;

import java.util.List;

public class DominionFGParameters extends DominionParameters {
    public DominionFGParameters() {
        super(List.of(
                CardType.CELLAR,
                CardType.MARKET,
                CardType.MERCHANT,
                CardType.MILITIA,
                CardType.MINE,
                CardType.MOAT,
                CardType.REMODEL,
                CardType.SMITHY,
                CardType.VILLAGE,
                CardType.WORKSHOP
        ));
    }
}
