package games.toads;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import games.GameType;

import java.util.*;


public class ToadGameState extends AbstractGameState {


    public ToadGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    List<Deck<ToadCard>> playerDecks;
    List<Deck<ToadCard>> playerHands;
    int[][] battlesWon;
    List<Deck<ToadCard>> playerDiscards;
    ToadCard[] hiddenFlankCards;
    ToadCard[] fieldCards;
    ToadCard[] tieBreakers;

    @Override
    protected GameType _getGameType() {
        return GameType.WarOfTheToads;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(playerDecks.get(0).stream().toList());
        components.addAll(playerDecks.get(1).stream().toList());
        components.addAll(playerHands.get(0).stream().toList());
        components.addAll(playerHands.get(1).stream().toList());
        return components;
    }

    @Override
    protected ToadGameState _copy(int playerId) {
        ToadParameters params = (ToadParameters) this.gameParameters;
        ToadGameState copy = new ToadGameState(params.shallowCopy(), getNPlayers());
        copy.playerDecks = new ArrayList<>();
        for (Deck<ToadCard> deck : playerDecks) {
            copy.playerDecks.add(deck.copy());
        }
        copy.playerHands = new ArrayList<>();
        for (Deck<ToadCard> hand : playerHands) {
            copy.playerHands.add(hand.copy());
        }
        copy.playerDiscards = new ArrayList<>();
        for (Deck<ToadCard> discard : playerDiscards) {
            copy.playerDiscards.add(discard.copy());
        }
        copy.battlesWon = new int[2][2];
        for (int i = 0; i < 2; i++) {
            copy.battlesWon[i] = Arrays.copyOf(battlesWon[i], 2);
        }

        copy.hiddenFlankCards = new ToadCard[hiddenFlankCards.length];
        copy.fieldCards = new ToadCard[fieldCards.length];
        copy.tieBreakers = new ToadCard[tieBreakers.length];

        for (int i = 0; i < hiddenFlankCards.length; i++) {
            if (hiddenFlankCards[i] != null)
                copy.hiddenFlankCards[i] = hiddenFlankCards[i].copy();
            if (fieldCards[i] != null)
                copy.fieldCards[i] = fieldCards[i].copy();
        }
        if (tieBreakers[0] != null) {
            copy.tieBreakers[0] = tieBreakers[0].copy();
            copy.tieBreakers[1] = tieBreakers[1].copy();
        }
        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // shuffle the other player's deck and hand, including the hidden flank card
            int playerToShuffle = 1 - playerId;
            copy.playerDecks.get(playerToShuffle).add(copy.playerHands.get(playerToShuffle));
            if (hiddenFlankCards[playerToShuffle] != null)
                copy.playerDecks.get(playerToShuffle).add(hiddenFlankCards[playerToShuffle]);
            // tieBreakers are always known to both players
            copy.playerHands.get(playerToShuffle).clear();
            copy.playerDecks.get(playerToShuffle).shuffle(redeterminisationRnd);
            for (int i = 0; i < playerHands.get(playerToShuffle).getSize(); i++) {
                ToadCard card = copy.playerDecks.get(playerToShuffle).draw();
                copy.playerHands.get(playerToShuffle).add(card);
            }
            if (hiddenFlankCards[playerToShuffle] != null)
                copy.hiddenFlankCards[playerToShuffle] = copy.playerDecks.get(playerToShuffle).draw();
        }
        return copy;
    }

    public Deck<ToadCard> getPlayerHand(int playerId) {
        return playerHands.get(playerId);
    }

    public void playFieldCard(int playerId, ToadCard card) {
        if (fieldCards[playerId] != null)
            throw new AssertionError("Field card already played");
        fieldCards[playerId] = card;
        if (playerHands.get(playerId).contains(card))
            playerHands.get(playerId).remove(card);
        else
            throw new AssertionError("Card not in hand");
    }

    public void playFlankCard(int playerId, ToadCard card) {
        if (hiddenFlankCards[playerId] != null)
            throw new AssertionError("Flank card already played");
        hiddenFlankCards[playerId] = card;
        if (playerHands.get(playerId).contains(card))
            playerHands.get(playerId).remove(card);
        else
            throw new AssertionError("Card not in hand");
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        // if the game is not over, our score is battles won in the current round
        if (isNotTerminal())
            return battlesWon[roundCounter][playerId];
        // otherwise we give a score of 10 for winning; 0 for losing; 5 for a tie
        // this is on the same scale of the battles won...and 10 is larger than any possible number of battles
        int[] roundsWon = new int[2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                if (battlesWon[j][i] > battlesWon[j][1 - i])
                    roundsWon[i]++;
            }
        }
        if (roundsWon[playerId] > roundsWon[1 - playerId])
            return 10;
        if (roundsWon[playerId] < roundsWon[1 - playerId])
            return 0;
        // if tied on rounds, winner of the second wins
        if (battlesWon[roundCounter][playerId] > battlesWon[roundCounter][1 - playerId])
            return 10;
        if (battlesWon[roundCounter][playerId] < battlesWon[roundCounter][1 - playerId])
            return 0;
        // otherwise we tie (and tiebreaker is used)
        return 5;
    }


    @Override
    public double getTiebreak(int playerId, int tier) {
        // TODO: Need to have battle(card, card) to obtain the actual result (and battle (card, card, card, card) for cross-effect tactics)
        return tieBreakers[playerId].value;
    }

    @Override
    public int getTiebreakLevels() {
        return 1;
    }


    @Override
    protected boolean _equals(Object o) {
        if (o instanceof ToadGameState toadGameState) {
            return playerDecks.equals(toadGameState.playerDecks) &&
                    playerHands.equals(toadGameState.playerHands) &&
                    Arrays.deepEquals(battlesWon, toadGameState.battlesWon) &&
                    playerDiscards.equals(toadGameState.playerDiscards) &&
                    Arrays.equals(hiddenFlankCards, toadGameState.hiddenFlankCards) &&
                    Arrays.equals(tieBreakers, toadGameState.tieBreakers) &&
                    Arrays.equals(fieldCards, toadGameState.fieldCards);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerDecks, playerHands, playerDiscards) + Arrays.deepHashCode(battlesWon) +
                Arrays.hashCode(hiddenFlankCards) + Arrays.hashCode(fieldCards) + Arrays.hashCode(tieBreakers);
    }

    @Override
    public String toString() {
        return super.hashCode() + "|" +
                playerDecks.hashCode() + "|" +
                playerHands.hashCode() + "|" +
                playerDiscards.hashCode() + "|" +
                Arrays.deepHashCode(battlesWon) + "|" +
                Arrays.hashCode(hiddenFlankCards) + "|" +
                Arrays.hashCode(fieldCards) + "|" +
                Arrays.hashCode(tieBreakers) + "|";
    }

}
