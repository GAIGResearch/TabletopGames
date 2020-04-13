package pandemic.actions;

import actions.Action;
import components.BoardNode;
import components.Card;
import components.Counter;
import components.Deck;
import content.PropertyIntArray;
import content.PropertyString;
import core.GameState;
import pandemic.Constants;
import pandemic.PandemicGameState;
import utilities.Hash;
import utilities.Utils;

import java.util.ArrayList;

import static pandemic.Constants.nameHash;
import static pandemic.Constants.playerHandHash;

public class CureDisease implements Action {
    String color;
    ArrayList<Card> cards;

    public CureDisease(String color, ArrayList<Card> cards) {
        this.color = color;
        this.cards = cards;
    }

    @Override
    public boolean execute(GameState gs) {
        // Find disease counter
        Counter diseaseCounter = gs.findCounter("Disease " + color);
        if (diseaseCounter.getValue() == 0) {
            diseaseCounter.setValue(1);  // Set to cured

            // Discard cards from player hand
            Deck playerHand = (Deck) gs.getAreas().get(gs.getActivePlayer()).getComponent(playerHandHash);
            for (Card c: cards) {
                playerHand.discard(c);
            }

            return true;
        }

        return false;
    }
}
