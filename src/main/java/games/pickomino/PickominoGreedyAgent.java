package games.pickomino;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.pickomino.actions.NullTurn;
import games.pickomino.actions.SelectDicesAction;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Greedy policy for Pickomino with the following behaviour:
 * - Play {@link NullTurn} when it is the only option.
 * - Stop as soon as a stop action is available, prioritising steals, then best-scoring tiles.
 * - If stopping is not possible, prefer worms (face value 6) when two or more are available and no worm was taken yet.
 * - Otherwise pick the selectable dice face that maximises the score increment.
 */
public class PickominoGreedyAgent extends AbstractPlayer {

    public PickominoGreedyAgent() {
        this(new Random());
    }

    public PickominoGreedyAgent(Random rnd) {
        super(null, "PickominoGreedyAgent");
        this.rnd = rnd;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> actions) {
        if (actions.size() == 1) {
            // only one action available, return it (null turn)
            return actions.get(0);
        }

        PickominoGameState state = (PickominoGameState) observation;

        // Separate stop and continue actions for easier reasoning
        List<SelectDicesAction> stopActions = actions.stream()
                .filter(a -> a instanceof SelectDicesAction sda && sda.isStop())
                .map(a -> (SelectDicesAction) a)
                .collect(Collectors.toList());

        // Stop as soon as possible, using prioritised selection
        if (!stopActions.isEmpty()) {
            return chooseStopAction(state, stopActions);
        }

        // Continue actions are the actions that are not stop actions
        List<SelectDicesAction> continueActions = actions.stream()
        .filter(a -> a instanceof SelectDicesAction sda && !sda.isStop())
        .map(a -> (SelectDicesAction) a)
        .collect(Collectors.toList());

        // If we cannot stop and are not forced into NullTurn, follow the dice-pick policy
        SelectDicesAction dicePick = chooseContinueAction(state, continueActions);
        if (dicePick != null) {
            return dicePick;
        }

        throw new AssertionError("Something went wrong, no action was chosen");
    }

    private SelectDicesAction chooseStopAction(PickominoGameState state, List<SelectDicesAction> stopActions) {
        SelectDicesAction bestSteal = null;
        int bestStealValue = -1;

        // First priority: steal a tile, preferring the highest-valued steal
        for (SelectDicesAction action : stopActions) {
            int totalAfter = projectedTotal(state, action);
            int stealValue = highestStealableTileValue(state, totalAfter);
            if (stealValue >= 0 && stealValue > bestStealValue) {
                bestStealValue = stealValue;
                bestSteal = action;
            }
        }
        if (bestSteal != null) return bestSteal;

        // Second priority: pick the tile with the highest score, using the lowest possible tile value as a tie-breaker
        Comparator<PickominoTile> tileComparator = Comparator
                .comparingInt(PickominoTile::getScore).reversed()
                .thenComparingInt(PickominoTile::getValue);

        SelectDicesAction bestStop = null;
        PickominoTile bestTile = null;
        for (SelectDicesAction action : stopActions) {
            PickominoTile candidate = bestTileForTotal(state, projectedTotal(state, action));
            if (candidate == null) continue;
            if (bestTile == null || tileComparator.compare(candidate, bestTile) < 0) {
                // comparator sorts descending by score, then ascending by value; we want 'better' => compare < 0
                bestTile = candidate;
                bestStop = action;
            }
        }
        if (bestStop != null) return bestStop;

        // If something unexpected happens, default to the first stop action
        return stopActions.get(0);
    }

    private SelectDicesAction chooseContinueAction(PickominoGameState state, List<SelectDicesAction> continueActions) {
        if (continueActions.isEmpty()) return null;

        // Prefer worms when there are at least 2 available and none assigned yet
        if (state.assignedDices[5] == 0 && state.currentRoll[5] >= 2) {
            for (SelectDicesAction action : continueActions) {
                if (action.getDiceValue() == 6) return action;
            }
        }

        // Otherwise pick the face that maximises score increment; tie-break on higher face value for determinism
        return continueActions.stream()
                .max(Comparator.<SelectDicesAction>comparingInt(a -> valueIncrement(a.getDiceValue(), state.currentRoll[a.getDiceValue() - 1]))
                        .thenComparingInt(SelectDicesAction::getDiceValue))
                .orElse(null);
    }

    private int projectedTotal(PickominoGameState state, SelectDicesAction action) {
        int diceValue = action.getDiceValue();
        int count = state.currentRoll[diceValue - 1];
        return state.totalDicesValue + valueIncrement(diceValue, count);
    }

    private int highestStealableTileValue(PickominoGameState state, int totalAfterSelection) {
        int best = -1;
        int currentPlayer = state.getCurrentPlayer();
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (p == currentPlayer) continue;
            PickominoTile top = state.playerTiles.get(p).peek();
            if (top != null && top.getValue() == totalAfterSelection) {
                if (top.getValue() > best) best = top.getValue();
            }
        }
        return best;
    }

    private PickominoTile bestTileForTotal(PickominoGameState state, int totalAfterSelection) {
        PickominoTile best = null;
        for (int i = 0; i < state.remainingTiles.getSize(); i++) {
            PickominoTile tile = state.remainingTiles.peek(i);
            if (tile.getValue() <= totalAfterSelection) {
                if (best == null
                        || tile.getScore() > best.getScore()
                        || (tile.getScore() == best.getScore() && tile.getValue() < best.getValue())) {
                    best = tile;
                }
            }
        }
        return best;
    }

    private int valueIncrement(int diceValue, int count) {
        return (diceValue == 6 ? 5 : diceValue) * count;
    }

    @Override
    public PickominoGreedyAgent copy() {
        PickominoGreedyAgent copy = new PickominoGreedyAgent(new Random(rnd.nextInt()));
        copy.decorators = decorators;
        copy.setName(this.toString());
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PickominoGreedyAgent;
    }
}

