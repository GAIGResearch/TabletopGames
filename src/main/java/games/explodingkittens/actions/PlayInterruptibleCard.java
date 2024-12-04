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

public class PlayInterruptibleCard extends AbstractAction implements IExtendedSequence {

    public final ExplodingKittensCard.CardType cardType;
    public final int cardPlayer;
    int currentInterrupter = -1;

    public PlayInterruptibleCard(ExplodingKittensCard.CardType cardType, int playerID) {
        this.cardType = cardType;
        this.cardPlayer = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        ExplodingKittensCard card = state.getPlayerHand(cardPlayer).stream()
                .filter(c -> c.cardType == cardType).findFirst()
                .orElseThrow(() -> new AssertionError("Player " + cardPlayer + " does not have a " + cardType + " card to play"));
        state.setInPlay(card, cardPlayer);

        if (cardType.nopeable) {
            // NOPEs always go on the stack so that we count them correctly in the playAndDiscardCard method
            if (cardType == NOPE)
                state.setActionInProgress(this);
            setNextInterrupter(state);
            // otherwise we only something on the stack if we need to check for NOPEs
            if (cardType != NOPE && currentInterrupter != cardPlayer) {
                state.setActionInProgress(this);
            }
        } else {
            currentInterrupter = cardPlayer;  // skip the nope check
        }
        // Having done all that, we now play the card if nothing is on the queue
        // otherwise this will be done in afterAction
        if (state.getActionsInProgress().isEmpty()) {
            playAndDiscardCard(state);
        }
        return true;
    }

    public void _execute(ExplodingKittensGameState state) {
        // default is to do nothing
        // Override this in sub-classes
    }

    private void playAndDiscardCard(ExplodingKittensGameState state) {
        // specific to exploding kittens we may have a sequence of NOPE cards on the stack, with
        // the first one being a non-NOPE card (that we may or may not execute)

        // we only call this method on the top of the stack, so we need to go down the stack
        // discarding each card

        int nopeCards = (int) state.getActionsInProgress().stream()
                .filter(a -> a instanceof PlayInterruptibleCard pic && pic.cardType == NOPE)
                .count();
        if (nopeCards % 2 == 0) {
            // get the actual action to execute.
            // this is *this* if we had no interruptions; otherwise it is the only non-NOPE on the stack
            List<PlayInterruptibleCard> originalAction = state.getActionsInProgress().isEmpty()
                    ? List.of(this)
                    : state.getActionsInProgress().stream()
                    .filter(a -> a instanceof PlayInterruptibleCard pic && pic.cardType != NOPE)
                    .map(a -> (PlayInterruptibleCard) a)
                    .toList();
            if (originalAction.size() != 1) {
                throw new AssertionError("Expected 1 original action, but found " + originalAction.size());
            }
//            state.getActionsInProgress().stream().filter(a -> a instanceof PlayInterruptibleCard)
//                    .forEach(a -> {
//                        PlayInterruptibleCard pic = (PlayInterruptibleCard) a;
//                        pic.currentInterrupter = pic.cardPlayer;  // mark all as done
//                    });

            // Now we execute it
            originalAction.get(0)._execute(state);
        }

        // And whether we execute or not, put everything inPlay into Discard
        state.getInPlay().forEach(c -> state.getDiscardPile().add(c));
        state.getInPlay().clear();
    }

    private void setNextInterrupter(ExplodingKittensGameState state) {
        if (currentInterrupter == -1) { // initialisation
            currentInterrupter = (cardPlayer + 1) % state.getNPlayers();
        } else if (currentInterrupter != cardPlayer) {
            currentInterrupter = (currentInterrupter + 1) % state.getNPlayers();
        }
        // Now we see if anyone has a Nope card
        while (currentInterrupter != cardPlayer) {
            Deck<ExplodingKittensCard> hand = state.getPlayerHand(currentInterrupter);
            boolean hasNope = hand.stream().anyMatch(c -> c.cardType == NOPE);
            if (hasNope) break;
            currentInterrupter = (currentInterrupter + 1) % state.getNPlayers();
        }
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        Deck<ExplodingKittensCard> hand = state.getPlayerHand(currentInterrupter);
        if (hand.stream().anyMatch(c -> c.cardType == NOPE)) {
            return List.of(new Pass(), new PlayInterruptibleCard(NOPE, currentInterrupter));
        }
        throw new AssertionError(currentInterrupter + " does not have a NOPE card to play");
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return currentInterrupter;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        // either the action is a Pass action or playing a NOPE card
        // anything else is unexpected
        if (action instanceof Pass) {
            // we have passed, so we move to the next player, and if back to the start then the card triggers
            setNextInterrupter(state);
            if (currentInterrupter == cardPlayer) {  // no-one left who can NOPE this action, so the sequence is complete
                playAndDiscardCard(state);
            }
        } else if (action instanceof PlayInterruptibleCard pic && pic.cardType == NOPE) {
            // this card has been Noped; we record the link but only check this once we've completed the whole sequence in case the Nope is Noped
            currentInterrupter = cardPlayer;
            if (pic.currentInterrupter == pic.cardPlayer) {
                // This has been Noped, but no-one can Nope the Nope that Noped us. So the sequence is complete.
                playAndDiscardCard(state);
            }
        } else {
            throw new AssertionError("Unexpected action: " + action);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return currentInterrupter == cardPlayer;  // we have gone round the table
    }

    @Override
    public PlayInterruptibleCard copy() {
        PlayInterruptibleCard retValue = _copy();
        retValue.currentInterrupter = currentInterrupter;
        return retValue;
    }

    public PlayInterruptibleCard _copy() {
        return new PlayInterruptibleCard(cardType, cardPlayer);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlayInterruptibleCard pic) {
            return _equals(obj) && pic.cardType == cardType &&
                    pic.cardPlayer == cardPlayer &&
                    pic.currentInterrupter == currentInterrupter;
        }
        return false;
    }

    public boolean _equals(Object obj) {
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardType.ordinal(), cardPlayer, currentInterrupter, _hashCode());
    }

    public int _hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Plays " + cardType;
    }
}
