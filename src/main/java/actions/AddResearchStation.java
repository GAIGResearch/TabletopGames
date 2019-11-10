package actions;

import components.Board;
import components.BoardNode;
import content.PropertyBoolean;
import content.PropertyString;
import core.GameState;
import pandemic.PandemicGameState;
import utilities.Hash;

import static pandemic.PandemicGameState.pandemicBoardHash;

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
            bn.addProperty(researchStationHash, new PropertyBoolean(true));  // TODO: change the value
            //gs.findCounter(researchStationCounterHash).decrement(1); // We have one less research station
            return true;
        }
        return false;
    }
}
