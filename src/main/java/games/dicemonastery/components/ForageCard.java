package games.dicemonastery.components;

import core.components.Component;
import core.properties.*;
import utilities.*;

import java.util.*;

public class ForageCard extends Component {

    final static int mHash = Hash.GetInstance().hash("multiplicity");
    final static int redHash = Hash.GetInstance().hash("red");
    final static int blueHash = Hash.GetInstance().hash("blue");
    final static int greenHash = Hash.GetInstance().hash("green");


    public final int green;
    public final int red;
    public final int blue;

    public static List<ForageCard> create(Component c, int playerCount) {
        List<ForageCard> retValue = new ArrayList<>();
        PropertyInt multiProp = (PropertyInt) c.getProperty(mHash);
        int multiplicity = multiProp == null ? 1 : multiProp.value;
        int[] green = ((PropertyIntArray) c.getProperty(greenHash)).getValues();
        int[] red = ((PropertyIntArray) c.getProperty(redHash)).getValues();
        int[] blue = ((PropertyIntArray) c.getProperty(blueHash)).getValues();
        for (int i = 0; i < multiplicity; i++)
            retValue.add(new ForageCard(green[playerCount-2], red[playerCount-2], blue[playerCount-2]));
        return retValue;
    }

    private ForageCard(int green, int red, int blue) {
        super(Utils.ComponentType.CARD, "Forage Card");
        this.blue = blue;
        this.green = green;
        this.red = red;
    }

    @Override
    public Component copy() {
        return this;  // immutable
    }

    @Override
    public String toString() {
        return String.format("Green: %d, Red: %d, Blue: %d", green, red, blue);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ForageCard) {
            ForageCard other = (ForageCard) o;
            return other.green == green && other.red == red &&
                    other.blue == blue;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 30344 + Objects.hash(green, red, blue);
    }
}
