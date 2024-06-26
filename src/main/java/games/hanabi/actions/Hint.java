package games.hanabi.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Counter;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.hanabi.CardType;
import games.hanabi.HanabiCard;
import games.hanabi.HanabiGameState;

import java.util.List;
import java.util.Objects;

public class Hint extends AbstractAction implements IPrintable {
    protected int playerHand;
    protected int number = 0;
    protected CardType color = null;



    public Hint (int playerHand, int number) {
        this.playerHand = playerHand;
        this.number = number;
    }

    public Hint (int playerHand, CardType color) {
        this.playerHand = playerHand;
        this.color = color;
    }
    @Override
    public boolean execute(AbstractGameState gameState) {
        HanabiGameState hbgs = (HanabiGameState) gameState;
        Counter HintCounter = hbgs.getHintCounter();
        for (HanabiCard cd : hbgs.getPlayerDecks().get(playerHand).getComponents()) {
            if (number != 0) {
                if (cd.number == this.number) {
                    cd.ownerKnowsNumber = true;
                }
                else{
                    cd.possibleNumber.remove(Integer.valueOf(this.number));
                }
            }
            else if (color != null) {
                if (cd.color.equals(this.color)) {
                    cd.ownerKnowsColor = true;
                }
                else{
                    cd.possibleColour.remove(this.color);
                }
            }

        }
        HintCounter.decrement(1);
        return true;
    }




    @Override
    public AbstractAction copy() {
        if (number !=0) {
            return new Hint(playerHand, number);
        }
        else{
            return new Hint(playerHand, color);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hint)) return false;
        Hint hint = (Hint) o;
        return playerHand == hint.playerHand && number == hint.number && Objects.equals(color, hint.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerHand, number, color);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println("Gave a Hint");
    }

    @Override
    public String toString() {
        return "Gave a Hint (" + (number != 0? "number: " + number : "color: " + color) + ")";
    }
}