package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.actions.AddCardToDeck;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.content.PropertyString;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import static games.pandemic.PandemicConstants.playerDeckHash;
import static utilities.CoreConstants.nameHash;
import static utilities.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class DrawCards extends RuleNode {

    @Override
    public boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;

        boolean epidemic = false;

        Deck<Card> tempDeck = pgs.getTempDeck();

        Deck<Card> playerDeck = (Deck<Card>) pgs.getComponent(playerDeckHash);
        DrawCard action = new DrawCard(playerDeck, tempDeck);
        boolean drawn = action.execute(gs);

        if (drawn) {
            Deck<Card> playerHand = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);

            Card c = tempDeck.pick();  // Check the drawn card
            // If epidemic card, do epidemic, only one per draw
            if (((PropertyString) c.getProperty(nameHash)).value.hashCode() == PandemicConstants.epidemicCard) {
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
