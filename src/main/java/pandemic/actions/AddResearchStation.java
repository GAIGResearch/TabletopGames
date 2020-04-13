package pandemic.actions;

import actions.Action;
import components.BoardNode;
import content.PropertyBoolean;
import core.GameState;
import pandemic.PandemicGameState;
import utilities.Hash;

public class AddResearchStation implements Action {
    private int researchStationHash = Hash.GetInstance().hash("researchStation");
    private String city;

    public AddResearchStation(String city) {
        this.city = city;
    }

    @Override
    public boolean execute(GameState gs) {
        BoardNode bn = ((PandemicGameState)gs).world.getNode("name", city);
        if (bn != null) {
            bn.setProperty(researchStationHash, new PropertyBoolean(true));
            gs.findCounter("Research Stations").decrement(1); // We have one less research station
            return true;
        }
        return false;
    }
}
