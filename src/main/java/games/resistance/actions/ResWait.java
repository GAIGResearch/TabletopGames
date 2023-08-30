package games.resistance.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.resistance.ResGameState;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResWait extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public ResWait copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ResWait);
    }

    @Override
    public int hashCode() {
        return 4739;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Is Waiting ";
    }


}
