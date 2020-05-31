package core.actions;

import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;

import java.util.Objects;

@SuppressWarnings("unchecked")
public class RemoveComponentFromDeck<T extends Component> extends DrawCard {
    private int deck;
    private int componentIdx;

    public RemoveComponentFromDeck(int deckFrom, int deckTo, int fromIndex, int deckRemove, int componentRemove) {
        super(deckFrom, deckTo, fromIndex);
        this.deck = deckRemove;
        this.componentIdx = componentRemove;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((Deck<T>)gs.getComponentById(deck)).remove(componentIdx); // card removed from the game

        // Discard other card from player hand
        return super.execute(gs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RemoveComponentFromDeck that = (RemoveComponentFromDeck) o;
        return deck == that.deck &&
                componentIdx == that.componentIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), deck, componentIdx);
    }

    @Override
    public String toString() {
        return "RemoveComponentFromDeck{" +
                "deck=" + deck +
                ", component=" + componentIdx +
                '}';
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "RemoveComponentFromDeck{" +
                "deck=" + deck +
                ", component=" + componentIdx +
                ", cardPlayed=" + getCard(gameState).getComponentName() +
                '}';
    }

    public int getComponentIdx() {
        return componentIdx;
    }

    public int getDeck() {
        return deck;
    }
}
