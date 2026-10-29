package players.heuristics;
import core.AbstractGameState;
import core.components.Counter;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;

import java.util.Map;

public class SushiGoHeuristic implements IStateHeuristic {
    /*
     * A score difference of about twenty points is already strategically
     * meaningful in Sushi Go. Dividing by this scale keeps score position
     * comparable with the smaller set-collection features before tanh is
     * applied. The MCTS configuration uses K = 0.1 for the resulting
     * bounded [-1, 1] heuristic values.
     */
    private static final double SCORE_DIFFERENCE_SCALE = 20.0;

    /**
     * Class estimates how favourable a Sushi Go state is for a specified player.
     *
     * The heuristic combines the player's score relative to the leading
     * opponent with unresolved scoring opportunities
     * Pudding is weighted more heavily as the game approaches its final round
     * because it is scored only at game end.
     *
     * Completed sets are not counted again because their points are already
     * included in the current score.
     * The final value is squashed into [-1, 1] using tanh to match the range
     * declared by IStateHeuristic and making it suitable for MCTS backup.
     *
     * @param gs       game state to evaluate
     * @param playerId ID of the player
     * @return a value in the range [-1, 1]
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        // gs is a general game state so i have to cast it to the Sushi Go one
        // otherwise we cant use the sushi go methods
        SGGameState state = (SGGameState) gs;

        // If the game is over, return the actual score for this player
        if (!state.isNotTerminal()) {
            // the result value is win/lose/draw for this player
            return state.getPlayerResults()[playerId].value;
        }

        // The heuristic is based on the current score, plus the value of any
        // incomplete sets of cards that have been played, plus the value of
        // any puddings that have been played, which are scored at the end of the game.

        // get the game parameters (these hold how many points each card set is worth)
        SGParameters parameters = (SGParameters) state.getGameParameters();

        // played is an array with one entry per player
        // each entry is a map that says how many of each card type that player has played this round
        Map<SGCard.SGCardType, Counter>[] played = state.getPlayedCardTypes();

        // the player we are currently looking at
        Map<SGCard.SGCardType, Counter> mine = played[playerId];

        // Score is relative to the current leader, rather than an absolute score
        // because the objective is to beat the other players.
        // Scale the score gap so it is comparable with the card features.
        double value = scoreDifference(state, playerId) / SCORE_DIFFERENCE_SCALE;

        // ---------- TEMPURA ----------
        // Only unfinished sets are added: completed sets are already included
        // in the score and must not be counted twice.

        int tempura = count(mine, SGCard.SGCardType.Tempura);
        // tempura comes in pairs, so if i have an odd number one can be added to make it even
        if (tempura % 2 == 1) {
            // add half the pair value because it is only half done
            value += parameters.valueTempuraPair / 2.0;
        }

        // ---------- SASHIMI ----------
        int sashimi = count(mine, SGCard.SGCardType.Sashimi);
        // sashimi comes in triples so sashimi % 3 is how many cards are in the unfinished triple
        // each of those cards is worth a third of the triple score
        value += (sashimi % 3) * (parameters.valueSashimiTriple / 3.0);

        // ---------- DUMPLING ----------
        int dumplings = count(mine, SGCard.SGCardType.Dumpling);
        // The next dumpling can still be useful, but repeated collection is
        // damped so that the heuristic does not chase dumplings forever.
        // Each additional card contributes a smaller exploratory bonus;
        // points already awarded remain in the score.
        double dumplingBonus = 0.0;
        for (int i = 0; i < dumplings; i++) {
            int marginalScore = parameters.valueDumpling[
                    Math.min(i, parameters.valueDumpling.length - 1)];
            dumplingBonus += marginalScore * 0.35 / Math.pow(i + 1.0, 2.0);
        }
        value += dumplingBonus;

        // ---------- MAKI ----------
        // Maki scoring is competitive and is awarded at the end of a round.
        int maki = count(mine, SGCard.SGCardType.Maki);
        // the highest maki count out of all the other players
        int opponentMaki = maxOpponentCount(played, playerId, SGCard.SGCardType.Maki);
        // if i have more maki than the best opponent this is positive, if less it is negative
        value += (maki - opponentMaki) * 0.35;

        // ---------- PUDDING ----------
        // Puddings are scored only at game end, so increase their influence as
        // the game approaches the final round.
        // puddings carry over between rounds so we use the "all game" played cards here
        // instead of the "this round" ones
        int puddings = count(state.getPlayedCardTypesAllGame()[playerId],
                SGCard.SGCardType.Pudding);
        // best pudding count of the opponents
        int opponentPuddings = maxOpponentCount(state.getPlayedCardTypesAllGame(),
                playerId, SGCard.SGCardType.Pudding);
        // round progress is 0 at the start of the game and 1 at the end
        double roundProgress = Math.min(1.0,
                (state.getRoundCounter() + 1.0) / parameters.nRounds);
        // the difference between my puddings and the best opponent's is multiplied by a weight
        // and by the round progress, so it is more important at the end of the game
        value += (puddings - opponentPuddings) * parameters.valuePuddingMost
                * roundProgress * 0.25;

        // tanh squashes any number into the range -1 to 1, which is what the class promises
        // dividing by 6 first stops it going flat at -1 or 1 too quickly
        return Math.tanh(value / 6.0);
    }

    // small helper method to get the number of cards of one type
    // the map gives a Counter object, and getValue() turns it into a normal int
    private static int count(Map<SGCard.SGCardType, Counter> cards,
                             SGCard.SGCardType type) {
        return cards.get(type).getValue();
    }

    // helper method that finds the biggest count of a card type among all the opposing players
    private static int maxOpponentCount(
            Map<SGCard.SGCardType, Counter>[] cards,
            int playerId, SGCard.SGCardType type) {
        // start at 0 and go up when we find a bigger number
        int maximum = 0;
        // loop through every player
        for (int opponent = 0; opponent < cards.length; opponent++) {
            // skip ourselves, we only want the opponents
            if (opponent != playerId) {
                // keep whichever is bigger, the old maximum or this opponent's count
                maximum = Math.max(maximum, count(cards[opponent], type));
            }
        }
        // return te maximum count of this card type among the opponents
        return maximum;
    }

    // helper method that works out my score minus the best opponent score
    private static double scoreDifference(SGGameState state, int playerId) {
        // start at 0, then find the best opponent score
        double opponentScore = 0.0;
        // loop through all the players
        for (int opponent = 0; opponent < state.getNPlayers(); opponent++) {
            // again skip ourselves
            if (opponent != playerId) {
                // keep the highest opponent score
                opponentScore = Math.max(opponentScore, state.getGameScore(opponent));
            }
        }
        // positive = winning
        // negative = losing
        return state.getGameScore(playerId) - opponentScore;
    }

    // the lowest value this heuristic can ever return (needed by the interface)
    @Override
    public double minValue() {
        return -1.0;
    }

    // the highest value this heuristic can ever return (needed by the interface)
    @Override
    public double maxValue() {
        return 1.0;
    }
}