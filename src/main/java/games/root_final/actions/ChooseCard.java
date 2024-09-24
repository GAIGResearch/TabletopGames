package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class ChooseCard extends AbstractAction {
    public final int playerID;
    public RootCard card;

    public ChooseCard(int playerID, RootCard card){
        this.playerID = playerID;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        if (gs.getCurrentPlayer() == playerID){
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new ChooseCard(playerID,card);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof ChooseCard c){
            return playerID == c.playerID && card.equals(c.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("ChooseCard", playerID, card.cardtype, card.suit);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " chooses " + card.suit.toString() + " card " + card.cardtype.toString();
    }
}
