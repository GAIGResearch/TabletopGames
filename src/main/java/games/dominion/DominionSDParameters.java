package games.dominion;

import games.dominion.cards.CardType;

import java.util.List;

public class DominionSDParameters extends DominionParameters {
    public DominionSDParameters() {
        super(List.of(
                CardType.ARTISAN,
                CardType.BANDIT,
                CardType.BUREAUCRAT,
                CardType.CHAPEL,
                CardType.FESTIVAL,
                CardType.GARDENS,
                CardType.SENTRY,
                CardType.THRONE_ROOM,
                CardType.WITCH,
                CardType.CURSE,
                CardType.WORKSHOP
        ));
    }
}
