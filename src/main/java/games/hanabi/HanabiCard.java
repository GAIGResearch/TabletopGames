package games.hanabi;

import core.components.Card;
import core.components.Deck;
import games.hanabi.HanabiGameState;
import java.util.List;
import java.util.ArrayList;

public class HanabiCard extends Card {

    public enum HanabiCardType {
        Number
//        SwapHands
    }

    public String color;
    public final HanabiCardType type;
    public int number;
    public final int drawN;
    public boolean colorVisibility;
    public boolean numberVisibility;
    public List<Integer> possibleNumber;
    public List<String>  possibleColour;


    public HanabiCard(HanabiCardType type, String color, int number){
        super(type.toString());
        this.color = color;
        this.type = type;
        this.number = number;
        this.drawN = -1;
        this.colorVisibility = false;
        this.numberVisibility = false;
        this.possibleNumber = new ArrayList<Integer>();
        for(int i=1; i<=5; i++){
            this.possibleNumber.add(i);
        }
        this.possibleColour = new ArrayList<String>();
        this.possibleColour.add("Yellow");
        this.possibleColour.add("White");
        this.possibleColour.add("Red");
        this.possibleColour.add("Green");
        this.possibleColour.add("Blue");
    }


    @Override
    public HanabiCard copy() {
        return new HanabiCard(type, color, number);
    }


    @Override
    public String toString() {
        return "{" + color + " " + number + "}";
    }

    // create setters
    public void setNumber(int n){
        this.number = n;
    }

    public void setColor(String c){
        this.color = c;
    }
}
