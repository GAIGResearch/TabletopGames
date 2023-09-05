package games.chinesecheckers.components;

import core.CoreConstants;
import core.components.Component;

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
    }

    CCNode occupiedNode;

    private Colour team;

    private boolean inDestination = false;

    public Peg(){
        super(CoreConstants.ComponentType.TOKEN, "PEG");

    }
    public Peg(Colour team, CCNode occupiedNode){
        super(CoreConstants.ComponentType.TOKEN, "PEG");
        this.team = team;
        this.occupiedNode = occupiedNode;
    }

    public void setOccupiedNode(CCNode occupiedNode)
    {
        this.occupiedNode = occupiedNode;
    }

    public void setInDestination(boolean value) { inDestination = value; }

    public CCNode getOccupiedNode()
    {
        if(occupiedNode == null) return null;
        else return occupiedNode;
    }

    public boolean getInDestination() { return inDestination; }
    public Colour getColour(){
        return team;
    }

    @Override
    public Component copy() {
        Peg copy = new Peg();
        copy.setInDestination(getInDestination());
        copy.team = getColour();
        return copy;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(componentID, occupiedNode, team, inDestination);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Peg) {
            Peg other = (Peg) o;
            return componentID == other.componentID && Objects.equals(occupiedNode, other.occupiedNode) && Objects.equals(team, other.team) && inDestination == other.inDestination;
        }
        return false;
    }
}


