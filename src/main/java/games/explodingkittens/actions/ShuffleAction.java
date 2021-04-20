package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.Arrays;
import java.util.Random;

public class ShuffleAction extends DrawCard implements IsNopeable, IPrintable {

    public ShuffleAction(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((ExplodingKittensGameState)gs).getDrawPile().shuffle(new Random(gs.getGameParameters().getRandomSeed()));
        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Player shuffles the draw pile";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Shuffle draw deck";
    }

    @Override
    public void nopedExecute(AbstractGameState gs) {
        super.execute(gs);
    }

    @Override
    public void actionPlayed(AbstractGameState gs) {
        // Mark card as visible in the player's deck to all other players
        PartialObservableDeck<ExplodingKittensCard> from = (PartialObservableDeck<ExplodingKittensCard>) gs.getComponentById(deckFrom);
        boolean[] vis = new boolean[gs.getNPlayers()];
        Arrays.fill(vis, true);
        from.setVisibilityOfComponent(fromIndex, vis);
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public AbstractAction copy() {
        return new ShuffleAction(deckFrom, deckTo, fromIndex);
    }
}
