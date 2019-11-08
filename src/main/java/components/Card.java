package components;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import content.Property;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils.ComponentType;

public class Card extends Component {

    //private int occurenceCount; //This was here once. Not sure why?

    public Card(){
        this.properties = new HashMap<>();
        super.type = ComponentType.CARD;
    }

    public Card copy(){
        Card copy = new Card();
        copy.type = type;

        copyComponent(copy);

        return copy;
    }

}
