package games.dominion.actions;

import games.dominion.DominionGameState;

public interface IBuyPhaseEffect {

    boolean apply(DominionGameState state);
}
