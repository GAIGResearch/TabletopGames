package components;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;


public class Token extends Component {
    // position should be reference to a graph
    private int position;           // todo graph
    private String token_type;            // string type or name
    private Color colour;           // colour
    private HashSet<Integer> owner; // owner
    private int value;              // value
    private int occurenceLimit;     // occurence limit

    public Token(){

    }

    public Token(String token_type){
        this.token_type = token_type;
    }

    public Token copy(){
        Token copy = new Token();
        copy.position = position;
        copy.token_type = new String(token_type);
        copy.colour = new Color(colour.getRGB());
        copy.owner = (HashSet)owner.clone();
        copy.value = value;
        copy.occurenceLimit = occurenceLimit;
        return copy;
    }

    public String getTokenType() {
        return token_type;
    }

    public void setTokenType(String token_type) {
        this.token_type = token_type;
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

    public static void main(String[] args) {
        // Example token setup for Pandemic
        Color[] colours = {Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED};
        ArrayList<Token> pawns = new ArrayList<>();
        for (int i = 0; i < 7; i++){
            pawns.add(new Token("pawn"));
        }
        ArrayList<Token> disaseCubes = new ArrayList<>();
        for (int i = 0; i < 4; i++){
            for (int j = 0; j < 24; j++){
                Token disaseCube = new Token("disaseCube");
                disaseCube.setColour(colours[i]);
                disaseCubes.add(disaseCube);
            }
        }
        ArrayList<Token> cureMarkers = new ArrayList<>();
        for (int i =0 ; i < 4; i ++){
            cureMarkers.add(new Token("cureMarker"));
        }
        ArrayList<Token> researchStations = new ArrayList<>();
        for (int i = 0 ; i < 6; i++){
            researchStations.add(new Token("researchStation"));
        }
    }
}
