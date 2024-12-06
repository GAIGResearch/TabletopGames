package games.explodingkittens;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.GameType;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExplodingKittensGameState extends AbstractGameState {

    // Cards in each player's hand, index corresponds to player ID
    List<PartialObservableDeck<ExplodingKittensCard>> playerHandCards;
    // Cards in the draw pile
    PartialObservableDeck<ExplodingKittensCard> drawPile;
    // Cards in the discard pile
    Deck<ExplodingKittensCard> discardPile;
    Deck<ExplodingKittensCard> inPlay;
    int currentPlayerTurnsLeft = 0;
    int nextAttackLevel = 0;
    boolean skip = false;

    int[] orderOfPlayerDeath;

    public ExplodingKittensGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.ExplodingKittens;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> ret = new ArrayList<>();
        ret.add(drawPile);
        ret.add(discardPile);
        ret.addAll(playerHandCards);
        return ret;
    }

    // when a card is played to the table, but before
    public void setInPlay(ExplodingKittensCard.CardType cardType, int playerID) {
        ExplodingKittensCard card = playerHandCards.get(playerID).stream()
                .filter(c -> c.cardType == cardType)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Player " + playerID + " does not have card " + cardType + " to play"));
        inPlay.add(card);
        playerHandCards.get(playerID).remove(card);
    }

    @Override
    protected ExplodingKittensGameState _copy(int playerId) {
        ExplodingKittensGameState ekgs = new ExplodingKittensGameState(gameParameters.copy(), getNPlayers());
        ekgs.discardPile = discardPile.copy();
        ekgs.currentPlayerTurnsLeft = currentPlayerTurnsLeft;
        ekgs.nextAttackLevel = nextAttackLevel;
        ekgs.inPlay = inPlay.copy();
        ekgs.orderOfPlayerDeath = orderOfPlayerDeath.clone();
        ekgs.playerHandCards = new ArrayList<>();
        for (PartialObservableDeck<ExplodingKittensCard> d : playerHandCards) {
            ekgs.playerHandCards.add(d.copy());
        }
        ekgs.drawPile = drawPile.copy();
        if (getCoreGameParameters().partialObservable && playerId != -1) {
            // Other player hands + draw deck are hidden, combine in draw pile and shuffle
            // Note: this considers the agent to track opponent's cards that are known to him by itself
            // e.g. in case the agent has previously given a favor card to its opponent
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    // Take all cards the player can't see from other players and put them in the draw pile.
                    ArrayList<ExplodingKittensCard> cs = new ArrayList<>();
                    for (int j = 0; j < ekgs.playerHandCards.get(i).getSize(); j++) {
                        if (!ekgs.playerHandCards.get(i).isComponentVisible(j, playerId)) {
                            ExplodingKittensCard c = ekgs.playerHandCards.get(i).get(j);
                            ekgs.drawPile.add(c, ekgs.playerHandCards.get(i).getVisibilityOfComponent(j).clone());
                            cs.add(c);
                        }
                    }
                    for (ExplodingKittensCard c : cs) {
                        ekgs.playerHandCards.get(i).remove(c);
                    }
                }
            }

            // Shuffles only hidden cards in draw pile, if player knows what's on top those will stay in place
            ekgs.drawPile.redeterminiseUnknown(redeterminisationRnd, playerId);
            Deck<ExplodingKittensCard> explosive = new Deck<>("tmp", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    for (int j = 0; j < playerHandCards.get(i).getSize(); j++) {
                        // Add back random cards for all components not visible to this player
                        if (playerHandCards.get(i).isComponentVisible(j, playerId)) continue;
                        boolean added = false;
                        int cardIndex = 0;
                        while (!added) {
                            // if the card is visible to the player we cannot move it somewhere else
                            if (ekgs.drawPile.getVisibilityForPlayer(cardIndex, playerId)) {
                                cardIndex++;
                                continue;
                            }
                            ExplodingKittensCard card = ekgs.drawPile.pick(cardIndex);
                            if (card.cardType != ExplodingKittensCard.CardType.EXPLODING_KITTEN) {
                                ekgs.playerHandCards.get(i).add(card);
                                added = true;
                            } else {
                                explosive.add(card);
                            }
                        }
                    }
                }
            }
            ekgs.drawPile.add(explosive);
        }
        return ekgs;
    }

    public void setNextAttackLevel(int value) {
        nextAttackLevel = value;
    }

    public int getCurrentPlayerTurnsLeft() {
        return currentPlayerTurnsLeft;
    }
    public void setCurrentPlayerTurnsLeft(int value) {
        currentPlayerTurnsLeft = value;
    }

    public boolean skipNext() {
        return skip;
    }
    public void setSkip(boolean skip) {
        this.skip = skip;
    }
    @Override
    protected double _getHeuristicScore(int playerId) {
        return new ExplodingKittensHeuristic().evaluateState(this, playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        if (playerResults[playerId] == CoreConstants.GameResult.LOSE_GAME)
            // knocked out
            return orderOfPlayerDeath[playerId];
        // otherwise our current score is the number knocked out + 1
        return Arrays.stream(playerResults).filter(status -> status == CoreConstants.GameResult.LOSE_GAME).count() + 1;
    }


    public PartialObservableDeck<ExplodingKittensCard> getPlayerHand(int playerId) {
        return playerHandCards.get(playerId);
    }
    public PartialObservableDeck<ExplodingKittensCard> getDrawPile() {
        return drawPile;
    }
    public Deck<ExplodingKittensCard> getDiscardPile() {
        return discardPile;
    }
    public Deck<ExplodingKittensCard> getInPlay() {
        return inPlay;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof ExplodingKittensGameState other) {
            return discardPile.equals(other.discardPile) &&
                    Arrays.equals(orderOfPlayerDeath, other.orderOfPlayerDeath) &&
                    playerHandCards.equals(other.playerHandCards) &&
                    currentPlayerTurnsLeft == other.currentPlayerTurnsLeft &&
                    nextAttackLevel == other.nextAttackLevel &&
                    inPlay.equals(other.inPlay) &&
                    drawPile.equals(other.drawPile);
        };
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(discardPile, playerHandCards, drawPile, inPlay, nextAttackLevel, currentPlayerTurnsLeft) + 31 * Arrays.hashCode(orderOfPlayerDeath);
    }

}
