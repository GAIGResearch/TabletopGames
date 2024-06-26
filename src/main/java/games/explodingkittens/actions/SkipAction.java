package games.explodingkittens.actions;

import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.ExplodingKittensTurnOrder;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.Arrays;

public class SkipAction extends DrawCard implements IsNopeable, IPrintable {

    public SkipAction(int deckFrom, int deckTo, int index) {
        super(deckFrom, deckTo, index);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Discard the card played
        super.execute(gs);
        // Execute action
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        gs.setGamePhase(CoreConstants.DefaultGamePhase.Main);
        ((ExplodingKittensTurnOrder)ekgs.getTurnOrder()).endPlayerTurnStep(gs);
        return true;
    }

    @Override
    public String toString(){
        return "Player skips its draw";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Skip";
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
        return new SkipAction(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SkipAction)) return false;
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 190;
    }
}
