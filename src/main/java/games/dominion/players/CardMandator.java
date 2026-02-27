package games.dominion.players;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.actions.GainCard;
import games.dominion.cards.CardType;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardMandator implements IPlayerDecorator {

    Map<CardType, Integer> cardsToAcquire;

    public CardMandator(String cardType) {
        cardsToAcquire = Map.of(CardType.valueOf(cardType), 1);
    }

    @SuppressWarnings("unchecked")
    public CardMandator(JSONObject json) {
        // in this case we expect a nested JSONObject called 'requirements'
        // which is then a set of key-value pairs to go into the map
        cardsToAcquire = new HashMap<>();
        JSONObject reqs = (JSONObject) json.get("requirements");
        reqs.forEach((k,v) -> cardsToAcquire.put(
                CardType.valueOf(k.toString()),
                Integer.parseInt(v.toString())));
    }


    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        // This only triggers if we do not yet have all of the required cards, AND one of them can be
        // acquired from one of the possible actions
        // In this case we only allow the player to gain one of the requisite cards
        DominionGameState dgs = (DominionGameState) state;
        int player = state.getCurrentPlayer();
        Map<CardType, Integer> cardsNeeded = new HashMap<>();
        for (CardType ct : cardsToAcquire.keySet()) {
            int inDeck = dgs.cardsOfType(ct, player, DominionConstants.DeckType.ALL);
            if (inDeck < cardsToAcquire.get(ct)) {
                cardsNeeded.put(ct, cardsToAcquire.get(ct) - inDeck);
            }
        }
        if (cardsNeeded.isEmpty())
            return possibleActions;

        // we have cards that are needed. Are any of the actions their acquisition
        List<AbstractAction> acquisitionActions = possibleActions.stream()
                .filter(c -> {
                            if (c instanceof GainCard gc) {
                                return cardsNeeded.containsKey(gc.cardType);
                            } else {
                                return false;
                            }
                        }
                ).toList();
        if (acquisitionActions.isEmpty())
            return possibleActions;
        return acquisitionActions;
    }

    @Override
    public boolean decisionPlayerOnly() {
        return true;
    }
}
