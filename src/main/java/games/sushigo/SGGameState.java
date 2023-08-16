package games.sushigo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import core.interfaces.IStateFeatureJSON;
import games.GameType;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import org.json.simple.JSONObject;
import utilities.Pair;

import java.util.*;

@SuppressWarnings("unchecked")
public class SGGameState extends AbstractGameState {
    List<Deck<SGCard>> playerHands;
    Deck<SGCard> drawPile;
    Deck<SGCard> discardPile;
    int nCardsInHand = 0;

    List<List<ChooseCard>> cardChoices;  // one list per player, per turn, indicates the actions chosen by the player, saved for simultaneous execution
    HashMap<SGCard.SGCardType, Counter>[] playedCardTypes;
    List<Deck<SGCard>> playedCards;
    Counter[] playerScore;

    // For statistics, not changed between rounds
    HashMap<SGCard.SGCardType, Counter>[] playedCardTypesAllGame;
    HashMap<SGCard.SGCardType, Counter>[] pointsPerCardType;

    Random rnd;
    int deckRotations = 0;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - amount of players for this game.
     */
    public SGGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        rnd = new Random(gameParameters.getRandomSeed());
    }

    @Override
    protected GameType _getGameType() {
        return GameType.SushiGo;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerHands);
            add(drawPile);
            add(discardPile);
            addAll(playedCards);
            for (int i = 0; i < getNPlayers(); i++) {
                add(playerScore[i]);
                addAll(playedCardTypes[i].values());
            }
        }};
    }

    @Override
    protected SGGameState _copy(int playerId) {
        SGGameState copy = new SGGameState(gameParameters.copy(), getNPlayers());

        copy.playerScore = new Counter[getNPlayers()];
        copy.playedCardTypes = new HashMap[getNPlayers()];
        copy.playedCardTypesAllGame = new HashMap[getNPlayers()];
        copy.pointsPerCardType = new HashMap[getNPlayers()];
        copy.playedCards = new ArrayList<>();
        for (int i = 0; i < getNPlayers(); i++) {
            copy.playedCards.add(playedCards.get(i).copy());
            copy.playerScore[i] = playerScore[i].copy();
            copy.playedCardTypes[i] = new HashMap<>();
            copy.playedCardTypesAllGame[i] = new HashMap<>();
            copy.pointsPerCardType[i] = new HashMap<>();
            for (SGCard.SGCardType ct : playedCardTypes[i].keySet()) {
                copy.playedCardTypes[i].put(ct, playedCardTypes[i].get(ct).copy());
                copy.playedCardTypesAllGame[i].put(ct, playedCardTypesAllGame[i].get(ct).copy());
                copy.pointsPerCardType[i].put(ct, pointsPerCardType[i].get(ct).copy());
            }
        }

        copy.nCardsInHand = nCardsInHand;
        copy.deckRotations = deckRotations;

        // Copy player hands
        copy.playerHands = new ArrayList<>();
        for (Deck<SGCard> d : playerHands) {
            copy.playerHands.add(d.copy());
        }

        // Other decks
        copy.drawPile = drawPile.copy();
        copy.discardPile = discardPile.copy();
        copy.cardChoices = new ArrayList<>();

        if (playerId == -1) {
            for (int i = 0; i < getNPlayers(); i++) {
                List<ChooseCard> copiedItems = new ArrayList<>();
                for (ChooseCard cc : cardChoices.get(i)) {
                    copiedItems.add(cc.copy());
                }
                copy.cardChoices.add(copiedItems);
            }
        } else {
            // Now we need to redeterminise
            // We need to shuffle the hands of other players with the draw deck and then redraw

            // Add player hands unseen back to the draw pile
            for (int p = 0; p < copy.playerHands.size(); p++) {
                if (hasNotSeenHand(playerId, p)) {
                    copy.drawPile.add(playerHands.get(p));
                }
            }
            copy.drawPile.shuffle(rnd);

            // Now we draw into the unknown player hands
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

            // We don't know what other players have chosen for this round, hide card choices
            turnOwner = playerId;
            for (int i = 0; i < getNPlayers(); i++) {
                copy.cardChoices.add(new ArrayList<>());
                if (i == playerId) {
                    for (ChooseCard cc : cardChoices.get(i)) {
                        copy.cardChoices.get(i).add(cc.copy());
                    }
                }
            }
        }

        return copy;
    }

    /**
     * we do know the contents of the hands of players up to T to our left, where T is the number of player turns
     * so far, as we saw that hand on its way through our own
     *
     * @param playerId   - id of player whose vision we're checking
     * @param opponentId - id of opponent owning the hand of cards we're checking vision of
     * @return - true if player has not seen the opponent's hand of cards, false otherwise
     */
    public boolean hasNotSeenHand(int playerId, int opponentId) {
        if (playerId == opponentId) return false;
        int opponentSpacesToLeft = opponentId - playerId;
        if (opponentSpacesToLeft < 0)
            opponentSpacesToLeft = getNPlayers() + opponentSpacesToLeft;
        return deckRotations < opponentSpacesToLeft;
    }

    public Counter[] getPlayerScore() {
        return playerScore;
    }

    public void addPlayerScore(int p, int amount, SGCard.SGCardType fromType) {
        playerScore[p].increment(amount);
        pointsPerCardType[p].get(fromType).increment(amount);
    }

    public List<Deck<SGCard>> getPlayerHands() {
        return playerHands;
    }

    public void clearCardChoices() {
        for (int i = 0; i < getNPlayers(); i++) cardChoices.get(i).clear();
    }

    public void addCardChoice(ChooseCard chooseCard, int playerId) {
        cardChoices.get(playerId).add(chooseCard);
    }

    public List<List<ChooseCard>> getCardChoices() {
        return cardChoices;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal())
            return playerScore[playerId].getValue() / 50.0;
        return getPlayerResults()[playerId].value;
    }

    @Override
    public double getTiebreak(int playerId, int tier) {
        // Tie-break is number of puddings
        return playedCardTypes[playerId].get(SGCard.SGCardType.Pudding).getValue();
    }

    @Override
    public double getGameScore(int playerId) {
        return playerScore[playerId].getValue();
    }

    public HashMap<SGCard.SGCardType, Counter>[] getPlayedCardTypes() {
        return playedCardTypes;
    }

    public HashMap<SGCard.SGCardType, Counter>[] getPlayedCardTypesAllGame() {
        return playedCardTypesAllGame;
    }

    public HashMap<SGCard.SGCardType, Counter>[] getPointsPerCardType() {
        return pointsPerCardType;
    }

    public Counter getPlayedCardTypes(SGCard.SGCardType cardType, int player) {
        return playedCardTypes[player].get(cardType);
    }

    public List<Deck<SGCard>> getPlayedCards() {
        return playedCards;
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
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SGGameState)) return false;
        if (!super.equals(o)) return false;
        SGGameState that = (SGGameState) o;
        return nCardsInHand == that.nCardsInHand && deckRotations == that.deckRotations &&
                Objects.equals(playerHands, that.playerHands) && Objects.equals(drawPile, that.drawPile) &&
                Objects.equals(discardPile, that.discardPile) && Objects.equals(cardChoices, that.cardChoices) &&
                Arrays.equals(playedCardTypes, that.playedCardTypes) && Objects.equals(playedCards, that.playedCards) &&
                Arrays.equals(playerScore, that.playerScore) && Arrays.equals(playedCardTypesAllGame, that.playedCardTypesAllGame) &&
                Objects.equals(rnd, that.rnd);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(playerHands, drawPile, discardPile,
                nCardsInHand, cardChoices, playedCards, deckRotations);
        result = 31 * result + Arrays.hashCode(playedCardTypes);
        result = 31 * result + Arrays.hashCode(playerScore);
        result = 31 * result + Arrays.hashCode(playedCardTypesAllGame);
        return result;
    }

    @Override
    public String toString() {
        return nCardsInHand + "|" +
                playerHands.hashCode() + "|" +
                drawPile.hashCode() + "|" +
                discardPile.hashCode() + "|" +
                cardChoices.hashCode() + "|" +
                playedCards.hashCode() + "|" +
                deckRotations + "|*|" +
                Arrays.hashCode(playerScore) + "|" +
                Arrays.hashCode(playedCardTypes) + "|" +
                Arrays.hashCode(playedCardTypesAllGame) + "|" +
                super.hashCode() + "|";
    }

}
