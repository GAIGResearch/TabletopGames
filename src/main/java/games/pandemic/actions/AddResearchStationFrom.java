package games.pandemic.actions;

import core.actions.IAction;
import core.components.BoardNode;
import core.components.Card;
import core.content.PropertyBoolean;
import core.AbstractGameState;
import core.content.PropertyString;
import games.pandemic.PandemicGameState;

import java.util.Objects;

import static games.pandemic.PandemicConstants.*;
import static utilities.CoreConstants.nameHash;


public class AddResearchStationFrom extends AddResearchStation implements IAction {

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
        BoardNode bn = pgs.world.getNode(nameHash, fromCity);
        if (bn != null) {
            bn.setProperty(researchStationHash, new PropertyBoolean(false));
            pgs.removeResearchStation(((PropertyString) bn.getProperty(nameHash)).value);
        }

        return success;
    }

    @Override
    public Card getCard() {
        return null;
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
        return "AddResearchStationFrom{" +
                "fromCity='" + fromCity + '\'' +
                ", toCity='" + city + '\'' +
                '}';
    }

    public String getFromCity() {
        return fromCity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fromCity);
    }
}
