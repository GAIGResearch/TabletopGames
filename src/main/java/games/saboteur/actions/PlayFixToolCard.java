package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.SaboteurCard;


public class PlayFixToolCard extends AbstractAction {

    private final int currentPlayerID;
    private final ActionCard fixToolCard;
    private final ActionCard.ToolCardType toolType;
    public PlayFixToolCard(int currentPlayerID, ActionCard fixToolCard, ActionCard.ToolCardType ToolType) {
        this.currentPlayerID = currentPlayerID;
        this.fixToolCard = fixToolCard;
        this.toolType = ToolType;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        Deck<SaboteurCard> otherBrokenToolDeck = sgs.brokenToolDecks.get(sgs.getCurrentPlayer());
        Deck<SaboteurCard> currentPlayerDeck = sgs.playerDecks.get(currentPlayerID);
        currentPlayerDeck.remove(fixToolCard);

        for(SaboteurCard card : otherBrokenToolDeck.getComponents()){
            ActionCard currentCard= (ActionCard) card;
            assert currentCard.toolTypes != null; // if we somehow check a card that is not a broken tool card

            if(currentCard.toolTypes[0] == toolType){
                otherBrokenToolDeck.remove(card);
                break;
            }
        }
        System.out.println(this);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    public String toString()
    {
        return "Fixed Tool " + toolType + " to " + currentPlayerID;
    }

    @Override
    public String getString(AbstractGameState gameState)
    {
        return "Fix " + toolType + " for player " + currentPlayerID;
    }
}
