package players.muditbot;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import players.PlayerConstants;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static games.sushigo.cards.SGCard.SGCardType.*;

/**
 * A BasicMCTSPlayer variant that evaluates Sushi Go drafts using card synergies and score position.
 */
public class MuditBot extends BasicMCTSPlayer {

    public MuditBot() {
        this(System.currentTimeMillis());
    }

    public MuditBot(long seed) {
        super(seed);
        BasicMCTSParams params = getParameters();
        // Keep the search within a short per-decision budget while extending rollouts
        // far enough to estimate the value of the current Sushi Go round.
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_TIME);
        params.setParameterValue("budget", 50);
        params.setParameterValue("rolloutLength", 100);
        params.setParameterValue("maxTreeDepth", 5);
        params.setParameterValue("heuristic", (IStateHeuristic) MuditBot::evaluateState);
        params.budgetType = PlayerConstants.BUDGET_TIME;
        params.budget = 50;
        params.rolloutLength = 100;
        params.maxTreeDepth = 5;
        params.heuristic = MuditBot::evaluateState;
        setName("MuditBot");
    }

    public MuditBot(BasicMCTSParams params) {
        super(params);
        getParameters().setParameterValue("heuristic", (IStateHeuristic) MuditBot::evaluateState);
        getParameters().heuristic = MuditBot::evaluateState;
        setName("MuditBot");
    }

    static double evaluateState(AbstractGameState gameState, int playerId) {
        return evaluateDraftState(gameState, playerId, true);
    }

    static double evaluateCardPickingState(AbstractGameState gameState, int playerId) {
        return evaluateDraftState(gameState, playerId, false);
    }

    private static double evaluateDraftState(AbstractGameState gameState, int playerId,
                                             boolean useFullDeckAvailability) {
        if (!(gameState instanceof SGGameState state)) {
            return gameState.getHeuristicScore(playerId);
        }
        if (!state.isNotTerminal()) {
            return state.getHeuristicScore(playerId);
        }

        SGParameters parameters = (SGParameters) state.getGameParameters();
        // The baseline bot uses full-deck frequencies. The card-picking variant can
        // instead condition its estimates on public cards and hands known to this player.
        CardAvailability availability = useFullDeckAvailability
                ? new CardAvailability(parameters)
                : estimateAvailability(state, playerId);
        int nPlayers = state.getNPlayers();
        int[] remainingPicks = new int[nPlayers];
        List<Map<SGCard.SGCardType, Integer>> counts = new ArrayList<>(nPlayers);
        double[] projectedScores = new double[nPlayers];

        for (int player = 0; player < nPlayers; player++) {
            // Choices are made simultaneously, so account for cards already chosen
            // this turn as well as future picks from each player's remaining hand.
            remainingPicks[player] = Math.max(0,
                    state.getPlayerHands().get(player).getSize() - state.getCardChoices().get(player).size());
            counts.add(getEffectiveCounts(state, player));
        }
        for (int player = 0; player < nPlayers; player++) {
            projectedScores[player] = state.getGameScore(player)
                    + pendingCardPoints(state, parameters, player)
                    + expectedCardPoints(parameters, counts.get(player), remainingPicks[player], availability)
                    + expectedPuddingPoints(state, parameters, player, remainingPicks, availability, 0);
        }

        addExpectedMakiPoints(state, parameters, counts, remainingPicks, projectedScores, availability);

        // Convert projected score differences into bounded win-probability estimates;
        // uncertainty grows with the number of picks and rounds left to play.
        double uncertainty = 5.0 + Math.max(0, parameters.nRounds - state.getRoundCounter() - 1) * 3.0;
        for (int picks : remainingPicks) {
            uncertainty += picks * 0.15;
        }

        double total = 0.0;
        for (int opponent = 0; opponent < nPlayers; opponent++) {
            if (opponent != playerId) {
                total += logistic((projectedScores[playerId] - projectedScores[opponent]) / uncertainty);
            }
        }
        return total / (nPlayers - 1);
    }

    private static Map<SGCard.SGCardType, Integer> getEffectiveCounts(SGGameState state, int player) {
        Map<SGCard.SGCardType, Integer> counts = new EnumMap<>(SGCard.SGCardType.class);
        for (SGCard.SGCardType type : SGCard.SGCardType.values()) {
            counts.put(type, state.getPlayedCardTypes()[player].get(type).getValue());
        }
        for (ChooseCard pending : state.getCardChoices().get(player)) {
            SGCard selected = (SGCard) pending.getCard(state);
            counts.merge(selected.type, selected.count, Integer::sum);
            if (isNigiri(selected.type) && counts.get(Wasabi) > 0) {
                counts.merge(Wasabi, -1, Integer::sum);
            }
        }
        return counts;
    }

    private static double pendingCardPoints(SGGameState state, SGParameters parameters, int player) {
        Map<SGCard.SGCardType, Integer> counts = new EnumMap<>(SGCard.SGCardType.class);
        for (SGCard.SGCardType type : SGCard.SGCardType.values()) {
            counts.put(type, state.getPlayedCardTypes()[player].get(type).getValue());
        }

        double points = 0.0;
        for (ChooseCard pending : state.getCardChoices().get(player)) {
            SGCard card = (SGCard) pending.getCard(state);
            switch (card.type) {
                case EggNigiri -> points += parameters.valueEggNigiri
                        * (counts.get(Wasabi) > 0 ? parameters.multiplierWasabi : 1);
                case SalmonNigiri -> points += parameters.valueSalmonNigiri
                        * (counts.get(Wasabi) > 0 ? parameters.multiplierWasabi : 1);
                case SquidNigiri -> points += parameters.valueSquidNigiri
                        * (counts.get(Wasabi) > 0 ? parameters.multiplierWasabi : 1);
                case Tempura -> {
                    if ((counts.get(Tempura) + card.count) / 2 > counts.get(Tempura) / 2) {
                        points += parameters.valueTempuraPair;
                    }
                }
                case Sashimi -> {
                    if ((counts.get(Sashimi) + card.count) / 3 > counts.get(Sashimi) / 3) {
                        points += parameters.valueSashimiTriple;
                    }
                }
                case Dumpling -> {
                    for (int i = 0; i < card.count; i++) {
                        int index = Math.min(counts.get(Dumpling) + i, parameters.valueDumpling.length - 1);
                        points += parameters.valueDumpling[index];
                    }
                }
                default -> {
                }
            }
            counts.merge(card.type, card.count, Integer::sum);
            if (isNigiri(card.type) && counts.get(Wasabi) > 0) {
                counts.merge(Wasabi, -1, Integer::sum);
            }
        }
        return points;
    }

    private static double expectedCardPoints(SGParameters parameters,
                                             Map<SGCard.SGCardType, Integer> counts,
                                             int remainingPicks,
                                             CardAvailability availability) {
        if (remainingPicks == 0) {
            return 0.0;
        }
        // Discount raw deck probabilities to reflect that not every remaining card
        // will reach this player or be worth selecting.
        double tempuraProbability = availability.probability(Tempura) * 0.65;
        double sashimiProbability = availability.probability(Sashimi) * 0.65;
        double dumplingProbability = availability.probability(Dumpling) * 0.75;
        double nigiriProbability = (availability.probability(EggNigiri)
                + availability.probability(SalmonNigiri)
                + availability.probability(SquidNigiri)) * 0.7;

        double value = expectedGroupedPoints(remainingPicks, tempuraProbability,
                counts.get(Tempura), 2, parameters.valueTempuraPair);
        value += expectedGroupedPoints(remainingPicks, sashimiProbability,
                counts.get(Sashimi), 3, parameters.valueSashimiTriple);

        double expectedDumplings = 0.0;
        for (int future = 0; future <= remainingPicks; future++) {
            double probability = binomialProbability(remainingPicks, future, dumplingProbability);
            for (int i = 0; i < future && counts.get(Dumpling) + i < parameters.valueDumpling.length; i++) {
                expectedDumplings += probability * parameters.valueDumpling[counts.get(Dumpling) + i];
            }
        }
        value += expectedDumplings;

        double expectedNigiri = remainingPicks * (availability.probability(EggNigiri) * parameters.valueEggNigiri
                + availability.probability(SalmonNigiri) * parameters.valueSalmonNigiri
                + availability.probability(SquidNigiri) * parameters.valueSquidNigiri) * 0.7;
        value += expectedNigiri;
        int activeWasabi = counts.get(Wasabi);
        int activeChopsticks = counts.get(Chopsticks);
        value += Math.min(activeWasabi, remainingPicks * nigiriProbability)
                * averageNigiriValue(parameters, availability) * (parameters.multiplierWasabi - 1);
        value += Math.min(activeChopsticks, remainingPicks) * 2.2;
        return value;
    }

    private static double expectedGroupedPoints(int picks, double probability, int currentCount,
                                                int groupSize, int pointsPerGroup) {
        double expected = 0.0;
        int existingGroups = currentCount / groupSize;
        for (int future = 0; future <= picks; future++) {
            int groups = (currentCount + future) / groupSize;
            expected += binomialProbability(picks, future, probability)
                    * (groups - existingGroups) * pointsPerGroup;
        }
        return expected;
    }

    private static double expectedPuddingPoints(SGGameState state,
                                                SGParameters parameters,
                                                int player,
                                                int[] remainingPicks,
                                                CardAvailability availability,
                                                int additionalPuddings) {
        double puddingProbability = availability.probability(Pudding);
        // Puddings persist between rounds, unlike most cards, so include a rough
        // estimate of future-round opportunities when projecting the final standings.
        double expectedFutureCards = Math.max(0, parameters.nRounds - state.getRoundCounter() - 1)
                * Math.max(0, parameters.nCards - state.getNPlayers() + 2);
        double[] projectedCounts = new double[state.getNPlayers()];
        double uncertainty = 1.0;
        for (int p = 0; p < state.getNPlayers(); p++) {
            int current = state.getPlayedCardTypesAllGame()[p].get(Pudding).getValue();
            for (ChooseCard pending : state.getCardChoices().get(p)) {
                if (((SGCard) pending.getCard(state)).type == Pudding) {
                    current++;
                }
            }
            projectedCounts[p] = current
                    + remainingPicks[p] * puddingProbability
                    + expectedFutureCards * puddingProbability;
            if (p == player) {
                projectedCounts[p] += additionalPuddings;
            }
            uncertainty += remainingPicks[p] * puddingProbability
                    + expectedFutureCards * puddingProbability;
        }

        double probabilityMost = 1.0;
        double probabilityLeast = 1.0;
        for (int opponent = 0; opponent < state.getNPlayers(); opponent++) {
            if (opponent == player) {
                continue;
            }
            double probabilityAhead = logistic((projectedCounts[player] - projectedCounts[opponent])
                    / Math.sqrt(uncertainty));
            probabilityMost *= probabilityAhead;
            probabilityLeast *= 1.0 - probabilityAhead;
        }
        double value = parameters.valuePuddingMost * probabilityMost;
        if (state.getNPlayers() > 2) {
            value += parameters.valuePuddingLeast * probabilityLeast;
        }
        return value;
    }

    private static void addExpectedMakiPoints(SGGameState state,
                                              SGParameters parameters,
                                              List<Map<SGCard.SGCardType, Integer>> counts,
                                              int[] remainingPicks,
                                              double[] projectedScores,
                                              CardAvailability availability) {
        // Maki scores depend on relative icon totals, so project each player's count
        // before awarding expected first- and second-place points.
        double iconsPerCard = expectedMakiIconsPerCard(availability);
        double[] projectedMaki = new double[state.getNPlayers()];
        for (int player = 0; player < state.getNPlayers(); player++) {
            projectedMaki[player] = counts.get(player).get(Maki) + remainingPicks[player] * iconsPerCard;
        }

        double highest = 0.0;
        for (double maki : projectedMaki) {
            highest = Math.max(highest, maki);
        }
        if (highest == 0.0) {
            return;
        }

        int mostPlayers = 0;
        for (double maki : projectedMaki) {
            if (Double.compare(maki, highest) == 0) {
                mostPlayers++;
            }
        }
        double secondHighest = 0.0;
        for (double maki : projectedMaki) {
            if (maki < highest) {
                secondHighest = Math.max(secondHighest, maki);
            }
        }
        int secondPlayers = 0;
        for (double maki : projectedMaki) {
            if (maki == secondHighest && secondHighest > 0.0) {
                secondPlayers++;
            }
        }

        for (int player = 0; player < state.getNPlayers(); player++) {
            if (projectedMaki[player] == highest) {
                projectedScores[player] += parameters.valueMakiMost / (double) mostPlayers;
            } else if (mostPlayers == 1 && projectedMaki[player] == secondHighest && secondHighest > 0.0) {
                projectedScores[player] += parameters.valueMakiSecond / (double) secondPlayers;
            }
        }
    }

    private static double expectedMakiIconsPerCard(CardAvailability availability) {
        int totalMaki = availability.makiCardsByIcons.values().stream().mapToInt(Integer::intValue).sum();
        if (totalMaki == 0) {
            return 0.0;
        }
        return availability.makiCardsByIcons.entrySet().stream()
                .mapToInt(entry -> entry.getKey() * entry.getValue()).sum() / (double) totalMaki;
    }

    private static double binomialProbability(int trials, int successes, double probability) {
        if (successes < 0 || successes > trials) {
            return 0.0;
        }
        if (probability <= 0.0) {
            return successes == 0 ? 1.0 : 0.0;
        }
        if (probability >= 1.0) {
            return successes == trials ? 1.0 : 0.0;
        }
        double coefficient = 1.0;
        for (int i = 1; i <= successes; i++) {
            coefficient *= (trials - (double) i + 1) / i;
        }
        return coefficient * Math.pow(probability, successes) * Math.pow(1.0 - probability, trials - successes);
    }

    private static double logistic(double value) {
        if (value >= 0.0) {
            double exp = Math.exp(-value);
            return 1.0 / (1.0 + exp);
        }
        double exp = Math.exp(value);
        return exp / (1.0 + exp);
    }

    private static double averageNigiriValue(SGParameters parameters, CardAvailability availability) {
        double count = availability.probability(EggNigiri)
                + availability.probability(SalmonNigiri)
                + availability.probability(SquidNigiri);
        if (count == 0.0) {
            return 0.0;
        }
        return (availability.probability(EggNigiri) * parameters.valueEggNigiri
                + availability.probability(SalmonNigiri) * parameters.valueSalmonNigiri
                + availability.probability(SquidNigiri) * parameters.valueSquidNigiri) / count;
    }

    static CardAvailability estimateAvailability(SGGameState state, int observerId) {
        return new CardAvailability(state, observerId);
    }

    @Override
    public AbstractAction getRolloutAction(AbstractGameState gameState, List<AbstractAction> actions,
                                           RandomPlayer randomPlayer) {
        return super.getRolloutAction(gameState, actions, randomPlayer);
    }

    AbstractAction selectCardPickingRolloutAction(AbstractGameState gameState, List<AbstractAction> actions,
                                                  RandomPlayer randomPlayer) {
        if (!(gameState instanceof SGGameState state) || actions.isEmpty()
                || !(actions.getFirst() instanceof ChooseCard)) {
            return super.getRolloutAction(gameState, actions, randomPlayer);
        }

        SGParameters parameters = (SGParameters) state.getGameParameters();
        // Rollouts choose for every simulated player, but availability is estimated
        // from the controlled bot's observation rather than omniscient hidden hands.
        int observerId = getPlayerID();
        if (observerId < 0 || observerId >= state.getNPlayers()) {
            observerId = state.getCurrentPlayer();
        }
        CardAvailability availability = estimateAvailability(state, observerId);
        AbstractAction bestAction = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (AbstractAction action : actions) {
            if (!(action instanceof ChooseCard choice)) {
                return super.getRolloutAction(gameState, actions, randomPlayer);
            }
            SGCard card = (SGCard) choice.getCard(state);
            double value = rolloutCardValue(state, parameters, choice.playerId, card, availability);
            value += rnd.nextDouble() * 1e-6;
            if (value > bestValue) {
                bestValue = value;
                bestAction = action;
            }
        }
        return bestAction;
    }

    private static double rolloutCardValue(SGGameState state, SGParameters parameters, int player,
                                           SGCard card, CardAvailability availability) {
        Map<SGCard.SGCardType, Integer> counts = getEffectiveCounts(state, player);
        int remainingPicks = Math.max(0, state.getPlayerHands().get(player).getSize()
                - state.getCardChoices().get(player).size() - 1);
        int futureDraftPicks = Math.min(4, remainingPicks);
        double probabilityTempura = availability.probability(Tempura) * 0.5;
        double probabilitySashimi = availability.probability(Sashimi) * 0.5;
        double nigiriProbability = availability.probability(EggNigiri)
                + availability.probability(SalmonNigiri) + availability.probability(SquidNigiri);
        // Estimate the marginal value of this pick: combinations get value for
        // completing them, while single-card points are scored immediately.
        return switch (card.type) {
            case EggNigiri -> parameters.valueEggNigiri
                    * (counts.get(Wasabi) > 0 ? parameters.multiplierWasabi : 1);
            case SalmonNigiri -> parameters.valueSalmonNigiri
                    * (counts.get(Wasabi) > 0 ? parameters.multiplierWasabi : 1);
            case SquidNigiri -> parameters.valueSquidNigiri
                    * (counts.get(Wasabi) > 0 ? parameters.multiplierWasabi : 1);
            case Wasabi -> Math.min(1.0, remainingPicks * nigiriProbability * 0.7)
                    * averageNigiriValue(parameters, availability) * (parameters.multiplierWasabi - 1);
            case Chopsticks -> remainingPicks > 0 ? 2.2 : 0.0;
            case Tempura -> counts.get(Tempura) % 2 == 1
                    ? parameters.valueTempuraPair
                    : parameters.valueTempuraPair * probabilityAtLeast(futureDraftPicks, 1, probabilityTempura);
            case Sashimi -> {
                int needed = 2 - counts.get(Sashimi) % 3;
                yield needed <= 0 ? parameters.valueSashimiTriple
                        : parameters.valueSashimiTriple
                        * probabilityAtLeast(futureDraftPicks, needed, probabilitySashimi);
            }
            case Dumpling -> parameters.valueDumpling[
                    Math.min(counts.get(Dumpling), parameters.valueDumpling.length - 1)];
            case Maki -> Math.max(0.0, makiAward(state, parameters, player, card.count)
                    - makiAward(state, parameters, player, 0)) + 0.5;
            case Pudding -> expectedPuddingPoints(state, parameters, player,
                    remainingPicksByPlayer(state), availability, 1)
                    - expectedPuddingPoints(state, parameters, player,
                    remainingPicksByPlayer(state), availability, 0);
        };
    }

    private static int[] remainingPicksByPlayer(SGGameState state) {
        int[] remainingPicks = new int[state.getNPlayers()];
        for (int player = 0; player < state.getNPlayers(); player++) {
            remainingPicks[player] = Math.max(0, state.getPlayerHands().get(player).getSize()
                    - state.getCardChoices().get(player).size());
        }
        return remainingPicks;
    }

    private static double probabilityAtLeast(int trials, int needed, double probability) {
        if (needed <= 0) {
            return 1.0;
        }
        double below = 0.0;
        for (int successes = 0; successes < needed; successes++) {
            below += binomialProbability(trials, successes, probability);
        }
        return Math.max(0.0, Math.min(1.0, 1.0 - below));
    }

    private static double makiAward(SGGameState state, SGParameters parameters, int player, int extraIcons) {
        int[] counts = new int[state.getNPlayers()];
        for (int p = 0; p < state.getNPlayers(); p++) {
            counts[p] = state.getPlayedCardTypes()[p].get(Maki).getValue();
            for (ChooseCard pending : state.getCardChoices().get(p)) {
                SGCard pendingCard = (SGCard) pending.getCard(state);
                if (pendingCard.type == Maki) {
                    counts[p] += pendingCard.count;
                }
            }
        }
        counts[player] += extraIcons;
        int most = Arrays.stream(counts).max().orElse(0);
        if (most == 0) {
            return 0.0;
        }
        int mostPlayers = (int) Arrays.stream(counts).filter(value -> value == most).count();
        if (counts[player] == most) {
            return parameters.valueMakiMost / (double) mostPlayers;
        }
        int second = Arrays.stream(counts).filter(value -> value < most).max().orElse(0);
        if (mostPlayers == 1 && counts[player] == second && second > 0) {
            int secondPlayers = (int) Arrays.stream(counts).filter(value -> value == second).count();
            return parameters.valueMakiSecond / (double) secondPlayers;
        }
        return 0.0;
    }

    static final class CardAvailability {
        private final EnumMap<SGCard.SGCardType, Integer> remainingByType =
                new EnumMap<>(SGCard.SGCardType.class);
        private final Map<Integer, Integer> makiCardsByIcons = new HashMap<>();
        private int totalRemaining;

        private CardAvailability(SGParameters parameters) {
            initializeDeck(parameters);
            totalRemaining = remainingByType.values().stream().mapToInt(Integer::intValue).sum();
        }

        private CardAvailability(SGGameState state, int observerId) {
            SGParameters parameters = (SGParameters) state.getGameParameters();
            initializeDeck(parameters);

            // Played cards are public. Only subtract hand contents when the game state
            // says they are visible to the observer; unknown hands must not leak information.
            for (int player = 0; player < state.getNPlayers(); player++) {
                for (SGCard.SGCardType type : SGCard.SGCardType.values()) {
                    if (type != Maki) {
                        subtract(type, state.getPlayedCardTypesAllGame()[player].get(type).getValue());
                    }
                }
                for (SGCard played : state.getPlayedCards().get(player).getComponents()) {
                    if (played.type == Maki) {
                        subtractMaki(played.count);
                    }
                }
            }
            for (int player = 0; player < state.getNPlayers(); player++) {
                if (observerId >= 0 && observerId < state.getNPlayers()
                        && state.isHandKnown(observerId, player)) {
                    for (SGCard card : state.getPlayerHands().get(player).getComponents()) {
                        if (card.type == Maki) {
                            subtractMaki(card.count);
                        } else {
                            subtract(card.type, 1);
                        }
                    }
                }
            }
            totalRemaining = remainingByType.values().stream().mapToInt(Integer::intValue).sum();
        }

        private void initializeDeck(SGParameters parameters) {
            for (SGCard.SGCardType type : SGCard.SGCardType.values()) {
                remainingByType.put(type, 0);
            }
            for (Map.Entry<Pair<SGCard.SGCardType, Integer>, Integer> entry
                    : parameters.nCardsPerType.entrySet()) {
                SGCard.SGCardType type = entry.getKey().a;
                int quantity = entry.getValue();
                remainingByType.merge(type, quantity, Integer::sum);
                if (type == Maki) {
                    makiCardsByIcons.put(entry.getKey().b, quantity);
                }
            }
        }

        private void subtract(SGCard.SGCardType type, int quantity) {
            remainingByType.compute(type, (ignored, count) -> Math.max(0, count - quantity));
        }

        private void subtractMaki(int icons) {
            Integer available = makiCardsByIcons.get(icons);
            if (available != null && available > 0) {
                makiCardsByIcons.put(icons, available - 1);
                subtract(Maki, 1);
            }
        }

        double probability(SGCard.SGCardType type) {
            return totalRemaining == 0 ? 0.0 : remainingByType.get(type) / (double) totalRemaining;
        }
    }

    private static boolean isNigiri(SGCard.SGCardType type) {
        return type == EggNigiri || type == SalmonNigiri || type == SquidNigiri;
    }

    @Override
    public MuditBot copy() {
        return new MuditBot((BasicMCTSParams) parameters.copy());
    }
}
