package components;

import java.awt.*;
import java.util.HashMap;

import content.Property;
import utilities.Utils.ComponentType;

public class Card extends Component {
    private int cardType;
    private String name;
    private Color colour;
    private String text;
    private int occurenceCount;


    private HashMap<Integer, Property> properties; //Extra properties for this node.


    /* Constructors */

    public Card(){
        super.type = ComponentType.CARD;
    }

    public Card(String name){
        this.properties = new HashMap<>();

        super.type = ComponentType.CARD;
        this.name = name;
    }

    public Card(String name, int cardType){
        this.properties = new HashMap<>();

        super.type = ComponentType.CARD;
        this.name = name;
        this.cardType = cardType;
    }

    public Card(String name, int cardType, String text, int occurenceCount){
        this.properties = new HashMap<>();

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

        for(int prop_key : properties.keySet())
        {
            Property newProp = properties.get(prop_key).copy();
            copy.addProperty(prop_key, newProp);
        }

        return copy;
    }



    /**
     * Gets a property from the card properties.
     * @param propId id of the property to look for
     * @return the property value. Null if it doesn't exist.
     * TODO: This is repeated in Card, Deck, Board, Dice, etc. Worth abstracting it in a superclass?
     */
    public Property getProperty(int propId)
    {
        return properties.get(propId);
    }

    /**
     * Adds a property with an id and a Property object
     * @param propId ID of the property
     * @param prop property to add
     * TODO: This is repeated in Card, Deck, Board, Dice, etc. Worth abstracting it in a superclass?
     */
    public void addProperty(int propId, Property prop)
    {
        properties.put(propId, prop);
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
