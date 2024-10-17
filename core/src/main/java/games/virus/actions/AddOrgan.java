package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.virus.cards.VirusCard;

import java.util.Objects;

public class AddOrgan extends PlayVirusCard implements IPrintable {

    public AddOrgan(int deckFrom, int deckTo, int fromIndex, int bodyId) {
        super(deckFrom, deckTo, fromIndex, bodyId);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        getBody(gs).addOrgan((VirusCard)getCard(gs));
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddOrgan)) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Add " + getCard(gameState).toString() + " on body of player " + gameState.getCurrentPlayer();
    }

    @Override
    public void printToConsole() {
        System.out.println("Add organ on body of player");
    }

    @Override
    public AbstractAction copy() {
        return new AddOrgan(deckFrom, deckTo, fromIndex, bodyId);
    }
}
