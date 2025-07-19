package games.chinesecheckers.components;

import core.CoreConstants;
import core.components.Component;

import java.awt.*;
import java.util.Objects;

public class Peg extends Component {

    public enum Colour {
        purple,
        blue,
        yellow,
        red,
        orange,
        green,
        neutral;

        public Color toGraphicsColor() {
            switch (this) {
                case purple:
                    return Color.magenta;
                case blue:
                    return Color.blue;
                case yellow:
                    return Color.yellow;
                case red:
                    return Color.red;
                case orange:
                    return Color.orange;
                case green:
                    return Color.green;
                case neutral:
                    return Color.black;
                default:
                    throw new AssertionError("Unknown colour: " + this);
            }
        }
    }

    // componentID is used to identify the peg in the game state
    int occupiedNode;

    private Colour team;

    private boolean inDestination = false;

    public Peg(Colour team, CCNode occupiedNode) {
        super(CoreConstants.ComponentType.TOKEN, "PEG");
        this.team = team;
        this.occupiedNode = occupiedNode.getComponentID();
    }

    private Peg(int componentID) {
        super(CoreConstants.ComponentType.TOKEN, "PEG", componentID);
    }

    public void setInDestination(boolean value) {
        inDestination = value;
    }

    public boolean getInDestination() {
        return inDestination;
    }

    public Colour getColour() {
        return team;
    }

    @Override
    public Component copy() {
        Peg copy = new Peg(this.componentID);
        copy.inDestination = this.inDestination;
        copy.team = team;
        copy.occupiedNode = occupiedNode;
        return copy;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(componentID, occupiedNode, team, inDestination);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Peg other) {
            return componentID == other.componentID && occupiedNode == other.occupiedNode &&
                    team == other.team && inDestination == other.inDestination;
        }
        return false;
    }
}


