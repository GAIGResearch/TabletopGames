package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class PlaceKitten extends AbstractAction {

    public final int index;

    public PlaceKitten(int atIndex) {
        this.index = atIndex;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        int player = state.getCurrentPlayer();
        ExplodingKittensCard card = state.getPlayerHand(player).stream()
                .filter(c -> c.cardType == ExplodingKittensCard.CardType.EXPLODING_KITTEN).findFirst()
                .orElseThrow(() -> new AssertionError("No Exploding Kitten found"));
        state.getPlayerHand(player).remove(card);
        boolean[] visibility = new boolean[state.getNPlayers()];
        visibility[state.getCurrentPlayer()] = true;
        state.getDrawPile().add(card, index, visibility);
        state.setSkip(true);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlaceKitten pk && pk.index == index;
    }

    @Override
    public int hashCode() {
        return 3805093 + index;
    }

    @Override
    public String getString(AbstractGameState gameState, int perspectivePlayer) {
        return perspectivePlayer == gameState.getCurrentPlayer() ? toString() : getString(gameState);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Places Exploding Kitten in deck somewhere";
    }

    @Override
    public String toString() {
        return "Place Exploding Kitten at index " + index;
    }
}
