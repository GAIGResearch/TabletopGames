package pandemic.actions;

import actions.IAction;
import components.BoardNode;
import content.PropertyBoolean;
import core.AbstractGameState;
import pandemic.PandemicGameState;
import turnorder.TurnOrder;

import static pandemic.Constants.*;


public class AddResearchStationFrom extends AddResearchStation implements IAction {

    private String fromCity;

    public AddResearchStationFrom(String from, String to) {
        super(to);
        this.fromCity = from;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        boolean success = super.Execute(gs, turnOrder);

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
