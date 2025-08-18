package games.toads;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.GameType;
import games.toads.components.ToadCard;
import utilities.DeterminisationUtilities;

import java.util.*;


public class ToadGameState extends AbstractGameState {


    public ToadGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    List<PartialObservableDeck<ToadCard>> playerDecks;
    List<PartialObservableDeck<ToadCard>> playerHands;
    int discardOptions;
    int[][] battlesWon;
    int[] battlesTied;
    int nextBattle = 0;
    protected int[][] roundWinners;
    List<Deck<ToadCard>> playerDiscards;
    ToadCard[] hiddenFlankCards;
    ToadCard[] fieldCards;
    ToadCard[] tieBreakers;
    Set<ToadConstants.ToadCardType> cardTypesInPlay;

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
        components.addAll(playerDiscards.get(0).stream().toList());
        components.addAll(playerDiscards.get(1).stream().toList());
        if (hiddenFlankCards[0] != null)
            components.add(hiddenFlankCards[0]);
        if (hiddenFlankCards[1] != null)
            components.add(hiddenFlankCards[1]);
        if (fieldCards[0] != null)
            components.add(fieldCards[0]);
        if (fieldCards[1] != null)
            components.add(fieldCards[1]);
        if (tieBreakers[0] != null)
            components.add(tieBreakers[0]);
        if (tieBreakers[1] != null)
            components.add(tieBreakers[1]);
        return components;
    }

    @Override
    protected ToadGameState _copy(int playerId) {
        ToadParameters params = (ToadParameters) this.gameParameters;
        ToadGameState copy = new ToadGameState(params.shallowCopy(), getNPlayers());
        copy.playerDecks = new ArrayList<>();
        for (PartialObservableDeck<ToadCard> deck : playerDecks) {
            copy.playerDecks.add(deck.copy());
        }
        copy.playerHands = new ArrayList<>();
        for (PartialObservableDeck<ToadCard> hand : playerHands) {
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
        copy.battlesTied = Arrays.copyOf(battlesTied, 2);
        copy.discardOptions = discardOptions;
        copy.cardTypesInPlay = cardTypesInPlay; // this is immutable
        copy.nextBattle = nextBattle;

        // battlesWon tracks the win/loss rates over all 8 Battles
        copy.roundWinners = new int[8][2];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 2; j++) {
                copy.roundWinners[i][j] = roundWinners[i][j];
            }
        }

        copy.hiddenFlankCards = new ToadCard[hiddenFlankCards.length];
        copy.fieldCards = new ToadCard[fieldCards.length];
        copy.tieBreakers = new ToadCard[tieBreakers.length];

        for (int i = 0; i < hiddenFlankCards.length; i++) {
            if (hiddenFlankCards[i] != null)
                copy.hiddenFlankCards[i] = hiddenFlankCards[i].copy();
        }
        for (int i = 0; i < fieldCards.length; i++) {
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
//            if (hiddenFlankCards[playerToShuffle] != null)
//                copy.playerDecks.get(playerToShuffle).add(hiddenFlankCards[playerToShuffle]);
            DeterminisationUtilities.reshuffle(playerId,
                    List.of(
                            copy.playerDecks.get(playerToShuffle),
                            copy.playerHands.get(playerToShuffle)
                    ),
                    i -> true,  // no special conditions
                    redeterminisationRnd);
            // then put hidden flank card back
            if (hiddenFlankCards[playerToShuffle] != null) {
                int deckSize = copy.playerHands.get(playerToShuffle).getSize();
                copy.hiddenFlankCards[playerToShuffle] = copy.playerHands.get(playerToShuffle).peek(redeterminisationRnd.nextInt(deckSize));
            }
            // and their tiebreaker is shuffled with *our* as yet undrawn deck
            if (tieBreakers[playerToShuffle] != null)
                copy.playerDecks.get(playerId).add(tieBreakers[playerToShuffle]);
            copy.playerDecks.get(playerId).shuffle(redeterminisationRnd);
            if (tieBreakers[playerToShuffle] != null)
                copy.tieBreakers[playerToShuffle] = copy.playerDecks.get(playerId).draw();
        }
        return copy;
    }

    public PartialObservableDeck<ToadCard> getPlayerHand(int playerId) {
        return playerHands.get(playerId);
    }

    public PartialObservableDeck<ToadCard> getPlayerDeck(int playerId) {
        return playerDecks.get(playerId);
    }

    public void seeOpponentsHand(int player, ToadConstants.ToadCardType exception) {
        PartialObservableDeck<ToadCard> handToSee = playerHands.get(1 - player);
        for (int i = 0; i < handToSee.getSize(); i++) {
            if (exception != null && handToSee.peek(i).type == exception)
                continue;
            handToSee.setVisibilityOfComponent(i, player, true);
        }
    }

    public Set<ToadConstants.ToadCardType> getCardTypesInPlay() {
        return cardTypesInPlay;
    }

    public ToadCard getFieldCard(int playerId) {
        return fieldCards[playerId];
    }

    public ToadCard getHiddenFlankCard(int playerId) {
        return hiddenFlankCards[playerId];
    }

    public ToadCard getTieBreaker(int playerId) {
        return tieBreakers[playerId];
    }
    public int getBattlesWon(int round, int playerId) {
        return battlesWon[round][playerId];
    }
    public int getBattlesTied(int round) {
        return battlesTied[round];
    }
    public int getScoreInBattle(int battle, int playerId) {
        return roundWinners[battle][playerId];
    }

    public Deck<ToadCard> getDiscards(int playerId) {
        return playerDiscards.get(playerId);
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
        if (!playerHands.get(playerId).contains(card))
            throw new AssertionError("Card not in hand");
    }

    public void revealFlankCards() {
        for (int i = 0; i < 2; i++) {
            if (hiddenFlankCards[i] != null) {
                playerHands.get(i).remove(hiddenFlankCards[i]);
            }
        }
    }
    public void unsetHiddenFlankCard(int playerId) {
        hiddenFlankCards[playerId] = null;
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
        if (tieBreakers[playerId] == null)
            return 0;
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
                    discardOptions == toadGameState.discardOptions &&
                    nextBattle == toadGameState.nextBattle &&
                    Arrays.deepEquals(battlesWon, toadGameState.battlesWon) &&
                    playerDiscards.equals(toadGameState.playerDiscards) &&
                    Arrays.equals(hiddenFlankCards, toadGameState.hiddenFlankCards) &&
                    Arrays.equals(tieBreakers, toadGameState.tieBreakers) &&
                    Arrays.equals(battlesTied, toadGameState.battlesTied) &&
                    Arrays.equals(fieldCards, toadGameState.fieldCards);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerDecks, playerHands, playerDiscards, discardOptions, nextBattle) + Arrays.deepHashCode(battlesWon) +
                Arrays.hashCode(hiddenFlankCards) + Arrays.hashCode(fieldCards) + Arrays.hashCode(tieBreakers) + Arrays.hashCode(battlesTied);
    }

    @Override
    public String toString() {
        return super.hashCode() + "|" +
                playerDecks.hashCode() + "|" +
                playerHands.hashCode() + "|" +
                playerDiscards.hashCode() + "|" +
                discardOptions + nextBattle * 31 + "|" +
                Arrays.deepHashCode(battlesWon) + "|" +
                Arrays.hashCode(battlesTied) + "|" +
                Arrays.hashCode(hiddenFlankCards) + "|" +
                Arrays.hashCode(fieldCards) + "|" +
                Arrays.hashCode(tieBreakers) + "|";
    }

}
