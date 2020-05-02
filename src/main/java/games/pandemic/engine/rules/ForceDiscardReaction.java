package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;

import java.util.ArrayList;

import static games.pandemic.PandemicConstants.playerDeckDiscardHash;

@SuppressWarnings("unchecked")
public class ForceDiscardReaction extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        // player needs to discard N cards
        int activePlayer = pgs.getActingPlayerID();
        Deck<Card> playerDeck = (Deck<Card>) pgs.getComponent(PandemicConstants.playerHandHash, activePlayer);
        Deck<Card> playerDiscardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);  // TODO: not general

        int nDiscards = playerDeck.getCards().size() - playerDeck.getCapacity();
        ArrayList<IAction> acts = new ArrayList<>();  // Only discard card core.actions available
        for (int i = 0; i < playerDeck.getCards().size(); i++) {
            acts.add(new DrawCard(playerDeck, playerDiscardDeck, i));  // adding card i from player deck to player discard deck
        }
        for (int i = 0; i < nDiscards; i++) {
            pgs.addReactivePlayer(activePlayer, acts);
        }
        return false;
    }
}
