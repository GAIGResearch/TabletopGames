package pandemic.actions;

import actions.Action;
import components.BoardNode;
import content.PropertyBoolean;
import core.GameState;
import pandemic.PandemicGameState;

import static pandemic.Constants.*;


public class AddResearchStationFrom extends AddResearchStation implements Action {

    String fromCity;

    public AddResearchStationFrom(String from, String to) {
        super(to);
        this.fromCity = from;
    }

    @Override
    public boolean execute(GameState gs) {
        boolean success = super.execute(gs);

        // Remove research station from "fromCity" location
        BoardNode bn = ((PandemicGameState)gs).world.getNode(nameHash, fromCity);
        if (bn != null) {
            bn.setProperty(researchStationHash, new PropertyBoolean(false));
        }

        return success;
    }
}
