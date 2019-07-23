import java.awt.*;

public class Card {
    private int type;
    private String name;
    private Color colour;
    private String text;
    private int occurenceCount;

    /* Constructors */

    public Card(){}

    public Card(String name){
        this.name = name;
    }

    public Card(String name, int type){
        this.name = name;
        this.type = type;
    }

    public Card(String name, int type, String text, int occurenceCount){
        this.name = name;
        this.type = type;
        this.text = text;
        this.occurenceCount = occurenceCount;
    }

    public Card copy(){
        Card copy = new Card();
        copy.name = new String(this.name);
        copy.type = type;
        copy.text = new String(text);
        copy.occurenceCount = occurenceCount;
        copy.colour = new Color(this.colour.getRGB());
        return copy;
    }

    /* Getters and Setters */

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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
