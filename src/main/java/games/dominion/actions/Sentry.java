package games.dominion.actions;

import core.AbstractGameState;
import core.actions.*;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.dominion.DominionGameState;
import games.dominion.cards.*;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static games.dominion.actions.Sentry.Stage.*;

public class Sentry extends DominionAction implements IExtendedSequence {

    enum Stage {playSentry, decisionOne, decisionTwo, reset}

    enum Decision {trash, discard, keep}

    public int CARDS_AFFECTED = 2; // TODO: Changing this will require some serious re-writing of the code given combinatorial effects

    CardType[] topCards = new CardType[CARDS_AFFECTED];
    Stage completedStage = playSentry;
    Decision[] decisions = new Decision[CARDS_AFFECTED];

    public Sentry(int playerId) {
        super(CardType.SENTRY, playerId);
        if (CARDS_AFFECTED != 2) {
            throw new AssertionError("Sentry not yet implemented for changing the number of cards drawn.");
        }
    }
    public Sentry(int playerId, boolean dummy) {
        super(CardType.SENTRY, playerId, dummy);
        if (CARDS_AFFECTED != 2) {
            throw new AssertionError("Sentry not yet implemented for changing the number of cards drawn.");
        }
    }

    @Override
    boolean _execute(DominionGameState state) {
        for (int i = 0; i < CARDS_AFFECTED; i++)
            state.drawCard(player);
        Deck<DominionCard> hand = state.getDeck(DeckType.HAND, player);
        for (int i = 0; i < CARDS_AFFECTED; i++) {
            if (hand.getSize() > i)
                topCards[i] = hand.peek(i).cardType();
        }
        // we draw two cards into hand, and then keep track of them
        // storing them in hand makes them visible to a human player, and we can DISCARD and TRASH from hand easily
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> retValue = new ArrayList<>(CARDS_AFFECTED + 1);
        switch (completedStage) {
            case playSentry:
                if (topCards[0] != null) {
                    retValue.add(new TrashCard(topCards[0], player));
                    retValue.add(new DiscardCard(topCards[0], player));
                }
                retValue.add(new DoNothing());
                return retValue;
            case decisionOne:
                if (topCards[1] != null) {
                    retValue.add(new TrashCard(topCards[1], player));
                    retValue.add(new DiscardCard(topCards[1], player));
                }
                retValue.add(new DoNothing());
                return retValue;
            case decisionTwo:
                // we should only be here if we keep both cards - otherwise there is no decision to be made
                retValue.add(
                        new CompositeAction(
                                new MoveCard(topCards[0], player, DeckType.HAND, player, DeckType.DRAW, false),
                                new MoveCard(topCards[1], player, DeckType.HAND, player, DeckType.DRAW, false)
                        )
                );
                retValue.add(
                        new CompositeAction(
                                new MoveCard(topCards[1], player, DeckType.HAND, player, DeckType.DRAW, false),
                                new MoveCard(topCards[0], player, DeckType.HAND, player, DeckType.DRAW, false)
                        )
                );
                return retValue;
            case reset:
                throw new AssertionError("No follow on actions after reset");
        }
        return retValue;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        Decision decision = null;
        if (action instanceof TrashCard && ((TrashCard) action).player == player)
            decision = Decision.trash;
        if (action instanceof DiscardCard && ((DiscardCard) action).player == player)
            decision = Decision.discard;
        if (action instanceof DoNothing)
            decision = Decision.keep;

        if (decision != null) {
            if (decisions[0] == null) {
                if (topCards[0] == null)
                    decision = Decision.trash;
                decisions[0] = decision;
                completedStage = decisionOne;
            } else {
                if (topCards[1] == null)
                    decision = Decision.trash;
                decisions[1] = decision;
                completedStage = decisionTwo;
                // and now we check for the need for a further decision
                if (decisions[0] == Decision.keep && decisions[1] == Decision.keep && topCards[0] != topCards[1]) {
                    // we are keeping two non-identical cards, so have to make decision
                } else {
                    for (int i = 0; i < 2; i++) {
                        if (decisions[i] == Decision.keep) {
                            (new MoveCard(topCards[i], player, DeckType.HAND, player, DeckType.DRAW, false)).execute(state);
                        }
                    }
                    completedStage = reset;
                }
            }
        }

        if (action instanceof CompositeAction) {
            completedStage = reset;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return completedStage == reset;
    }

    @Override
    public Sentry copy() {
        Sentry retValue = new Sentry(player, dummyAction);
        retValue.completedStage = completedStage;
        for (int i = 0; i < 2; i++) {
            retValue.decisions[i] = decisions[i];
            retValue.topCards[i] = topCards[i];
        }
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Sentry) {
            Sentry other = (Sentry) obj;
            return super.equals(obj) && other.completedStage == completedStage && other.topCards[0] == topCards[0] && other.topCards[1] == topCards[1] &&
                    other.decisions[0] == decisions[0] && other.decisions[1] == decisions[1];
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 89 * super.hashCode() + Objects.hash(topCards[0], topCards[1], decisions[0], decisions[1], completedStage);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Sentry [%s -> %s, %s -> %s] ", topCards[0], decisions[0], topCards[1], decisions[1]);
    }
}
