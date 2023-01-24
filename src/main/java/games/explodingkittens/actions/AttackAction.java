package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.ExplodingKittensTurnOrder;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.Arrays;
import java.util.Objects;


public class AttackAction extends DrawCard implements IsNopeable, IPrintable {
    int attackTargetID;

    public AttackAction(int deckFrom, int deckTo, int index, int attackTargetID) {
        super(deckFrom, deckTo, index);
        this.attackTargetID = attackTargetID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Discard card played
        super.execute(gs);
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        // Execute action
        ((ExplodingKittensTurnOrder) ekgs.getTurnOrder()).registerAttackAction(attackTargetID);
        return false;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("Attack player %d", attackTargetID);
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player attacks player %d", attackTargetID);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttackAction)) return false;
        if (!super.equals(o)) return false;
        AttackAction that = (AttackAction) o;
        return attackTargetID == that.attackTargetID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackTargetID);
    }

    @Override
    public AbstractAction copy() {
        return new AttackAction(deckFrom, deckTo, fromIndex, attackTargetID);
    }
}
