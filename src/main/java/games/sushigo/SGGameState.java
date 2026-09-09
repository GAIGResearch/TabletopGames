package games.sushigo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import games.GameType;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;

import java.util.*;

@SuppressWarnings("unchecked")
public class SGGameState extends AbstractGameState {
    List<Deck<SGCard>> playerHands;
    Deck<SGCard> drawPile;
    Deck<SGCard> discardPile;
    int nCardsInHand = 0;

    List<List<ChooseCard>> cardChoices;  // one list per player, per turn, indicates the actions chosen by the player, saved for simultaneous execution
    Map<SGCard.SGCardType, Counter>[] playedCardTypes;
    List<Deck<SGCard>> playedCards;
    Counter[] playerScore;

    // For statistics, not changed between rounds
    HashMap<SGCard.SGCardType, Counter>[] playedCardTypesAllGame;
    HashMap<SGCard.SGCardType, Counter>[] pointsPerCardType;

    int deckRotations = 0;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - amount of players for this game.
     */
    public SGGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    public List<Integer> getCurrentSimultaneousPlayers() {
        // TODO: This should really be intercepted at the AbstractLevel, given that has ownership of actionsInProgress
        // TODO: and avoid future implementations missing this check
        if (isActionInProgress()) {
            return super.getCurrentSimultaneousPlayers();
        }
        if (playerHands == null) {
            throw new AssertionError("No player has a hand");
        }
        // only report players who still have to choose this turn. a player who has already
        // committed a card must not be asked again, or they end up playing two cards.
        // A plain loop rather than a stream: the decoupled tree search calls this on every node visit
        // and every forward model call, so the pipeline set-up shows up in profiles.
        List<Integer> toDecide = new ArrayList<>(getNPlayers());
        for (int p = 0; p < getNPlayers(); p++)
            if (cardChoices.get(p).isEmpty())
                toDecide.add(p);
        if (toDecide.isEmpty()) {
            // everyone has chosen, so the cards should already have been revealed and the
            // choices cleared. if we get here the turn cycle is broken, so say so loudly.
            throw new AssertionError("All players have chosen but the turn has not been resolved");
        }
        return toDecide;
    }

    @Override
    protected GameType _getGameType() {
        return GameType.SushiGo;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>() {{
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

        for (int i = 0; i < getNPlayers(); i++) {
            List<ChooseCard> copiedItems = new ArrayList<>();
            for (ChooseCard cc : cardChoices.get(i)) {
                copiedItems.add(cc.copy());
            }
            copy.cardChoices.add(copiedItems);
        }
        return copy;
    }

    @Override
    public void redeterminise(int playerId) {
        // We need to shuffle the hands of other players with the draw deck and then redraw

        // Add player hands unseen back to the draw pile
        for (int p = 0; p < playerHands.size(); p++) {
            if (!isHandKnown(playerId, p)) {
                drawPile.add(playerHands.get(p));
            }
        }
        drawPile.shuffle(redeterminisationRnd);

        // Now we draw into the unknown player hands
        for (int p = 0; p < playerHands.size(); p++) {
            if (!isHandKnown(playerId, p)) {
                Deck<SGCard> hand = playerHands.get(p);
                int handSize = hand.getSize();
                hand.clear();
                for (int i = 0; i < handSize; i++) {
                    hand.add(drawPile.draw());
                }
            }
        }

        // we have to set the turn owner so that getCurrentPlayer() returns the correct value
        // all players think they are the current player when picking actions simultaneously
        setTurnOwner(playerId);
        // hide cardChoices (if made) of all other players, but keep our own:
        // getCurrentSimultaneousPlayers() decides who still has to move from cardChoices being empty,
        // so losing our own choice here would have us asked to play a second card.
        List<List<ChooseCard>> oldChoices = cardChoices;
        cardChoices = new ArrayList<>();
        for (int i = 0; i < getNPlayers(); i++) {
            cardChoices.add(new ArrayList<>());
            if (i == playerId) {
                for (ChooseCard cc : oldChoices.get(i)) {
                    cardChoices.get(i).add(cc.copy());
                }
            }
        }
    }

    /**
     * we know the contents of the hands of the players that are deckRotations spaces to the left of the current player
     * if this returns true, then the information provided by the playerHands is reliably correct (if false, then this information is shuffled)
     */
    public boolean isHandKnown(int playerId, int opponentId) {
        // Player 0 is one space to the 'Left' of player 1
        int opponentSpacesToLeft = (playerId - opponentId + getNPlayers()) % getNPlayers();
        return opponentSpacesToLeft <= deckRotations;
    }

    public Counter[] getPlayerScore() {
        return playerScore;
    }

    public void addPlayerScore(int p, int amount, SGCard.SGCardType fromType) {
        playerScore[p].increment(amount);
        pointsPerCardType[p].get(fromType).increment(amount);
    }

    /**
     * Returns a List of player hands. This is ordered by player ID.
     */
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
    /**
     * Tie break is the number of puddings
     */
    public double getTiebreak(int playerId, int tier) {
        // Tie-break is number of puddings
        return playedCardTypes[playerId].get(SGCard.SGCardType.Pudding).getValue();
    }

    @Override
    public double getGameScore(int playerId) {
        return playerScore[playerId].getValue();
    }

    /**
     * Returns a Map from CardType to the number of times that card type has been played by the player.
     * This only includes the current round.
     */
    public Map<SGCard.SGCardType, Counter>[] getPlayedCardTypes() {
        return playedCardTypes;
    }

    /**
     * Returns a Map from CardType to the number of times that card type has been played by the player.
     * This is over the whole game.
     */
    public Map<SGCard.SGCardType, Counter>[] getPlayedCardTypesAllGame() {
        return playedCardTypesAllGame;
    }

    public Map<SGCard.SGCardType, Counter>[] getPointsPerCardType() {
        return pointsPerCardType;
    }

    /**
     * Returns the number of times a card type has been played by a player in the current round.
     * (Value returned in a Counter object)
     */
    public Counter getPlayedCardTypes(SGCard.SGCardType cardType, int player) {
        return playedCardTypes[player].get(cardType);
    }

    /**
     * The Deck of all played cards
     */
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
                Arrays.equals(playerScore, that.playerScore) && Arrays.equals(playedCardTypesAllGame, that.playedCardTypesAllGame);
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
