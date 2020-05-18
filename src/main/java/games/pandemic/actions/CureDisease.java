package games.pandemic.actions;

import core.actions.IAction;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;
import utilities.Hash;

import java.util.ArrayList;
import java.util.Objects;

import static utilities.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class CureDisease implements IAction {
    private String color;
    private ArrayList<Card> cards;

    public CureDisease(String color, ArrayList<Card> cards) {
        this.color = color;
        this.cards = cards;
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
            for (Card c: cards) {
                playerHand.remove(c);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof CureDisease)
        {
            CureDisease otherAction = (CureDisease) other;
            if(!color.equals(otherAction.color)) return false;
            if(cards.size() != otherAction.cards.size()) return false;

            for(Card c : cards)
                if(!otherAction.cards.contains(c))  return false;

            return true;

        }else return false;
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "CureDisease{" +
                "color='" + color + '\'' +
                ", cards=" + cards.toString() +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, cards);
    }
}
