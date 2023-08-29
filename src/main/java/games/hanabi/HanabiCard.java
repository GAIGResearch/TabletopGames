package games.hanabi;

import core.components.Card;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class HanabiCard extends Card {

    public CardType color;
    public int number;
    public boolean colorVisibility;
    public boolean numberVisibility;
    public List<Integer> possibleNumber;
    public List<CardType>  possibleColour;

    public boolean ownerKnowsColor, ownerKnowsNumber;

    public HanabiCard(CardType color, int number){
        super(color.name() + number);
        this.color = color;
        this.number = number;
        this.colorVisibility = true;
        this.numberVisibility = true;
        this.possibleNumber = new ArrayList<>();
        for(int i=1; i<=5; i++){
            this.possibleNumber.add(i);
        }
        this.possibleColour = new ArrayList<>();
        this.possibleColour.addAll(Arrays.asList(CardType.values()));
    }


    protected HanabiCard(CardType color, int number, int componentID){
        super(color.name() + number, componentID);
        this.color = color;
        this.number = number;
    }

    @Override
    public HanabiCard copy(int playerId) {
        HanabiCard card = new HanabiCard(color, number, componentID);
        card.colorVisibility = (ownerId == playerId ? ownerKnowsColor : colorVisibility);
        card.numberVisibility = (ownerId == playerId ? ownerKnowsNumber : numberVisibility);
        card.ownerKnowsColor = ownerKnowsColor;
        card.ownerKnowsNumber = ownerKnowsNumber;
        card.possibleColour = new ArrayList<>(possibleColour);
        card.possibleNumber = new ArrayList<>(possibleNumber);
        return card;
    }

    public HanabiCard copy() {
        HanabiCard card = new HanabiCard(color, number, componentID);
        card.colorVisibility = colorVisibility;
        card.numberVisibility = numberVisibility;
        card.ownerKnowsColor = ownerKnowsColor;
        card.ownerKnowsNumber = ownerKnowsNumber;
        card.possibleColour = new ArrayList<>(possibleColour);
        card.possibleNumber = new ArrayList<>(possibleNumber);
        return card;
    }

    @Override
    public String toString() {
        return "{" + (colorVisibility? color : "UnknownColor") + " " + (numberVisibility? number : "UnknownNumber") + "}";
    }

    @Override
    public String toString(int playerId) {
        return "{" + getColorStr(playerId) + " " + getNumberStr(playerId) + "}";
    }

    public String getColorStr(int playerId) {
        return (playerId == ownerId? (ownerKnowsColor? color.name() : "?Color?") : colorVisibility ? color.name(): "?Color?");
    }

    public String getNumberStr(int playerId) {
        return (playerId == ownerId? (ownerKnowsNumber? ""+number : "?Number?") : numberVisibility ? ""+number : "?Number?");
    }

    // create setters
    public void setNumber(int n){
        this.number = n;
    }

    public void setColor(CardType c){
        this.color = c;
    }
}
