package games.catan.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanParameters.Resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Player may steal a resource from a player when moving a robber or playing a knight card
 */
public class StealResource extends AbstractAction {
    public final int targetPlayerID;

    public StealResource(int targetPlayerID){
        this.targetPlayerID = targetPlayerID;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        Deck<Card> currentPlayerResources = (Deck<Card>)cgs.getComponentActingPlayer(CoreConstants.playerHandHash);
        Deck<Card> targetPlayerResources = (Deck<Card>)cgs.getComponent(CoreConstants.playerHandHash, targetPlayerID);
        Random random = new Random(gs.getGameParameters().getRandomSeed());
        if (targetPlayerResources.getSize() == 0){
            return false;
        }
        int cardIndex = random.nextInt(targetPlayerResources.getSize());

        Card card = targetPlayerResources.pick(cardIndex);
        currentPlayerResources.add(card);

        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof StealResource){
            StealResource otherAction = (StealResource)other;
            return targetPlayerID == otherAction.targetPlayerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetPlayerID);
    }

    @Override
    public String toString() {
        return "Stealing a resource card from player " + targetPlayerID;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
