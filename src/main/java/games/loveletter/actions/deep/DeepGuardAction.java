package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.PlayCard;
import games.loveletter.cards.CardType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The guard allows to attempt guessing another player's card. If the guess is correct, the targeted opponent
 * is removed from the game.
 */
public class DeepGuardAction extends PlayCardDeep implements IExtendedSequence, IPrintable {
    enum Step {
        TargetPlayer,
        CardType,
        Done
    }
    private int targetPlayer;
    private Step step;

    public DeepGuardAction(int cardIdx, int playerID) {
        super(CardType.Guard, cardIdx, playerID);
        step = Step.TargetPlayer;
        targetPlayer = -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeepGuardAction)) return false;
        if (!super.equals(o)) return false;
        DeepGuardAction that = (DeepGuardAction) o;
        return targetPlayer == that.targetPlayer && step == that.step;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetPlayer, step);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> cardActions = new ArrayList<>();
        LoveLetterGameState gs = (LoveLetterGameState) state;
        if (step == Step.TargetPlayer) {
            // Actions to select player
            for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
                if (targetPlayer == playerID || !gs.isCurrentlyActive(targetPlayer) || gs.isProtected(targetPlayer))
                    continue;
                cardActions.add(new ChoosePlayer(targetPlayer));
            }
            // If no player can be targeted, create an effectively do-nothing action
            if (cardActions.size() == 0) cardActions.add(new ChoosePlayer(-1));
        } else {
            // Complete actions
            cardActions.addAll(CardType.Guard.flatActions(gs, new PlayCard(cardIdx, playerID, false, targetPlayer)));
        }
        return cardActions;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (step == Step.TargetPlayer) {
            targetPlayer = ((ChoosePlayer)action).player;
            if (targetPlayer == -1) step = Step.Done;
            else step = Step.CardType;
        }
        else step = Step.Done;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return step == Step.Done;
    }

    @Override
    public DeepGuardAction copy() {
        DeepGuardAction copy = new DeepGuardAction(cardIdx, playerID);
        copy.step = step;
        copy.targetPlayer = targetPlayer;
        return copy;
    }
}
