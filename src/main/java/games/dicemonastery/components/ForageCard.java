package games.dicemonastery.components;

import core.components.Component;
import utilities.Utils;

import java.util.Objects;

public class ForageCard extends Component {

    public final int green;
    public final int red;
    public final int blue;

    public ForageCard(int green, int red, int blue) {
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
