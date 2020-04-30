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
import static pandemic.Constants.nameHash;
import static pandemic.Constants.playerDeckHash;

public class DrawCards extends RuleNode {

    @Override
    public boolean run(GameState gs) {
        boolean epidemic = false;
        int activePlayer = gs.getActingPlayer().a;

        String tempDeckID = gs.tempDeck();
        IDeck tempDeck = gs.findDeck(tempDeckID);

        Deck playerDeck = (Deck) gs.getAreas().get(-1).getComponent(playerDeckHash);
        DrawCard action = new DrawCard(playerDeck, tempDeck);
        boolean drawn = action.execute(gs);

        if (drawn) {
            Deck playerHand = (Deck) gs.getAreas().get(activePlayer).getComponent(Constants.playerHandHash);

            Card c = tempDeck.pick();  // Check the drawn card
            // If epidemic card, do epidemic, only one per draw
            if (((PropertyString) c.getProperty(nameHash)).value.hashCode() == Constants.epidemicCard) {
                epidemic = true;
            } else {  // Otherwise, give card to player
                if (playerHand != null) {
                    new AddCardToDeck(c, playerHand).execute(gs);
                }
            }
            ((PandemicGameState) gs).setEpidemic(epidemic);
            ((PandemicGameState) gs).cardWasDrawn();
            return true;
        }
        return false;
    }
}
