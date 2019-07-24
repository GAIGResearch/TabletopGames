package core;

import java.awt.*;

public class Card extends Component {
    private int cardType;
    private String name;
    private Color colour;
    private String text;
    private int occurenceCount;

    /* Constructors */

    public Card(){
        super.type = ComponentType.CARD;
    }

    public Card(String name){
        super.type = ComponentType.CARD;
        this.name = name;
    }

    public Card(String name, int cardType){
        super.type = ComponentType.CARD;
        this.name = name;
        this.cardType = cardType;
    }

    public Card(String name, int cardType, String text, int occurenceCount){
        super.type = ComponentType.CARD;
        this.name = name;
        this.cardType = cardType;
        this.text = text;
        this.occurenceCount = occurenceCount;
    }

    public Card copy(){
        Card copy = new Card();
        copy.name = new String(this.name);
        copy.type = type;
        copy.cardType = cardType;
        copy.text = new String(text);
        copy.occurenceCount = occurenceCount;
        copy.colour = new Color(this.colour.getRGB());
        return copy;
    }

    /* Getters and Setters */

    public int getCardType() {
        return cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getColour() {
        return colour;
    }

    public void setColour(Color colour) {
        this.colour = colour;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getOccurenceCount() {
        return occurenceCount;
    }

    public void setOccurenceCount(int occurenceCount) {
        this.occurenceCount = occurenceCount;
    }
}
