package games.hanabi.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Counter;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.hanabi.HanabiCard;
import games.hanabi.HanabiGameState;

import java.util.List;

public class Hint extends DrawCard implements IPrintable {
    protected PartialObservableDeck<HanabiCard> playerHand;
    protected int number = 0;
    protected String color = "";



    public Hint (PartialObservableDeck<HanabiCard> playerHand, int number) {
        this.playerHand = playerHand;
        this.number = number;
    }

    public Hint (PartialObservableDeck<HanabiCard> playerHand, String color) {
        this.playerHand = playerHand;
        this.color = color;
    }
    @Override
    public boolean execute(AbstractGameState gameState) {
        HanabiGameState hbgs = (HanabiGameState) gameState;
        Counter HintCounter = hbgs.getHintCounter();
        for (HanabiCard cd : playerHand.getComponents()) {
            if (number != 0) {
                    if (cd.number == this.number) {
                        cd.numberVisibility = true;
                    }
                    else{
                        cd.possibleNumber.remove(Integer.valueOf(this.number));
                    }
            }
            else {
                if (cd.color.equals(this.color)) {
                    cd.colorVisibility = true;
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
    public boolean equals(Object obj) {
        return obj instanceof Play;
    }

    @Override
    public int hashCode() {
        return 0;
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