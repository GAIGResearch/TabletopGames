package pandemic.actions;

import actions.IAction;
import components.Card;
import components.Counter;
import components.Deck;
import core.AbstractGameState;
import pandemic.PandemicGameState;
import turnorder.TurnOrder;
import utilities.Hash;

import java.util.ArrayList;

import static pandemic.Constants.playerHandHash;

public class CureDisease implements IAction {
    private String color;
    private ArrayList<Card> cards;

    public CureDisease(String color, ArrayList<Card> cards) {
        this.color = color;
        this.cards = cards;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        // Find disease counter
        PandemicGameState pgs = (PandemicGameState)gs;
        Counter diseaseCounter = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease " + color));
        if (diseaseCounter.getValue() == 0) {
            diseaseCounter.setValue(1);  // Set to cured

            // Discard cards from player hand
            Deck playerHand = (Deck) pgs.getComponent(playerHandHash, ((PandemicGameState) gs).getActingPlayer());
            for (Card c: cards) {
                playerHand.discard(c);
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
}
