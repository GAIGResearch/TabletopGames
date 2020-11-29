package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.*;
import games.dominion.DominionConstants.*;
import games.dominion.cards.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Militia extends DominionAction implements IExtendedSequence {

    public Militia(int playerId) {
        super(CardType.MILITIA, playerId);
    }

    int currentTarget;
    boolean[] reactionComplete;

    @Override
    boolean _execute(DominionGameState state) {
        state.spend(-2); // player gets +2 to spend
        state.setActionInProgress(this);
        currentTarget = (player + 1) % state.getNPlayers();
        reactionComplete = new boolean[state.getNPlayers()];
        checkCurrentTarget(state);
        return true;
    }

    private void checkCurrentTarget(DominionGameState state) {
        int prospectiveDiscardPlayer = currentTarget;
        do {
            if (!state.isDefended(prospectiveDiscardPlayer) &&
                    state.getDeck(DominionConstants.DeckType.HAND, prospectiveDiscardPlayer).getSize() > 3) {
                currentTarget = prospectiveDiscardPlayer;
                if (!reactionComplete[currentTarget])
                    state.setActionInProgress(new AttackReaction(state, player, currentTarget));
                reactionComplete[currentTarget] = true;
                return;
            }
            prospectiveDiscardPlayer = (prospectiveDiscardPlayer + 1) % state.getNPlayers();
        } while (prospectiveDiscardPlayer != player);
        currentTarget = player; // we're done
    }

    @Override
    public List<AbstractAction> followOnActions(DominionGameState state) {
        // we can discard any card in hand, so create a DiscardCard action for each
        if (state.getDeck(DeckType.HAND, currentTarget).getSize() < 4 || state.isDefended(currentTarget))
            throw new AssertionError("Should not be here - there are no actions to be taken");
        Set<DominionCard> uniqueCardsInHand = state.getDeck(DeckType.HAND, currentTarget).stream().collect(toSet());
        return uniqueCardsInHand.stream()
                .map(card -> new DiscardCard(card.cardType(), currentTarget))
                .distinct()
                .collect(toList());
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        checkCurrentTarget(state);
        return currentTarget;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        checkCurrentTarget(state);
    }

    @Override
    public boolean executionComplete(DominionGameState state) {
        checkCurrentTarget(state);
        return currentTarget == player;
    }

    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     *
     * @return - new AbstractAction object with the same properties.
     */
    @Override
    public Militia copy() {
        Militia retValue = new Militia(player);
        retValue.currentTarget = currentTarget;
        retValue.reactionComplete = reactionComplete != null ? reactionComplete.clone() : null;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Militia) {
            Militia other = (Militia) obj;
            return other.player == player && other.currentTarget == currentTarget
                    && Arrays.equals(reactionComplete, other.reactionComplete);
        }
        return false;
    }
}
