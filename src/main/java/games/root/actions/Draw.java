package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import evaluation.metrics.Event;
import games.root.RootGameState;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class Draw extends AbstractAction {
    protected final int playerID;
    protected final int numberOfCards;
    protected final boolean passSubGamePhase;

    public Draw(int playerID, int numberOfCards, boolean passSubGamePhase){
        this.playerID = playerID;
        this.numberOfCards = numberOfCards;
        this.passSubGamePhase = passSubGamePhase;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID) {
            Deck<RootCard> drawPile = currentState.getDrawPile();
            Deck<RootCard> playerHand = currentState.getPlayerHand(currentState.getCurrentPlayer());
            currentState.increaseActionsPlayed();
            if(passSubGamePhase){
                currentState.increaseSubGamePhase();
            }
            for (int i = 0; i < numberOfCards; i++) {
                if (drawPile.getSize() == 0){
                    for (int e = 0; e< currentState.getDiscardPile().getSize(); e++){
                        drawPile.add(currentState.getDiscardPile().draw());
                    }
                    drawPile.shuffle(0, drawPile.getSize(), currentState.getRnd());
                }
                if (drawPile.getSize() > 0) {
                    playerHand.add(drawPile.draw());
                }else {
                    currentState.logEvent(Event.GameEvent.GAME_EVENT, "Draw pile and discard piles are empty");
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public Draw copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof Draw other){
            return playerID == other.playerID && numberOfCards == other.numberOfCards && passSubGamePhase == other.passSubGamePhase;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Draw",playerID, numberOfCards, passSubGamePhase);
    }

    @Override
    public String toString() {
        return "p" + playerID + " draws " + numberOfCards;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " draws " + numberOfCards;
    }
}
