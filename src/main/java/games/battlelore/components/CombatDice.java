package games.battlelore.components;

import core.components.Dice;

public class CombatDice extends Dice
{
    public enum Result
    {
        N_A, Strike, Cleave, Pierce, Morale, Lore, Heroic
    }

    public CombatDice(int defaultNum)
    {
        super();
    }

    public Result getResult()
    {
        int value = super.getValue();
        switch(value)
        {
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
