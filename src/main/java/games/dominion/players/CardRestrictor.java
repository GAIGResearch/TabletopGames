package games.dominion.players;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;
import games.dominion.actions.BuyCard;
import games.dominion.cards.CardType;

import java.util.List;

public class CardRestrictor implements IPlayerDecorator {

    List<CardType> cardsToIgnore;

    public CardRestrictor(String cardType) {
        this(List.of(cardType));
    }

    public CardRestrictor(List<String> types) {
        cardsToIgnore = types.stream().map(CardType::valueOf).toList();
    }

    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        return possibleActions.stream().filter(action -> {
            if (action instanceof BuyCard) {
                return !cardsToIgnore.contains(((BuyCard) action).cardType);
            } else {
                return true;
            }
        }).toList();
    }

    @Override
    public boolean decisionPlayerOnly() {
        return true;
    }
}
