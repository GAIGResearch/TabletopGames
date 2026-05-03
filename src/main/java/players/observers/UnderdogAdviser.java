package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

public class UnderdogAdviser implements IAdviceFilter {

    @Override
    public boolean payAttention(AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee) {
        return false; // TODO
    }

    @Override
    public boolean provideAdvice(AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee, AbstractAction advice, GameAdviser adviser) {
        return false; // TODO
    }
}
