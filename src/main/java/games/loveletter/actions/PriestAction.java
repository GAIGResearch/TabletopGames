package games.loveletter.actions;

import core.AbstractGameState;
import core.components.Deck;
import core.observations.IPrintable;
import games.loveletter.LoveLetterGameState;
import core.components.PartialObservableDeck;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

public class PriestAction extends DrawCard implements IPrintable {

    private final int opponentID;

    public PriestAction(int deckFrom, int deckTo, int fromIndex, int opponentID) {
        super(deckFrom, deckTo, fromIndex);
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        int playerID = gs.getTurnOrder().getCurrentPlayer(gs);
        Deck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);
        PartialObservableDeck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);

        if (((LoveLetterGameState) gs).isNotProtected(opponentID)){
            for (int i = 0; i < opponentDeck.getComponents().size(); i++)
                opponentDeck.setVisibilityOfComponent(i, playerID, true);
        }

        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Priest - see the cards of player "+ opponentID;
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PriestAction that = (PriestAction) o;
        return opponentID == that.opponentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), opponentID);
    }
}
