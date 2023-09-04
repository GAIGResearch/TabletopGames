package games.hearts.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import games.hearts.HeartsGameState;

import java.util.Objects;

public class Play extends AbstractAction implements IPrintable {
    public final int playerID;
    public final FrenchCard card;


    public Play(int playerID, FrenchCard card) {
        this.playerID = playerID;
        this.card = card;

    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        HeartsGameState hgs = (HeartsGameState) gameState;
        if (playerID >= 0 && playerID < hgs.getPlayerDecks().size()) {
            Deck<FrenchCard> playerHand = hgs.getPlayerDecks().get(playerID);
            if (playerHand.getComponents().remove(card)) {
                hgs.getChosenCards().put(playerID, card);
                return true;
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Play(playerID, card.copy());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Play)) return false;
        Play play = (Play) o;
        return playerID == play.playerID && card.equals(play.card);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, card);
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Play: " + card.toString();
    }
}
