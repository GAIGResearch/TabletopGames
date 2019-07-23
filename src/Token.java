import java.awt.*;
import java.util.HashSet;

public class Token {
    // position should be reference to a graph
    private int position;           // todo graph
    private String type;            // string type or name
    private Color colour;           // colour
    private HashSet<Integer> owner; // owner
    private String shape;           // shape
    private int value;              // value
    private int occurenceLimit;     // occurence limit

    public Token(){

    }

    public Token(String type){
        this.type = type;
    }

    public Token copy(){
        Token copy = new Token();
        copy.position = position;
        copy.type = new String(type);
        copy.colour = new Color(colour.getRGB());
        copy.owner = (HashSet)owner.clone();
        copy.shape = new String(shape);
        copy.value = value;
        copy.occurenceLimit = occurenceLimit;
        return copy();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Color getColour() {
        return colour;
    }

    public void setColour(Color colour) {
        this.colour = colour;
    }

    public HashSet<Integer> getOwner() {
        return owner;
    }

    public void setOwner(HashSet<Integer> owner) {
        this.owner = owner;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getOccurenceLimit() {
        return occurenceLimit;
    }

    public void setOccurenceLimit(int occurenceLimit) {
        this.occurenceLimit = occurenceLimit;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {

        return position;
    }
}
