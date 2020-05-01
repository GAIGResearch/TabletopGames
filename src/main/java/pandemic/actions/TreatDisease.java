package pandemic.actions;

import actions.IAction;
import components.BoardNode;
import components.Counter;
import content.PropertyIntArray;
import core.AbstractGameState;
import pandemic.PandemicGameState;
import turnorder.TurnOrder;
import utilities.Hash;
import utilities.Utils;

import static pandemic.Constants.*;

public class TreatDisease implements IAction {

    //PandemicParameters gp;
    private int initialDiseaseCubes;
    private String color;
    private String city;
    private boolean treatAll;

    public TreatDisease(int initialDiseaseCubes, String color, String city) {
        this.initialDiseaseCubes = initialDiseaseCubes;
        this.color = color;
        this.city = city;
        this.treatAll = false;
    }

    public TreatDisease(int initialDiseaseCubes, String color, String city, boolean treatAll) {
        this.initialDiseaseCubes = initialDiseaseCubes;
        this.color = color;
        this.city = city;
        this.treatAll = treatAll;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        PandemicGameState pgs = (PandemicGameState) gs;

        Counter diseaseToken = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease " + color));
        Counter diseaseCubeCounter = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + color));
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
            if (diseaseToken.getValue() == 1 && diseaseCubeCounter.getValue() == initialDiseaseCubes) {
                diseaseToken.setValue(2);
            }

            return true;
        }
        return false;
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof TreatDisease)
        {
            TreatDisease otherAction = (TreatDisease) other;
            return color.equals(otherAction.color) && city.equals(otherAction.city) &&
                    initialDiseaseCubes == otherAction.initialDiseaseCubes && treatAll == otherAction.treatAll;

        }else return false;
    }
}
