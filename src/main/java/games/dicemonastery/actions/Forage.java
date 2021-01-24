package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;

import java.util.Random;

import static games.dicemonastery.DiceMonasteryConstants.*;

public class Forage extends UseMonk {

    public static Random rnd = new Random();

    public Forage() {
        super(1);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        int roll = rnd.nextInt(6) + 1;
        Resource r = null;
        switch (roll) {
            case 1:
            case 2:
                break;
            case 3:
            case 4:
                r = Resource.PIGMENT;
                break;
            case 5:
            case 6:
                r = Resource.GRAPES;
                break;
            default:
                throw new AssertionError("Unexpected number rolled : " + roll);
        }
        if (r != null) {
            state.addResource(state.getCurrentPlayer(), r, 1);
        }
        return true;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof Forage;
    }

    @Override
    public int hashCode() {
        return 2482;
    }

    @Override
    public String toString() {
        return "Forage";
    }
}
