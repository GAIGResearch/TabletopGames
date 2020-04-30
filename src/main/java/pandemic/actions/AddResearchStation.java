package pandemic.actions;

import actions.Action;
import components.BoardNode;
import components.Counter;
import content.PropertyBoolean;
import core.GameState;
import pandemic.Constants;
import pandemic.PandemicGameState;
import static pandemic.Constants.*;

public class AddResearchStation implements Action {
    private String city;

    public AddResearchStation(String city) {
        this.city = city;
    }

    @Override
    public boolean execute(GameState gs) {
        BoardNode bn = ((PandemicGameState)gs).world.getNode(nameHash, city);
        PandemicGameState pgs = (PandemicGameState)gs;
        if (bn != null) {
            bn.setProperty(researchStationHash, new PropertyBoolean(true));
            Counter rStationCounter = (Counter) pgs.getComponent(Constants.researchStationHash);
            rStationCounter.decrement(1); // We have one less research station
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
