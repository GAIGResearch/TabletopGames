package pandemic.actions;

import actions.Action;
import components.BoardNode;
import components.Counter;
import content.PropertyIntArray;
import core.GameParameters;
import core.GameState;
import pandemic.PandemicGameState;
import pandemic.PandemicParameters;
import utilities.Utils;

import static pandemic.Constants.*;

public class TreatDisease implements Action {

    PandemicParameters gp;
    String color;
    String city;
    boolean treatAll;

    public TreatDisease(GameParameters gp, String color, String city) {
        this.gp = (PandemicParameters)gp;
        this.color = color;
        this.city = city;
        this.treatAll = false;
    }

    public TreatDisease(GameParameters gp, String color, String city, boolean treatAll) {
        this.gp = (PandemicParameters)gp;
        this.color = color;
        this.city = city;
        this.treatAll = treatAll;
    }

    @Override
    public boolean execute(GameState gs) {
        PandemicGameState pgs = (PandemicGameState) gs;

        Counter diseaseToken = pgs.findCounter("Disease " + color);
        Counter diseaseCubeCounter = gs.findCounter("Disease Cube " + color);
        int colorIdx = Utils.indexOf(colors, color);

        BoardNode bn = pgs.world.getNode(nameHash, city);
        if (bn != null) {
            PropertyIntArray infectionArray = (PropertyIntArray) bn.getProperty(infectionHash);
            int[] array = infectionArray.getValues();

            boolean disease_cured = diseaseToken.getValue() > 0;
            if (!disease_cured || !treatAll) {  // Only remove 1 cube
                diseaseCubeCounter.increment(Math.min(array[colorIdx], 1));
                array[colorIdx] = Math.max(0, array[colorIdx] - 1);
            } else {
                diseaseCubeCounter.increment(array[colorIdx]);
                array[colorIdx] = 0;
            }

            // If disease cured and no more cubes of this color on the map, disease becomes eradicated
            if (diseaseToken.getValue() == 1 && diseaseCubeCounter.getValue() == gp.n_initial_disease_cubes) {
                diseaseToken.setValue(2);
            }

            return true;
        }
        return false;
    }
}
