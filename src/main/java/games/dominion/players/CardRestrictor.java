package games.dominion.players;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;
import games.dominion.actions.BuyCard;
import games.dominion.actions.GainCard;
import games.dominion.cards.CardType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

public class CardRestrictor implements IPlayerDecorator {

    List<CardType> cardsToIgnore;

    public CardRestrictor(String cardType) {
        this(List.of(cardType));
    }

    public CardRestrictor(List<String> types) {
        cardsToIgnore = types.stream().map(CardType::valueOf).toList();
    }

    @SuppressWarnings("unchecked")
    public CardRestrictor(JSONObject json) {
        // in this case we expect an array of the Enums to be excluded
        this(((JSONArray) json.get("cardsToIgnore")).stream().map(Object::toString).toList());
    }

    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        List<AbstractAction> filteredActions = possibleActions.stream().filter(action -> {
            if (action instanceof GainCard gc) {
                return !cardsToIgnore.contains(gc.cardType);
            } else {
                return true;
            }
        }).toList();
        if (filteredActions.isEmpty()) return possibleActions;
        return filteredActions;
    }

    @Override
    public boolean decisionPlayerOnly() {
        return true;
    }
}
