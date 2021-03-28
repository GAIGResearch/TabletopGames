package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import java.util.Random;

import static games.dicemonastery.DiceMonasteryConstants.Resource;

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
                break;
            case 2:
                r = Resource.PALE_BLUE_PIGMENT;
                break;
            case 3:
                r = Resource.PALE_GREEN_PIGMENT;
                break;
            case 4:
                r = Resource.PALE_RED_PIGMENT;
                break;
            case 5:
                // TODO: This is actually a choice, requiring FORAGE to be turned into an Extended Sequence
                int rndPigment = rnd.nextInt(3);
                if (rndPigment == 0)
                    r = Resource.PALE_BLUE_PIGMENT;
                if (rndPigment == 1)
                    r = Resource.PALE_GREEN_PIGMENT;
                if (rndPigment == 2)
                    r = Resource.PALE_RED_PIGMENT;
                break;
            case 6:
                r = Resource.BERRIES;
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
