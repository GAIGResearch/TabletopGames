package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.Hero;

public class Rest extends DescentAction{
    public Rest() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Rest";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        ((Hero)gs.getActingFigure()).setRested(true);
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }
}
