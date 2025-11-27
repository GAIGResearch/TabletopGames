package games.seasaltpaper.cards;

import core.actions.AbstractAction;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SeaSaltPaperParameters;
import games.seasaltpaper.actions.PlayDuo;

import java.util.List;
import java.util.function.BiFunction;

public enum CardSuite {

    //TODO move the collectorbonus and duoBonus to parameters
    //TODO store card properties (e.g. collector bonus) in parameters, and intialize them directly with forwardModel setup, not here
    BOAT(new int[]{}, 1, 1),
    FISH(new int[]{}, 1, 1),
    CRAB(new int[]{}, 1, 1),
    SHELL(new int[]{0, 2, 4, 6, 8, 10}, 1, 0), // TODO SHELL != CRAB, ADD CRAB SUITE AND RENAME SHELLDUO TO CRABDUO
    OCTOPUS(new int[]{0, 3, 6, 9, 12}, 1, 0),
    PENGUIN(new int[]{1, 3, 5}, 1, 2),
    SAILOR(new int[]{0, 5}, 1, 3),
    SHARK(new int[]{}, 1, 0),
    SWIMMER(new int[]{}, 1, 0),
    MERMAID(new int[]{}, 0, 0);

    private final int[] collectorBonus;

    private final int duoBonus;

    private final int multiplier;

    CardSuite(int[] collectorBonus, int duoBonus, int multiplier) {
        this.collectorBonus = collectorBonus;
        this.duoBonus = duoBonus;
        this.multiplier = multiplier;
    }

    public int[] getCollectorBonus() {
        return collectorBonus;
    }

    public int getDuoBonus() {
        return duoBonus;
    }

    public int getMultiplier()
    {
        return multiplier;
    }

}
