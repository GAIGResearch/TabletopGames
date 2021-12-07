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

    public int getRandomNumberUsingNextInt (int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

    public Result getResult() {
        int value = getRandomNumberUsingNextInt(0, 3);
        switch(value) {
            case 1:
                return Result.Strike;
            case 2:
                return Result.Cleave;
            case 3:
                return Result.Pierce;
            case 4:
                return Result.Morale;
            case 5:
                return Result.Lore;
            case 6:
                return Result.Heroic;
            default:
                return Result.N_A;
        }
    }
}
