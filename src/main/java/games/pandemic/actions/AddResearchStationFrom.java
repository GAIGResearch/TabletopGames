package games.pandemic.actions;

import core.actions.IAction;
import core.components.BoardNode;
import core.content.PropertyBoolean;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import static games.pandemic.Constants.*;


public class AddResearchStationFrom extends AddResearchStation implements IAction {

    private String fromCity;

    public AddResearchStationFrom(String from, String to) {
        super(to);
        this.fromCity = from;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean success = super.execute(gs);

        // Remove research station from "fromCity" location
        BoardNode bn = ((PandemicGameState)gs).world.getNode(nameHash, fromCity);
        if (bn != null) {
            bn.setProperty(researchStationHash, new PropertyBoolean(false));
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
}
