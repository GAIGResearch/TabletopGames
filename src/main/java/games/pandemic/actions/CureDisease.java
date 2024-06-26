package games.pandemic.actions;

import core.actions.AbstractAction;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;
import utilities.Hash;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;

import static core.CoreConstants.playerHandHash;
import static games.pandemic.PandemicConstants.playerDeckDiscardHash;

@SuppressWarnings("unchecked")
public class CureDisease extends AbstractAction {
    private String color;
    private ArrayList<Integer> cardIds;

    public CureDisease(String color, ArrayList<Integer> cardIds) {
        this.color = color;
        this.cardIds = cardIds;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Find disease counter
        PandemicGameState pgs = (PandemicGameState)gs;
        Counter diseaseCounter = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease " + color));
        if (diseaseCounter.getValue() == 0) {
            diseaseCounter.setValue(1);  // Set to cured

            // Discard cards from player hand
            Deck<Card> playerHand = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);
            Deck<Card> playerDiscard = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);
            for (Integer cardId: cardIds) {
                Card c = (Card)gs.getComponentById(cardId);
                playerHand.remove(c);
                playerDiscard.add(c);
            }

            return true;
        }

        return false;
    }

    @Override
    public AbstractAction copy() {
        ArrayList<Integer> cardIds = new ArrayList(this.cardIds);
        return new CureDisease(this.color, cardIds);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CureDisease)) return false;
        CureDisease that = (CureDisease) o;
        return Objects.equals(color, that.color) && cardIds.equals(that.cardIds);
    }

    public ArrayList<Integer> getCards() {
        return cardIds;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "Cure Disease " + color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, cardIds);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
