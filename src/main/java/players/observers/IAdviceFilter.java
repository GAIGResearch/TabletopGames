package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;

@FunctionalInterface
public interface IAdviceFilter {

    boolean advise(AbstractGameState state, AbstractPlayer player);
}
