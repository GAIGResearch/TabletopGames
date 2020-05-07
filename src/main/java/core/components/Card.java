package core.components;

import java.util.HashMap;

import core.content.PropertyString;
import utilities.Utils.ComponentType;

import static utilities.CoreConstants.nameHash;

public class Card extends Component implements Cloneable{

    //private int occurenceCount; //This was here once. Not sure why?

    public Card(){
        this.properties = new HashMap<>();
        super.type = ComponentType.CARD;
    }

    @Override
    public Card clone(){
        Card copy = new Card();
        copy.type = type;

        copyComponentTo(copy);

        return copy;
    }

    @Override
    public String toString() {
        return ((PropertyString)getProperty(nameHash)).value;
    }
}
