package core.actions;

import core.AbstractGameState;
import core.components.Card;

import java.util.Objects;

public class DoNothing implements IAction {
    private final int id = 0;

   @Override
    public boolean execute(AbstractGameState gs) {
       return true;
   }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DoNothing";
    }
}
