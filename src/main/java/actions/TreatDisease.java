package actions;

import components.BoardNode;
import components.Card;
import components.Counter;
import content.PropertyIntArray;
import content.PropertyString;
import core.GameState;
import pandemic.PandemicGameState;
import utilities.Hash;
import utilities.Utils;

import static actions.MovePlayer.playerLocationHash;
import static pandemic.PandemicGameState.*;

public class TreatDisease implements Action {
    String color;

    public TreatDisease(String color) {
        this.color = color;
    }

    @Override
    public boolean execute(GameState gs) {
        int colorIdx = Utils.indexOf(PandemicGameState.colors, color);

        // Find player current location
        int activePlayer = gs.getActivePlayer();
        PropertyString currentLocation = (PropertyString) ((Card) gs.getAreas().get(activePlayer).getComponent(playerCardHash)).getProperty(playerLocationHash);

        // Find board node
        BoardNode bn = ((PandemicGameState)gs).world.getNode("name", currentLocation.value);

        // Find disease counter
        Counter diseaseCounter = gs.findCounter(diseaseHash.get(colorIdx));

        // Find cube counters
        Counter cubeCounter = gs.findCounter(diseaseCubeHash.get(colorIdx));

        // Find infection array to update
        int[] infection = ((PropertyIntArray) bn.getProperty(Hash.GetInstance().hash("infection"))).getValues();

        if (diseaseCounter.getCounter() == 0) {
            if (infection[colorIdx] > 0) {
                infection[colorIdx] -= 1;
                cubeCounter.increment(1);
            }
        } else {  // Disease cured or eradicated, remove all cubes
            cubeCounter.increment(infection[colorIdx]);
            infection[colorIdx] = 0;
        }



        return false;
    }
}
