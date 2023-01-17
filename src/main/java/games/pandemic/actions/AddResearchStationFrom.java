package games.pandemic.actions;

import core.actions.AbstractAction;
import core.components.BoardNode;
import core.properties.PropertyBoolean;
import core.AbstractGameState;
import core.properties.PropertyString;
import games.pandemic.PandemicGameState;

import java.util.Objects;

import static core.CoreConstants.nameHash;


public class AddResearchStationFrom extends AddResearchStation {

    protected String fromCity;

    public AddResearchStationFrom(String from, String to) {
        super(to);
        this.fromCity = from;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        boolean success = super.execute(gs);

        // Remove research station from "fromCity" location
        BoardNode bn = pgs.getWorld().getNodeByStringProperty(nameHash, fromCity);
        if (bn != null) {
            bn.setProperty(new PropertyBoolean("Research Stations", false));
            pgs.removeResearchStation(((PropertyString) bn.getProperty(nameHash)).value);
        }

        return success;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof AddResearchStationFrom)
        {
            AddResearchStationFrom otherAction = (AddResearchStationFrom) other;
            return fromCity.equals(otherAction.fromCity);

        }else return false;
    }

    @Override
    public String toString() {
        return "Add Research Station in " + city + " from " + fromCity;
    }

    @Override
    public AbstractAction copy() {
        return new AddResearchStationFrom(this.fromCity, this.city);
    }

    public String getFromCity() {
        return fromCity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fromCity);
    }
}
