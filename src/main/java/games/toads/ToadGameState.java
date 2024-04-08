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
    int[] battlesWon;
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
        copy.battlesWon = Arrays.copyOf(battlesWon, battlesWon.length);
        copy.hiddenFlankCards = new ToadCard[hiddenFlankCards.length];
        copy.fieldCards = new ToadCard[fieldCards.length];

        for (int i = 0; i < hiddenFlankCards.length; i++) {
            if (hiddenFlankCards[i] != null)
                copy.hiddenFlankCards[i] = hiddenFlankCards[i].copy();
            if (fieldCards[i] != null)
                copy.fieldCards[i] = fieldCards[i].copy();
            if (tieBreakers[i] != null)
                copy.tieBreakers[i] = tieBreakers[i].copy();
        }
        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // shuffle the other player's deck and hand, including the hidden flank card
            int playerToShuffle = playerId == 0 ? 1 : 0;
            copy.playerDecks.get(playerToShuffle).add(copy.playerHands.get(playerToShuffle));
            if (hiddenFlankCards[playerToShuffle] != null)
                copy.playerDecks.get(playerToShuffle).add(hiddenFlankCards[playerToShuffle]);
            // tieBreakers are always known to both players
            copy.playerHands.clear();
            copy.playerDecks.get(playerToShuffle).shuffle(redeterminisationRnd);
            for (int i = 0; i < params.handSize; i++) {
                ToadCard card = copy.playerDecks.get(playerToShuffle).draw();
                copy.playerHands.get(playerToShuffle).add(card);
            }
            if (hiddenFlankCards[playerToShuffle] != null)
                copy.hiddenFlankCards[playerToShuffle] = copy.playerDecks.get(playerToShuffle).draw();
        }
        return copy;
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
        return battlesWon[playerId];
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof ToadGameState toadGameState) {
            return playerDecks.equals(toadGameState.playerDecks) &&
                    playerHands.equals(toadGameState.playerHands) &&
                    Arrays.equals(battlesWon, toadGameState.battlesWon) &&
                    playerDiscards.equals(toadGameState.playerDiscards) &&
                    Arrays.equals(hiddenFlankCards, toadGameState.hiddenFlankCards) &&
                    Arrays.equals(tieBreakers, toadGameState.tieBreakers) &&
                    Arrays.equals(fieldCards, toadGameState.fieldCards);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerDecks, playerHands, playerDiscards) + 31 * battlesWon[0] + 31 * 31 * battlesWon[1] +
                Arrays.hashCode(hiddenFlankCards) + Arrays.hashCode(fieldCards) + Arrays.hashCode(tieBreakers);
    }

    // TODO: If your game has multiple special tiebreak options, then implement the next two methods.
    // TODO: The default is to tie-break on the game score (if this is the case, ignore these)
    // public double getTiebreak(int playerId, int tier);
    // public int getTiebreakLevels();

}
