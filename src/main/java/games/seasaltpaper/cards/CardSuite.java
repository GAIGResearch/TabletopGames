package games.seasaltpaper.cards;

import core.actions.AbstractAction;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SeaSaltPaperParameters;
import games.seasaltpaper.actions.PlayDuo;

import java.util.List;
import java.util.function.BiFunction;

public enum CardSuite {

    //TODO move the collectorbonus and duoBonus to parameters
    BOAT(new SeaSaltPaperParameters().boatCollectorBonus, 1),
    FISH(new int[]{1, 2, 3}, 1),
    SHELL(new int[]{1, 2, 3}, 1),
    OCTOPUS(new int[]{1, 2, 3}, 1),
    PENGUIN(new int[]{1, 2, 3}, 1),
    SAILOR(new int[]{1, 2, 3}, 1),
    SHARK(new int[]{1, 2, 3}, 1);

    private final int[] collectorBonus;

    private final int duoBonus;

    CardSuite(int[] collectorBonus, int duoBonus) {
        this.collectorBonus = collectorBonus;
        this.duoBonus = duoBonus;
    }

    public int[] GetCollectorBonus() {
        return collectorBonus;
    }

    public int GetDuoBonus() {
        return duoBonus;
    }

}
