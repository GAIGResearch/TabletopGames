package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Component;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;
import players.sushiGoPlayer.CardCounter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class CountingCardsHeuristic implements IStateHeuristic {

    /**
     * This cares mostly about the raw game score - but will treat winning as a 50% bonus
     * and losing as halving it
     *
     * @param gs       - game state to evaluate and score.
     * @param playerId - player id
     * @return
     */

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {

        SGGameState gameState = (SGGameState) gs;

        CardCounter cardCounter = new CardCounter(gameState);

        // This counts all cards which have already been played
        gameState.getPlayedCards().stream()
                .flatMap(deck -> deck.getComponents().stream())
                .forEach(card -> {
                    cardCounter.markAsSeen(card);
                });

        // This counts all cards in the discard pile
        Component discardPile = gs.getAllTopLevelComponents().stream()
                .filter(component -> "Discard pile".equals(component.getComponentName()))
                .findFirst().orElse(null);

        var d = (Deck<SGCard>) discardPile;

        d.getComponents().forEach(card -> {
            cardCounter.markAsSeen((SGCard) card);
        });

        // This adds all the cards which are hands that you have already seen
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            if (gameState.isHandKnown(playerId, i)) {
                gameState.getPlayerHands().get(i).forEach(card -> {
                    cardCounter.markAsSeen((SGCard) card);
                });
            }
        }

//        if(playerId == 3) {
//            System.out.println(gameState.getNPlayers());
//            System.out.println(gameState.getRoundCounter());
//            System.out.println(cardCounter);
//        }

        HashMap<SGCard.SGCardType, Double> expectedScore = new HashMap<>();

        int numberOfPlayers = gameState.getNPlayers();

        int R = gameState.getPlayerHands().get(playerId).getSize() - 1;

        for (int i = 0; i < gameState.getPlayerHands().get(playerId).getComponents().size(); i++) {
            SGCard card = (SGCard) gameState.getPlayerHands().get(playerId).getComponents().get(i);


            switch (card.type) {
                case Tempura:
                    int valueTempura = ((SGParameters) gameState.getGameParameters()).valueTempuraPair;
                    Counter tempuraOnTable = gameState.getPlayedCardTypes(SGCard.SGCardType.Tempura, playerId);
                    int tempuraNeeded = tempuraOnTable.getValue() % 2 == 0 ? 1 : 0;

                    if (tempuraNeeded == 0) {
                        // 5 points from getting the tempura pair
                        expectedScore.putIfAbsent(SGCard.SGCardType.Tempura, (double) valueTempura);
                        continue;
                    } else if (tempuraNeeded == 1 && R == 0) {
                        expectedScore.putIfAbsent(SGCard.SGCardType.Tempura, 0.0);
                        continue;
                    } else {
                        double lambda = 0;

                        for (int s = 1; s <= Math.min(numberOfPlayers - 1, R); s++) {
                            // This is the player who holds a hand that is due to reach you
                            int holder = (playerId + s) % numberOfPlayers;
                            // This is basically, all the players who will get this hand before it reaches the player
                            List<Integer> pickers = IntStream.rangeClosed(1, s)
                                    .map(n -> (playerId + n) % numberOfPlayers)
                                    .boxed()
                                    .toList();

                            lambda += cardsOutThere(gameState, holder, playerId, card.type,card.count, cardCounter) * survival(pickers);
                        }

                        if (R >= numberOfPlayers) {
                            List<Integer> pickersOtherThanMe = IntStream.range(0, numberOfPlayers)
                                    .filter(n -> n != playerId)
                                    .boxed()
                                    .toList();

                            lambda += ownCards(playerId, gameState, card.type) * survival(pickersOtherThanMe);
                        }

                        lambda = Math.min(R, lambda);

                        expectedScore.putIfAbsent(SGCard.SGCardType.Tempura, (double) valueTempura * (1 - Math.exp(-lambda)));
                    }
                    break;
                case Sashimi:
                    int valueSashmi = ((SGParameters) gameState.getGameParameters()).valueSashimiTriple;
                    Counter sashmiOnTable = gameState.getPlayedCardTypes(SGCard.SGCardType.Sashimi, playerId);
                    int sashmiNeeded;

                    if (sashmiOnTable.getValue() % 3 == 0) {
                        sashmiNeeded = 2;
                    } else if (sashmiOnTable.getValue() % 3 == 1) {
                        sashmiNeeded = 1;
                    } else {
                        sashmiNeeded = 0;
                    }


                    if (sashmiNeeded == 0) {
                        // 10 points from getting the tempura pair
                        expectedScore.putIfAbsent(SGCard.SGCardType.Sashimi, (double) valueSashmi);
                        break;
                    } else if ((sashmiNeeded == 1 && R == 0) || (sashmiNeeded == 2 && R <= 1)) {
                        expectedScore.putIfAbsent(SGCard.SGCardType.Sashimi, 0.0);
                        break;
                    } else {
                        double lambda = 0;

                        for (int s = 1; s <= Math.min(numberOfPlayers - 1, R); s++) {
                            // This is the player who holds a hand that is due to reach you
                            int holder = (playerId + s) % numberOfPlayers;
                            // This is basically, all the players who will get this hand before it reaches the player
                            List<Integer> pickers = IntStream.rangeClosed(1, s)
                                    .map(n -> (playerId + n) % numberOfPlayers)
                                    .boxed()
                                    .toList();

                            lambda += cardsOutThere(gameState, holder, playerId, card.type, card.count, cardCounter) * survival(pickers);
                        }

                        if (R >= numberOfPlayers) {
                            List<Integer> pickersOtherThanMe = IntStream.range(0, numberOfPlayers)
                                    .filter(n -> n != playerId)
                                    .boxed()
                                    .toList();

                            lambda += ownCards(playerId, gameState, card.type) * survival(pickersOtherThanMe);
                        }

                        lambda = Math.min(R, lambda);
                        if (sashmiNeeded == 2) {
                            expectedScore.putIfAbsent(SGCard.SGCardType.Sashimi, (double) valueSashmi * (1 - Math.exp(-lambda) * (1 + lambda)));
                        } else {
                            expectedScore.putIfAbsent(SGCard.SGCardType.Sashimi, (double) valueSashmi * (1 - Math.exp(-lambda)));
                        }

                    }
                    break;
                case Dumpling:
                    break;
                case SquidNigiri:
                    addNigiriExpectedScore(gameState,playerId, SGCard.SGCardType.SquidNigiri, expectedScore);
                    break;
                case SalmonNigiri:
                    addNigiriExpectedScore(gameState,playerId, SGCard.SGCardType.SalmonNigiri, expectedScore);
                    break;
                case EggNigiri:
                    addNigiriExpectedScore(gameState,playerId, SGCard.SGCardType.EggNigiri, expectedScore);
                    break;
                case Wasabi:
                    Counter wasabiOnTable = gameState.getPlayedCardTypes(SGCard.SGCardType.Wasabi, playerId);

                    int nigriNeeded = wasabiOnTable.getValue() + 1;

                    double wasabiMultiplier = ((SGParameters) gameState.getGameParameters()).multiplierWasabi;
                    double squidLambda = calculateProbabilityOfKCardsArriveToMainPlayer(gameState, playerId, R, nigriNeeded, SGCard.SGCardType.SquidNigiri, 0, cardCounter);
                    double salmonLambda = calculateProbabilityOfKCardsArriveToMainPlayer(gameState, playerId, R, nigriNeeded, SGCard.SGCardType.SalmonNigiri, 0, cardCounter);
                    double eggLambda = calculateProbabilityOfKCardsArriveToMainPlayer(gameState, playerId, R, nigriNeeded, SGCard.SGCardType.EggNigiri, 0, cardCounter);

                    int valueSquid = ((SGParameters) gameState.getGameParameters()).valueSquidNigiri;
                    int valueSalmon = ((SGParameters) gameState.getGameParameters()).valueSalmonNigiri;
                    int valueEgg = ((SGParameters) gameState.getGameParameters()).valueEggNigiri;

                    double squidProbability = calculateProbabilityByCardsNeededAndLambda(squidLambda, nigriNeeded);
                    double salmonProbability = calculateProbabilityByCardsNeededAndLambda(salmonLambda, nigriNeeded);
                    double eggProbability = calculateProbabilityByCardsNeededAndLambda(eggLambda, nigriNeeded);

                    double maxEstimatedScore = Math.max(Math.max((wasabiMultiplier - 1) * squidProbability * valueSquid, (wasabiMultiplier - 1) * salmonProbability * valueSalmon)
                            ,(wasabiMultiplier - 1) * eggProbability * valueEgg);

                    expectedScore.putIfAbsent(SGCard.SGCardType.Wasabi, maxEstimatedScore);
                    break;
                case Chopsticks:
                    // handle Chopsticks
                    break;
                case Pudding:
                    // handle Pudding
                    break;
                case Maki:
                    switch (card.count) {
                        case 1:
                            // handle Maki1
                            break;
                        case 2:
                            // handle Maki2
                            break;
                        case 3:
                            // handle Maki3
                            break;
                        default:
                            throw new IllegalStateException("Unexpected Maki count: " + card.count);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected card type: " + card.type);
            }
        }

        double sumExpectedScores = expectedScore.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        double score = gs.getGameScore(playerId);
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME)
            return score * 1.5;
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME)
            return score * 0.5;
        return score + sumExpectedScores;
    }

    private double calculateProbabilityOfKCardsArriveToMainPlayer(SGGameState gameState, int playerId, int R, int cardsNeeded, SGCard.SGCardType cardType, int cardCount, CardCounter cardCounter) {
        double lambda = 0;

        if (cardsNeeded <= 0) {
            return 1;
        }
        if (cardsNeeded > R) {
            return 0;
        }

        int numberOfPlayers = gameState.getNPlayers();

        for (int s = 1; s <= Math.min(numberOfPlayers - 1, R); s++) {
            // This is the player who holds a hand that is due to reach you
            int holder = (playerId + s) % numberOfPlayers;
            // This is basically, all the players who will get this hand before it reaches the player
            List<Integer> pickers = IntStream.rangeClosed(1, s)
                    .map(n -> (playerId + n) % numberOfPlayers)
                    .boxed()
                    .toList();

            lambda += cardsOutThere(gameState, holder, playerId, cardType,cardCount, cardCounter) * survival(pickers);
        }

        if (R >= numberOfPlayers) {
            List<Integer> pickersOtherThanMe = IntStream.range(0, numberOfPlayers)
                    .filter(n -> n != playerId)
                    .boxed()
                    .toList();

            lambda += ownCards(playerId, gameState, cardType) * survival(pickersOtherThanMe);
        }

        lambda = Math.min(R, lambda);

        return lambda;
    }

    private double calculateProbabilityByCardsNeededAndLambda(double lambda, int cardsNeeded) {
        double term = Math.exp(-lambda);
        double sum = term;

        for (int j = 1; j <= cardsNeeded - 1; j++) {
            term = term * lambda / j;
            sum += term;
        }
        return 1 - sum;
    }

    private void addNigiriExpectedScore(
            SGGameState gameState,
            int playerId,
            SGCard.SGCardType nigiriType,
            Map<SGCard.SGCardType, Double> expectedScore
    ) {
        SGParameters params = (SGParameters) gameState.getGameParameters();

        int baseValue = switch (nigiriType) {
            case SquidNigiri -> params.valueSquidNigiri;
            case SalmonNigiri -> params.valueSalmonNigiri;
            case EggNigiri -> params.valueEggNigiri;
            default -> throw new IllegalArgumentException("Not a Nigiri type: " + nigiriType);
        };

        int wasabiAvailable = gameState.getPlayedCardTypes(SGCard.SGCardType.Wasabi, playerId).getValue();
        double score = wasabiAvailable > 0 ? (double) baseValue * params.multiplierWasabi : (double) baseValue;

        expectedScore.putIfAbsent(nigiriType, score);
    }


    // TODO: 5. Maki is different. The known count and T_Y use icons (card.count), and keyOf(card.type, card.count) only matches one maki variant. It's fine for tempura, but needs a special case when yo do maki.
    private double cardsOutThere(SGGameState gameState, int currentPlayerHolder, int mainPlayer, SGCard.SGCardType cardType, int cardCount, CardCounter cardCounter) {
        Deck<SGCard> holdersDeck = gameState.getPlayerHands().get(currentPlayerHolder);
        if (gameState.isHandKnown(mainPlayer, currentPlayerHolder)) {
            return holdersDeck.stream()
                    .filter(c -> {
                        SGCard card1 = (SGCard) c;
                        return card1.type == cardType;
                    })
                    .count();
        } else {
            double probabilityOfUnseenTempura = cardCounter.unseenCards.get(CardCounter.keyOf(cardType, cardCount)) / (double) cardCounter.countAllUnseenCards();
            int holdersHandSize = holdersDeck.getSize();
            return probabilityOfUnseenTempura * holdersHandSize;
        }
    }

    private double ownCards(int playerId, SGGameState gameState, SGCard.SGCardType cardType) {
        return gameState.getPlayerHands().get(playerId).stream()
                .filter(c -> {
                    SGCard card1 = (SGCard) c;
                    return card1.type == cardType;
                })
                .count();
    }

    private double survival(List<Integer> survialChanceTheHandBeforeItReachsMainPlayer) {
        double result = 1;

        for (Integer player :  survialChanceTheHandBeforeItReachsMainPlayer) {
            result *= (1 - 0.25);
        }
        return result;
    }
    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }
    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public String toString() {
        return "CountingCardsHeuristic";
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof CountingCardsHeuristic;
    }
    @Override
    public int hashCode() {
        return 3;
    }
}
