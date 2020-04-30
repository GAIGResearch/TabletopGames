package pandemic.engine.rules;

import actions.AddCardToDeck;
import actions.DrawCard;
import components.Card;
import components.Deck;
import components.IDeck;
import content.PropertyString;
import core.GameState;
import pandemic.Constants;
import pandemic.PandemicGameState;
import pandemic.engine.Node;
import static pandemic.Constants.nameHash;

public class DrawCards extends RuleNode {

    public DrawCards(Node next) {
        super(next);
    }

    @Override
    public boolean run(GameState gs) {
        boolean epidemic = false;
        int activePlayer = gs.getActingPlayer().a;

        String tempDeckID = gs.tempDeck();
        DrawCard action = new DrawCard("Player Deck", tempDeckID);
        boolean drawn = action.execute(gs);

        if (drawn) {
            IDeck tempDeck = gs.findDeck(tempDeckID);
            Deck playerDeck = (Deck) gs.getAreas().get(activePlayer).getComponent(Constants.playerHandHash);

            Card c = tempDeck.pick();  // Check the drawn card
            // If epidemic card, do epidemic, only one per draw
            if (((PropertyString) c.getProperty(nameHash)).value.hashCode() == Constants.epidemicCard) {
                epidemic = true;
            } else {  // Otherwise, give card to player
                if (playerDeck != null) {
                    // deck size doesn't go beyond 7
                    new AddCardToDeck(c, playerDeck).execute(gs);
                }
            }
            ((PandemicGameState) gs).setEpidemic(epidemic);
            ((PandemicGameState) gs).cardWasDrawn();
            return true;
        }
        return false;
    }
}
