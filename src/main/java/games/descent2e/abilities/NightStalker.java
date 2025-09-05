package games.descent2e.abilities;

import games.descent2e.DescentGameState;
import games.descent2e.components.*;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static games.descent2e.components.DiceType.BROWN;

// This monster gains an extra Brown defence die when its attacker is not adjacent to it
public class NightStalker {

    public static HashMap<DiceType, Integer> nightStalkerDice = new HashMap<DiceType, Integer>() {{
        put(BROWN, 1);
    }};

    public static DicePool nightStalkerDicePool = DicePool.constructDicePool(nightStalkerDice);

    public static DicePool getNightStalkerDicePool() {
        return nightStalkerDicePool;
    }

    public static void addNightStalker(DescentGameState state, Vector2D position, Vector2D other) {
        // Apply Night Stalker ability if the attacker is not adjacent to the defender
        if (Math.abs(position.getX() - other.getX()) > 1 || Math.abs(position.getY() - other.getY()) > 1) {
            List<DescentDice> dice = new ArrayList<>(state.getDefenceDicePool().getComponents());
            dice.addAll(getNightStalkerDicePool().getComponents());
            DicePool newPool = state.getDefenceDicePool().copy();
            newPool.setDice(dice);
            state.setDefenceDicePool(newPool);
            //System.out.println("Night Stalker is active! " + defender.getComponentName() + " gains 1 " + getNightStalkerDicePool().getComponents().get(0).getColour() + " die against this attack!");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }
    @Override
    public int hashCode() {
        return -2201402;
    }
}
