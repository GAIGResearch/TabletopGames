package games.scarto;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.TarotCard;
import games.GameType;
import games.tricktaking.ITrickTakingState;
import games.tricktaking.KnownVoids;
import games.tricktaking.Trick;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * <p>Components tracked:</p>
 * <ul>
 *     <li>playerHands - one Deck per player, VISIBLE_TO_OWNER</li>
 *     <li>currentTrick - the trick in progress, VISIBLE_TO_ALL</li>
 *     <li>cardsWon - the cards each player has captured this deal, and the Fool if they played it. VISIBLE_TO_ALL,
 *     as every card in it was seen in a trick</li>
 *     <li>scarto - the cards left over after the deal, which will score for the dealer: HIDDEN_TO_ALL with no owner.
 *     With ScartoParameters.dealerExchange, the dealer's discards instead, owned by the dealer and
 *     VISIBLE_TO_OWNER</li>
 *     <li>knownVoids - the suits each player is publicly known not to hold this deal</li>
 *     <li>bankedScores - the points each player scored in the deals already completed</li>
 * </ul>
 */
public class ScartoGameState extends AbstractGameState implements ITrickTakingState<TarotCard, TarotCard.Suit> {

    List<Deck<TarotCard>> playerHands;
    Trick<TarotCard, TarotCard.Suit> currentTrick;
    List<Deck<TarotCard>> cardsWon;
    Deck<TarotCard> scarto;
    KnownVoids<TarotCard.Suit> knownVoids;
    int[] bankedScores;

    public ScartoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Scarto;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerHands);
        components.add(currentTrick);
        components.addAll(cardsWon);
        components.add(scarto);
        return components;
    }

    @Override
    public Deck<TarotCard> getPlayerHand(int player) {
        return playerHands.get(player);
    }

    @Override
    public Trick<TarotCard, TarotCard.Suit> getCurrentTrick() {
        return currentTrick;
    }

    @Override
    public KnownVoids<TarotCard.Suit> getKnownVoids() {
        return knownVoids;
    }

    @Override
    public void recordVoids(int player, TarotCard card) {
        // a player who does not follow suit has none of the suit led, and unless they play a trump, no trumps either
        knownVoids.recordMustTrump(player, currentTrick, card, TarotCard.Suit.Trumps);
    }

    public Deck<TarotCard> getCardsWon(int player) {
        return cardsWon.get(player);
    }

    public Deck<TarotCard> getScarto() {
        return scarto;
    }

    public int getBankedScore(int player) {
        return bankedScores[player];
    }

    /**
     * Whether the dealer has taken the scarto into their hand (ScartoParameters.dealerExchange) and not yet
     * discarded as many cards.
     */
    public boolean isExchanging() {
        ScartoParameters params = (ScartoParameters) gameParameters;
        return params.dealerExchange && playerHands.get(getDealer()).getSize() > params.handSize;
    }

    public int getDealer() {
        // the last player deals first, then the deal passes to the next player
        return (getRoundCounter() + getNPlayers() - 1) % getNPlayers();
    }

    @Override
    protected ScartoGameState _copy(int playerId) {
        ScartoGameState copy = new ScartoGameState(gameParameters, getNPlayers());
        copy.playerHands = new ArrayList<>();
        for (Deck<TarotCard> hand : playerHands)
            copy.playerHands.add(hand.copy());
        copy.currentTrick = currentTrick.copy();
        copy.cardsWon = new ArrayList<>();
        for (Deck<TarotCard> won : cardsWon)
            copy.cardsWon.add(won.copy());
        copy.scarto = scarto.copy();
        copy.knownVoids = knownVoids.copy();
        copy.bankedScores = bankedScores.clone();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // the other players' hands and the scarto are unknown. No player is given a card of a suit they are known
            // to be void in; the scarto is exempt, as what the dealer is void in says nothing about it
            List<Deck<TarotCard>> hidden = new ArrayList<>(copy.playerHands);
            hidden.add(copy.scarto);
            BiPredicate<Deck<TarotCard>, TarotCard> notVoid = knownVoids.permits(ScartoCardOrder.INSTANCE);
            DeterminisationUtilities.reshuffle(playerId, hidden, c -> true, redeterminisationRnd,
                    ((ScartoParameters) gameParameters).rememberVoids
                            ? (deck, card) -> deck == copy.scarto || notVoid.test(deck, card) : null);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        // the score as a fraction of all the points in the deals played so far, this one included
        return getGameScore(playerId) / (((ScartoParameters) gameParameters).pointsPerDeal() * (getRoundCounter() + 1));
    }

    /**
     * The points of the cards the player has captured this deal (with the scarto, for the dealer).
     */
    public int getDealScore(int playerId) {
        List<TarotCard> pile = new ArrayList<>(cardsWon.get(playerId).getComponents());
        if (playerId == getDealer())
            pile.addAll(scarto.getComponents());
        return ((ScartoParameters) gameParameters).pilePoints(pile);
    }

    /**
     * The points banked from the deals completed, plus those of this deal.
     */
    @Override
    public double getGameScore(int playerId) {
        return bankedScores[playerId] + getDealScore(playerId);
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScartoGameState that)) return false;
        return Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(currentTrick, that.currentTrick) &&
                Objects.equals(cardsWon, that.cardsWon) &&
                Objects.equals(scarto, that.scarto) &&
                Objects.equals(knownVoids, that.knownVoids) &&
                Arrays.equals(bankedScores, that.bankedScores);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, currentTrick, cardsWon, scarto, knownVoids)
                + 31 * Arrays.hashCode(bankedScores);
    }
}
