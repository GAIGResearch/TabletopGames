package games.descent2e.abilities;

import games.descent2e.DescentGameState;
import games.descent2e.components.DicePool;
import games.descent2e.components.DiceType;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.Collections;
import java.util.HashMap;

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

    public static int rollNightStalker(DescentGameState state, Figure attacker, Figure defender) {
        Vector2D position = attacker.getPosition();
        Vector2D other = defender.getPosition();
        if (Math.abs(position.getX() - other.getX()) > 1 || Math.abs(position.getY() - other.getY()) > 1) {
            getNightStalkerDicePool().roll(state.getRandom());
            System.out.println("Night Stalker is active! Reduced damaged by: " + getNightStalkerDicePool().getShields());
            return getNightStalkerDicePool().getShields();
        }
        else {
            return 0;
        }
    }
}
