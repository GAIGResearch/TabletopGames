package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class PlayBrutalTactics extends AbstractAction {
    public final int playerID;
    public RootCard card;

    public PlayBrutalTactics(int playerID, RootCard card){
        this.playerID = playerID;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && card.cardtype == RootCard.CardType.BrutalTactics){

        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new PlayBrutalTactics(playerID, card);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof PlayBrutalTactics p){
            return playerID == p.playerID && card.equals(p.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("BrutalTactics", playerID, card.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " plays " + card.cardtype.toString();
    }
}
