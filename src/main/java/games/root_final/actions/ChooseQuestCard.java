package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.cards.RootQuestCard;

import java.util.Objects;

public class ChooseQuestCard extends AbstractAction{
    public final int playerID;
    public RootQuestCard card;

    public ChooseQuestCard(int playerID, RootQuestCard card){
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
        return new ChooseQuestCard(playerID,card);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof ChooseQuestCard c){
            return playerID == c.playerID && card.equals(c.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("ChooseQuestCard", playerID, card.cardType, card.suit);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " chooses quest " + card.suit.toString() + " card " + card.cardType.toString();
    }
}
