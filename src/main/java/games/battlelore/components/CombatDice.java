package games.battlelore.components;

import core.components.Dice;
import java.util.Random;

public class CombatDice extends Dice {
    public enum Result {
        N_A, Strike, Cleave, Pierce, Morale, Lore, Heroic
    }

    public CombatDice() {
        super();
    }

    public Result getResult(Random rnd) {
        int value = rnd.nextInt(6);
        switch(value) {
            case 0:
                return Result.Strike;
            case 1:
                return Result.Cleave;
            case 2:
                return Result.Pierce;
            case 3:
                return Result.Morale;
            case 4:
                return Result.Lore;
            case 5:
                return Result.Heroic;
            default:
                throw new AssertionError("Invalid value: " + value);
        }
    }
}
