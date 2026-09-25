package games.toads.actions;

import core.actions.AbstractAction;
import core.actions.OneShotExtendedAction;
import games.toads.ToadConstants.ToadCardType;
import games.toads.ToadGameState;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The decision of a Siege Cannon's owner: which card to guess in the opponent's hand.
 */
public class SiegeCannonGuess extends OneShotExtendedAction {

    public SiegeCannonGuess(int player) {
        super("Siege Cannon guess for player " + player, player, state -> guesses((ToadGameState) state, player));
    }

    private static List<AbstractAction> guesses(ToadGameState state, int player) {
        // a type is ruled out if it is the guesser's own Casualty or already in the opponent's discards
        Predicate<ToadCardType> ruledOut = t ->
                (state.getTieBreaker(player) != null && state.getTieBreaker(player).type == t) ||
                        state.getDiscards(1 - player).stream().anyMatch(c -> c.type == t);
        // one guess per printed name, unless every type with that name is ruled out
        List<AbstractAction> retValue = state.getCardTypesInPlay().stream()
                .filter(t -> !ruledOut.test(t))
                .map(ToadCardType::guessGroup)
                .distinct()
                .sorted()
                .map(t -> (AbstractAction) new GuessCard(t))
                .collect(Collectors.toList());
        if (retValue.isEmpty())
            retValue.add(new GuessCard(ToadCardType.NONE_OF_THESE));
        return retValue;
    }

    @Override
    public SiegeCannonGuess copy() {
        SiegeCannonGuess retValue = new SiegeCannonGuess(player);
        retValue.executed = executed;
        return retValue;
    }
}
