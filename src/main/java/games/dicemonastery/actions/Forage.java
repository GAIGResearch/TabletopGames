package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import java.util.Random;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class Forage extends UseMonk {

    public static Random rnd = new Random();

    public Forage(int times) {
        super(times);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        for (int loop= 0; loop < actionPoints; loop++) {
            int roll = rnd.nextInt(6) + 1;
            Resource r = null;
            switch (roll) {
                case 1:
                    break;
                case 2:
                    r = PALE_BLUE_PIGMENT;
                    break;
                case 3:
                    r = Resource.PALE_GREEN_PIGMENT;
                    break;
                case 4:
                    r = Resource.PALE_RED_PIGMENT;
                    break;
                case 5:
                    // Technically a choice, but we'll just go with the least common type
                    int player = state.getCurrentPlayer();
                    int blue = state.getResource(player, PALE_BLUE_PIGMENT, STOREROOM) + state.getResource(player, PALE_BLUE_INK, STOREROOM);
                    int green = state.getResource(player, PALE_GREEN_PIGMENT, STOREROOM) + state.getResource(player, PALE_GREEN_INK, STOREROOM);
                    int red = state.getResource(player, PALE_RED_PIGMENT, STOREROOM) + state.getResource(player, PALE_RED_INK, STOREROOM);

                    if (blue <= green && blue <= red)
                        r = PALE_BLUE_PIGMENT;
                    else if (green <= red)
                        r = PALE_GREEN_PIGMENT;
                    else
                        r = PALE_RED_PIGMENT;
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
        }
        return true;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof Forage && actionPoints == ((Forage) obj).actionPoints;
    }

    @Override
    public int hashCode() {
        return 2482 + actionPoints;
    }

    @Override
    public String toString() {
        return String.format("Forage %d times", actionPoints);
    }
}
