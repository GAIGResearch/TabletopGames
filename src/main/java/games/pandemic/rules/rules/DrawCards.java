package games.pandemic.rules.rules;

import core.AbstractGameStateWithTurnOrder;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.properties.PropertyString;
import core.rules.Node;
import core.rules.nodetypes.RuleNode;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import static games.pandemic.PandemicConstants.playerDeckHash;
import static core.CoreConstants.nameHash;
import static core.CoreConstants.playerHandHash;

public class DrawCards extends RuleNode {

    public DrawCards() {
        super();
    }

    /**
     * Copy constructor
     * @param drawCards - Node to be copied
     */
    public DrawCards(DrawCards drawCards) {
        super(drawCards);
    }

    @Override
    public boolean run(AbstractGameStateWithTurnOrder gs) {
        PandemicGameState pgs = (PandemicGameState)gs;

        boolean epidemic = false;

        Deck<Card> tempDeck = pgs.getTempDeck();

        Deck<Card> playerDeck = (Deck<Card>) pgs.getComponent(playerDeckHash);
        DrawCard action = new DrawCard(playerDeck.getComponentID(), tempDeck.getComponentID(), 0);
        boolean drawn = action.execute(gs);

        if (drawn) {
            Deck<Card> playerHand = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);

            Card c = tempDeck.draw();  // Check the drawn card
            // If epidemic card, do epidemic, only one per draw
            if (((PropertyString) c.getProperty(nameHash)).value.hashCode() == PandemicConstants.epidemicCard) {
                epidemic = true;
            } else {  // Otherwise, give card to player
                if (playerHand != null) {
                    playerHand.add(c);
                }
            }
            ((PandemicGameState) gs).setEpidemic(epidemic);
            ((PandemicGameState) gs).cardWasDrawn();
            return true;
        }
        return false;
    }

    @Override
    protected Node _copy() {
        return new DrawCards(this);
    }
}
