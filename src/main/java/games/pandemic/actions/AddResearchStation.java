package games.pandemic.actions;

import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.Counter;
import core.properties.PropertyBoolean;
import core.AbstractGameState;
import core.properties.PropertyString;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;

import java.util.Objects;

import static core.CoreConstants.nameHash;

public class AddResearchStation extends AbstractAction {
    protected String city;

    public AddResearchStation(String city) {
        this.city = city;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        BoardNode bn = ((PandemicGameState)gs).getWorld().getNodeByStringProperty(nameHash, city);
        PandemicGameState pgs = (PandemicGameState)gs;
        if (bn != null) {
            bn.setProperty(new PropertyBoolean("Research Stations", true));
            Counter rStationCounter = (Counter) pgs.getComponent(PandemicConstants.researchStationHash);
            rStationCounter.decrement(1); // We have one less research station
            pgs.addResearchStation(((PropertyString) bn.getProperty(nameHash)).value);
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new AddResearchStation(this.city);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof AddResearchStation)
        {
            AddResearchStation otherAction = (AddResearchStation) other;
            return city.equals(otherAction.city);

        }else return false;
    }

    @Override
    public String toString() {
        return "Add Research Station in " + city;
    }

    public String getCity() {
        return city;
    }

    @Override
    public int hashCode() {
        return Objects.hash(city);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
