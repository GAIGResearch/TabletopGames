package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.SaboteurCard;

import javax.tools.Tool;

public class PlayBrokenToolCard extends AbstractAction {

    private final int playerID;
    private final ActionCard brokenToolCard;

    private final ActionCard.ToolCardType toolType;
    public PlayBrokenToolCard(ActionCard brokenToolCard, int playerID, ActionCard.ToolCardType toolType)
    {
        this.brokenToolCard = brokenToolCard;
        this.playerID = playerID;
        this.toolType = toolType;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        Deck<SaboteurCard> currentPlayerDeck = sgs.playerDecks.get(sgs.getCurrentPlayer());
        currentPlayerDeck.remove(brokenToolCard);
        sgs.brokenToolDecks.get(playerID).add(brokenToolCard);
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

    public String toString() {
        return "Broken " + toolType + " to " + playerID;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return "Broken " + toolType + " to player " + playerID;
    }
}
