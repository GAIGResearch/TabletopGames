package games.pandemic.actions;

import core.actions.IAction;
import core.components.BoardNode;
import core.components.Counter;
import core.content.PropertyBoolean;
import core.AbstractGameState;
import core.content.PropertyString;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;

import static games.pandemic.PandemicConstants.*;
import static utilities.CoreConstants.nameHash;

public class AddResearchStation implements IAction {
    private String city;

    public AddResearchStation(String city) {
        this.city = city;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        BoardNode bn = ((PandemicGameState)gs).world.getNode(nameHash, city);
        PandemicGameState pgs = (PandemicGameState)gs;
        if (bn != null) {
            bn.setProperty(researchStationHash, new PropertyBoolean(true));
            Counter rStationCounter = (Counter) pgs.getComponent(PandemicConstants.researchStationHash);
            rStationCounter.decrement(1); // We have one less research station
            pgs.addResearchStation(((PropertyString) bn.getProperty(nameHash)).value);
            return true;
        }
        return false;
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
}
