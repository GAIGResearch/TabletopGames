package pandemic.engine.rules;

import actions.Action;
import actions.DrawCard;
import components.Deck;
import core.GameState;
import pandemic.Constants;

import java.util.ArrayList;

public class ForceDiscardReaction extends RuleNode {

    @Override
    protected boolean run(GameState gs) {
        // player needs to discard N cards
        int activePlayer = gs.getActingPlayer().a;
        Deck playerDeck = (Deck) gs.getAreas().get(activePlayer).getComponent(Constants.playerHandHash);
        Deck playerDiscardDeck = (Deck) gs.findDeck("Player Deck Discard");  // TODO: not general

        int nDiscards = playerDeck.getCards().size() - playerDeck.getCapacity();
        ArrayList<Action> acts = new ArrayList<>();  // Only discard card actions available
        for (int i = 0; i < playerDeck.getCards().size(); i++) {
            acts.add(new DrawCard(playerDeck, playerDiscardDeck, i));  // adding card i from player deck to player discard deck
        }
        for (int i = 0; i < nDiscards; i++) {
            gs.addReactivePlayer(activePlayer, acts);
        }
        return false;
    }
}
