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

    public static void addNightStalker(DescentGameState state, Figure attacker, Figure defender) {
        Vector2D position = attacker.getPosition();
        Vector2D other = defender.getPosition();
        if (Math.abs(position.getX() - other.getX()) > 1 || Math.abs(position.getY() - other.getY()) > 1) {
            List<DescentDice> dice = new ArrayList<>(state.getDefenceDicePool().getComponents());
            dice.addAll(getNightStalkerDicePool().getComponents());
            DicePool newPool = state.getDefenceDicePool().copy();
            newPool.setDice(dice);
            state.setDefenceDicePool(newPool);
            System.out.println("Night Stalker is active! Barghest gains 1 " + getNightStalkerDicePool().getComponents().get(0).getColour() + " die against this attack!");
        }
    }
}
