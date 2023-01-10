package games.sushigo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Counter;
import core.components.Deck;
import games.GameType;
import games.sushigo.cards.SGCard;

import java.util.*;

public class SGGameState extends AbstractGameState {
    List<Deck<SGCard>> playerHands;
    Deck<SGCard> drawPile;
    Deck<SGCard> discardPile;
    int nCardsInHand = 0;
    int[] playerCardPicks;
    int[] playerExtraCardPicks;
    int[] playerExtraTurns;

    HashMap<SGCard.SGCardType, Counter>[] playedCards;
    Counter[] playerScore;
    Counter[] playerWasabiAvailable;

    boolean[] playerChopsticksActivated;
    Random rnd;
    int deckRotations = 0;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - amount of players for this game.
     */
    public SGGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new SGTurnOrder(nPlayers), GameType.SushiGo);
        rnd = new Random(gameParameters.getRandomSeed());
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerHands);
            add(drawPile);
            add(discardPile);
            for (int i = 0; i < getNPlayers(); i++) {
                add(playerScore[i]);
                addAll(playedCards[i].values());
                add(playerWasabiAvailable[i]);
            }
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        SGGameState copy = new SGGameState(gameParameters.copy(), getNPlayers());
        copy.playerCardPicks = playerCardPicks.clone();
        copy.playerExtraCardPicks = playerExtraCardPicks.clone();
        copy.playerChopsticksActivated = playerChopsticksActivated.clone();
        copy.playerExtraTurns = playerExtraTurns.clone();

        copy.playerScore = new Counter[getNPlayers()];
        copy.playedCards = new HashMap[getNPlayers()];
        copy.playerWasabiAvailable = new Counter[getNPlayers()];
        for (int i = 0; i < getNPlayers(); i++) {
            copy.playerScore[i] = playerScore[i].copy();
            copy.playedCards[i] = new HashMap<>();
            for (SGCard.SGCardType ct: playedCards[i].keySet()) {
                copy.playedCards[i].put(ct, playedCards[i].get(ct).copy());
            }
            copy.playerWasabiAvailable[i] = playerWasabiAvailable[i].copy();
        }

        copy.nCardsInHand = nCardsInHand;
        copy.deckRotations = deckRotations;

        //Copy player hands
        copy.playerHands = new ArrayList<>();
        for (Deck<SGCard> d : playerHands) {
            copy.playerHands.add(d.copy());
        }

        //Other decks
        copy.drawPile = drawPile.copy();
        copy.discardPile = discardPile.copy();

        // Now we need to redeterminise
        // discard pile and player fields are known - it is just the hands of other players we need to
        // shuffle with the draw deck and then redraw
        // however we do know the contents of the hands of players to our T to our left, where
        // T is the number of player turns so far, as we saw that hand on its way through our own
        if (playerId != -1) {

            // firstly blank out the 'unseen' actions of other players
            for (int i = 0; i < getNPlayers(); i++) {
                if (i == getCurrentPlayer())
                    continue;
                copy.playerCardPicks[i] = -1;
                copy.playerExtraCardPicks[i] = -1;
                copy.playerChopsticksActivated[i] = false;
                // we will now rechoose these actions with out opponent model
                // and not have incorrect perfect information
            }

            for (int p = 0; p < copy.playerHands.size(); p++) {
                if (hasNotSeenHand(playerId, p)) {
                    copy.drawPile.add(playerHands.get(p));
                }
            }
            copy.drawPile.shuffle(rnd);
            // now we draw into the unknown player hands
            for (int p = 0; p < copy.playerHands.size(); p++) {
                if (hasNotSeenHand(playerId, p)) {
                    Deck<SGCard> hand = copy.playerHands.get(p);
                    int handSize = hand.getSize();
                    hand.clear();
                    for (int i = 0; i < handSize; i++) {
                        hand.add(copy.drawPile.draw());
                    }
                }
            }
        }

        return copy;
    }

    public boolean hasNotSeenHand(int playerId, int opponentId) {
        int opponentSpacesToLeft = opponentId - playerId;
        if (opponentSpacesToLeft < 0)
            opponentSpacesToLeft = getNPlayers() + opponentSpacesToLeft;
        return deckRotations < opponentSpacesToLeft;
    }

    public Counter[] getPlayerScore() {
        return playerScore;
    }

    public int[] getPlayerCardPicks() {
        return playerCardPicks;
    }

    public void setPlayerCardPick(int cardIndex, int playerId) {
        this.playerCardPicks[playerId] = cardIndex;
    }

    public int[] getPlayerExtraCardPicks() {
        return playerExtraCardPicks;
    }

    public void setPlayerExtraCardPick(int cardIndex, int playerId) {
        this.playerExtraCardPicks[playerId] = cardIndex;
    }

    public List<Deck<SGCard>> getPlayerHands() {
        return playerHands;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal())
            return playerScore[playerId].getValue() / 50.0;
        return getPlayerResults()[playerId].value;
    }
    @Override
    public double getTiebreak(int playerId) {
        // Tie-break is number of puddings
        return playedCards[playerId].get(SGCard.SGCardType.Pudding).getValue();
    }

    @Override
    public double getGameScore(int playerId) {
        return playerScore[playerId].getValue();
    }

    public HashMap<SGCard.SGCardType, Counter>[] getPlayedCards() {
        return playedCards;
    }

    public Counter getPlayedCards(SGCard.SGCardType cardType, int player) {
        return playedCards[player].get(cardType);
    }

    public Counter getPlayerWasabiAvailable(int playerId) {
        return playerWasabiAvailable[playerId];
    }

    public boolean getPlayerChopSticksActivated(int playerId) {
        return playerChopsticksActivated[playerId];
    }

    public int getPlayerExtraTurns(int playerId) {
        return playerExtraTurns[playerId];
    }

    public void setPlayerChopsticksActivated(int playerId, boolean value) {
        playerChopsticksActivated[playerId] = value;
    }

    public void setPlayerExtraTurns(int playerId, int value) {
        playerExtraTurns[playerId] = value;
    }


    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<Integer>() {{
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    add(playerHands.get(i).getComponentID());
                    for (Component c : playerHands.get(i).getComponents()) {
                        add(c.getComponentID());

                    }
                    add(drawPile.getComponentID());
                }
            }
        }};
    }

    @Override
    protected boolean _equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof SGGameState)) return false;
        SGGameState that = (SGGameState) o;
        return Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(drawPile, that.drawPile) &&
                Objects.equals(discardPile, that.discardPile) &&
                deckRotations == that.deckRotations &&
                Arrays.equals(playerScore, that.playerScore) &&
                Arrays.equals(playerCardPicks, that.playerCardPicks) &&
                Arrays.equals(playerExtraCardPicks, that.playerExtraCardPicks) &&
                Arrays.equals(playerWasabiAvailable, that.playerWasabiAvailable) &&
                Arrays.equals(playerChopsticksActivated, that.playerChopsticksActivated) &&
                Arrays.equals(playerExtraTurns, that.playerExtraTurns);
    }

    @Override
    public int hashCode() {
        int retValue = Objects.hash(gameParameters, turnOrder, gameStatus, gamePhase);
        retValue = 31 * retValue + Arrays.hashCode(playerResults);
        retValue = 31 * retValue + Objects.hash(nCardsInHand, playerHands, drawPile, discardPile, deckRotations);
        retValue = retValue * 31 + Arrays.hashCode(playerScore);
        retValue = retValue * 31 + Arrays.hashCode(playerExtraCardPicks);
        retValue = retValue * 31 + Arrays.hashCode(playedCards);
        retValue = retValue * 31 + Arrays.hashCode(playerWasabiAvailable);
        retValue = retValue * 31 + Arrays.hashCode(playerChopsticksActivated);
        retValue = retValue * 31 + Arrays.hashCode(playerExtraTurns);
        return retValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nCardsInHand).append("|");
        sb.append(playerHands.hashCode()).append("|");
        sb.append(drawPile.hashCode()).append("|");
        sb.append(discardPile.hashCode()).append("|");
        sb.append(deckRotations).append("|*|");
        sb.append(Arrays.hashCode(playerScore)).append("|");
        sb.append(Arrays.hashCode(playerExtraCardPicks)).append("|");
        sb.append(Arrays.hashCode(playedCards)).append("|");
        sb.append(Arrays.hashCode(playerWasabiAvailable)).append("|");
        sb.append(Arrays.hashCode(playerChopsticksActivated)).append("|");
        sb.append(Arrays.hashCode(playerExtraTurns)).append("|");

        return sb.toString();
    }
}
