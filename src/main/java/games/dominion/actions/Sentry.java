package games.dominion.actions;

import core.AbstractGameState;
import core.actions.*;
import core.components.Deck;
import games.dominion.DominionGameState;
import games.dominion.cards.*;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static games.dominion.actions.Sentry.Stage.*;

public class Sentry extends DominionAction implements IExtendedSequence {

    enum Stage {playSentry, decisionOne, decisionTwo, reset}

    enum Decision {trash, discard, keep}

    CardType[] topTwo = new CardType[2];
    Stage completedStage = playSentry;
    Decision[] decisions = new Decision[2];

    public Sentry(int playerId) {
        super(CardType.SENTRY, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.changeActions(1);
        state.drawCard(player);
        state.drawCard(player);
        state.drawCard(player);
        Deck<DominionCard> hand = state.getDeck(DeckType.HAND, player);
        if (hand.getSize() > 1)
            topTwo[0] = hand.peek(1).cardType();
        if (hand.getSize() > 1)
            topTwo[1] = hand.peek(0).cardType();
        // we draw two cards into hand, and then keep track of them
        // string them in hand makes them visible to a human player, and we can DISCARD and TRASH from hand easily
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> followOnActions(DominionGameState state) {
        List<AbstractAction> retValue = new ArrayList<>(3);
        switch (completedStage) {
            case playSentry:
                if (topTwo[0] != null) {
                    retValue.add(new TrashCard(topTwo[0], player));
                    retValue.add(new DiscardCard(topTwo[0], player));
                }
                retValue.add(new DoNothing());
                return retValue;
            case decisionOne:
                if (topTwo[1] != null) {
                    retValue.add(new TrashCard(topTwo[1], player));
                    retValue.add(new DiscardCard(topTwo[1], player));
                }
                retValue.add(new DoNothing());
                return retValue;
            case decisionTwo:
                // we should only be here if we keep both cards - otherwise there is no decision to be made
                retValue.add(
                        new CompositeAction(
                                new MoveCard(topTwo[0], player, DeckType.HAND, player, DeckType.DRAW, false),
                                new MoveCard(topTwo[1], player, DeckType.HAND, player, DeckType.DRAW, false)
                        )
                );
                retValue.add(
                        new CompositeAction(
                                new MoveCard(topTwo[1], player, DeckType.HAND, player, DeckType.DRAW, false),
                                new MoveCard(topTwo[0], player, DeckType.HAND, player, DeckType.DRAW, false)
                        )
                );
                return retValue;
            case reset:
                throw new AssertionError("No follow on actions after reset");
        }
        return retValue;
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        Decision decision = null;
        if (action instanceof TrashCard && ((TrashCard) action).player == player)
            decision = Decision.trash;
        if (action instanceof DiscardCard && ((DiscardCard) action).player == player)
            decision = Decision.discard;
        if (action instanceof DoNothing)
            decision = Decision.keep;

        if (decision != null) {
            if (decisions[0] == null) {
                if (topTwo[0] == null)
                    decision = Decision.trash;
                decisions[0] = decision;
                completedStage = decisionOne;
            } else {
                if (topTwo[1] == null)
                    decision = Decision.trash;
                decisions[1] = decision;
                completedStage = decisionTwo;
                // and now we check for the need for a further decision
                if (decisions[0] == Decision.keep && decisions[1] == Decision.keep && topTwo[0] != topTwo[1]) {
                    // we are keeping two non-identical cards, so have to make decision
                } else {
                    for (int i = 0; i < 2; i++) {
                        if (decisions[i] == Decision.keep) {
                            (new MoveCard(topTwo[i], player, DeckType.HAND, player, DeckType.DRAW, false)).execute(state);
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
    public boolean executionComplete(DominionGameState state) {
        return completedStage == reset;
    }

    @Override
    public Sentry copy() {
        Sentry retValue = new Sentry(player);
        retValue.completedStage = completedStage;
        for (int i = 0; i < 2; i++) {
            retValue.decisions[i] = decisions[i];
            retValue.topTwo[i] = topTwo[i];
        }
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Sentry) {
            Sentry other = (Sentry) obj;
            return other.completedStage == completedStage && other.topTwo[0] == topTwo[0] && other.topTwo[1] == topTwo[1] &&
                    other.decisions[0] == decisions[0] && other.decisions[1] == decisions[1];
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 89 * super.hashCode() + Objects.hash(topTwo[0], topTwo[1], decisions[0], decisions[1], completedStage);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Sentry [%s -> %s, %s -> %s] ", topTwo[0], decisions[0], topTwo[1], decisions[1]);
    }
}
