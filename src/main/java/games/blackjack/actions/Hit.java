package games.blackjack.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.blackjack.BlackjackForwardModel;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;

import java.util.Arrays;
import java.util.Objects;

public class Hit extends AbstractAction implements IPrintable {
    public final int playerID;
    public final boolean advanceTurnOrder;
    public final boolean hidden;

    public Hit(int playerID){
        this.playerID = playerID;
        this.advanceTurnOrder = false;
        this.hidden = false;
    }

    public Hit(int playerID, boolean advanceTurnOrder, boolean hidden){
        this.playerID = playerID;
        this.advanceTurnOrder = advanceTurnOrder;
        this.hidden = hidden;
    }

    @Override
    public boolean execute(AbstractGameState gameState){
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        PartialObservableDeck<FrenchCard> playerHand = bjgs.getPlayerDecks().get(playerID);
        if (playerID != bjgs.getDealerPlayer()) {
            playerHand.add(bjgs.getDrawDeck().draw());
        } else {
            // Dealer
            boolean[] visibility = new boolean[gameState.getNPlayers()];
            Arrays.fill(visibility, !hidden);
            playerHand.add(bjgs.getDrawDeck().draw(), visibility);
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable state
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hit)) return false;
        Hit hit = (Hit) o;
        return hidden == hit.hidden && playerID == hit.playerID && advanceTurnOrder == hit.advanceTurnOrder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, advanceTurnOrder, hidden);
    }

    @Override
    public void printToConsole(){
        System.out.println("Hit");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Hit";
    }

    @Override
    public String toString() {
        return "Hit";
    }
}
