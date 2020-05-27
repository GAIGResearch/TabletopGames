package games.uno;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.interfaces.IPrintable;

import java.util.Objects;

public class PlayCard extends DrawCard implements IPrintable {

    private final AbstractAction postEffect;

    public PlayCard(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
        this.postEffect = null;
    }

    public PlayCard(int deckFrom, int deckTo, int fromIndex, AbstractAction postEffect) {
        super(deckFrom, deckTo, fromIndex);
        this.postEffect = postEffect;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        if (postEffect != null) postEffect.execute(gs);
        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println("Play card");
    }

    @Override
    public String toString() {
        return "PlayCard{" +
                "postEffect=" + (postEffect != null? postEffect.toString() : null) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PlayCard playCard = (PlayCard) o;
        return Objects.equals(postEffect, playCard.postEffect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), postEffect);
    }
}
