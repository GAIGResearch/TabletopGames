package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.List;
import java.util.Objects;

import static games.explodingkittens.cards.ExplodingKittensCard.CardType.NOPE;


public class NopeableAction implements IExtendedSequence {

    int currentInterrupter = -1;
    int lastCardPlayedBy;
    PlayEKCard originalAction;
    int nopes = 0;

    public NopeableAction(int player, PlayEKCard action, ExplodingKittensGameState state) {
        this.lastCardPlayedBy = player;
        this.originalAction = action.copy();
        setNextInterrupter(state);
    }


    // private constructor for copying
    private NopeableAction(int lastCardPlayedBy, PlayEKCard originalAction, int currentInterrupter, int nopes) {
        this.lastCardPlayedBy = lastCardPlayedBy;
        this.originalAction = originalAction.copy();
        this.currentInterrupter = currentInterrupter;
        this.nopes = nopes;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        Deck<ExplodingKittensCard> hand = state.getPlayerHand(currentInterrupter);
        if (hand.stream().anyMatch(c -> c.cardType == NOPE)) {
            return List.of(new Pass(), new Nope());
        }
        throw new AssertionError(currentInterrupter + " does not have a NOPE card to play");
    }


    private void setNextInterrupter(ExplodingKittensGameState state) {
        if (currentInterrupter == -1) { // initialisation
            currentInterrupter = (lastCardPlayedBy + 1) % state.getNPlayers();
        } else if (currentInterrupter != lastCardPlayedBy) {
            currentInterrupter = (currentInterrupter + 1) % state.getNPlayers();
        }
        // Now we see if anyone has a Nope card
        while (currentInterrupter != lastCardPlayedBy) {
            if (state.isNotTerminalForPlayer(currentInterrupter)) {
                Deck<ExplodingKittensCard> hand = state.getPlayerHand(currentInterrupter);
                boolean hasNope = hand.stream().anyMatch(c -> c.cardType == NOPE);
                if (hasNope) break;
            }
            currentInterrupter = (currentInterrupter + 1) % state.getNPlayers();
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return currentInterrupter;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        if (action instanceof Nope) {
            nopes++;
            // this resets the count
            lastCardPlayedBy = currentInterrupter;
            currentInterrupter = -1;
            setNextInterrupter(state);
        }
        if (action instanceof Pass) {
            setNextInterrupter(state);
        }
        if (executionComplete(state)) {
            // we have gone round the table
            if (nopes % 2 == 0) {
                // no one noped the action; execute the action
                originalAction.cardType.execute(state, originalAction.target);
            }
            state.getInPlay().forEach(c -> state.getDiscardPile().add(c));
            state.getInPlay().clear();
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return currentInterrupter == lastCardPlayedBy;  // we have gone round the table
    }

    @Override
    public IExtendedSequence copy() {
        return new NopeableAction(lastCardPlayedBy, originalAction, currentInterrupter, nopes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NopeableAction other)) return false;
        return lastCardPlayedBy == other.lastCardPlayedBy &&
                currentInterrupter == other.currentInterrupter &&
                nopes == other.nopes &&
                originalAction.equals(other.originalAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastCardPlayedBy, currentInterrupter, nopes, originalAction);
    }
}
