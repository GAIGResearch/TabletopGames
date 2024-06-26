package games.pandemic.actions;

import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.Counter;
import core.properties.PropertyIntArray;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;
import utilities.Hash;
import utilities.Utils;

import java.util.Objects;

import static games.pandemic.PandemicConstants.*;
import static core.CoreConstants.nameHash;

public class TreatDisease extends AbstractAction {

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
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState) gs;

        Counter diseaseToken = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease " + color));
        Counter diseaseCubeCounter = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + color));
        int colorIdx = Utils.indexOf(colors, color);

        BoardNode bn = pgs.getWorld().getNodeByStringProperty(nameHash, city);
        if (bn != null) {
            PropertyIntArray infectionArray = (PropertyIntArray) bn.getProperty(infectionHash);
            int[] array = infectionArray.getValues();

            boolean disease_cured = diseaseToken.getValue() > 0;

            if (!disease_cured && !treatAll) {  // Only remove 1 cube
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
    public AbstractAction copy() {
        return new TreatDisease(initialDiseaseCubes, color, city, treatAll);
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

    @Override
    public String  toString() {
        return "Treat Disease " + color + " in " + city + (treatAll? " (all)" : "");
    }

    @Override
    public int hashCode() {
        return Objects.hash(initialDiseaseCubes, color, city, treatAll);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
