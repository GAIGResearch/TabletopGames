package games.golfsix;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Game state for Six-card Golf. Data-only: all initialisation and rule logic lives in
 * {@link GolfSixForwardModel}.</p>
 *
 * <p>Components tracked:</p>
 * <ul>
 *     <li>grids - one PartialObservableDeck per player, always holding GolfSixParameters.GRID_SIZE cards. The index
 *     of a card is its grid position. A face-down card is visible to nobody, its owner included; a face-up card is
 *     visible to all</li>
 *     <li>drawDeck - HIDDEN_TO_ALL</li>
 *     <li>discardPile - VISIBLE_TO_ALL; index 0 is the top card</li>
 *     <li>drawnCard - the card the current player has drawn and not yet placed, if any. It is visible to all if it
 *     came from the discard pile, and otherwise only to the current player</li>
 *     <li>drawnFromDiscard - whether drawnCard came from the discard pile</li>
 *     <li>finisher - the player who turned up the last card of their grid this deal, or -1</li>
 *     <li>scores - the points each player has scored in the deals completed so far</li>
 * </ul>
 */
public class GolfSixGameState extends AbstractGameState {

    List<PartialObservableDeck<FrenchCard>> grids;
    Deck<FrenchCard> drawDeck;
    Deck<FrenchCard> discardPile;
    PartialObservableDeck<FrenchCard> drawnCard;
    boolean drawnFromDiscard;
    int finisher;
    int[] scores;

    public GolfSixGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.GolfSix;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(grids);
        components.add(drawDeck);
        components.add(discardPile);
        components.add(drawnCard);
        return components;
    }

    public List<PartialObservableDeck<FrenchCard>> getGrids() {
        return grids;
    }

    public PartialObservableDeck<FrenchCard> getGrid(int playerId) {
        return grids.get(playerId);
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<FrenchCard> getDiscardPile() {
        return discardPile;
    }

    /**
     * The card the current player has drawn and not yet placed, or null.
     */
    public FrenchCard getDrawnCard() {
        return drawnCard.getSize() == 0 ? null : drawnCard.get(0);
    }

    public PartialObservableDeck<FrenchCard> getDrawnCardDeck() {
        return drawnCard;
    }

    public boolean isDrawnFromDiscard() {
        return drawnFromDiscard;
    }

    public void setDrawnFromDiscard(boolean drawnFromDiscard) {
        this.drawnFromDiscard = drawnFromDiscard;
    }

    public boolean isFaceUp(int playerId, int position) {
        // a face-up card is visible to every player and a face-down card to none, so the owner's view is enough
        return grids.get(playerId).isComponentVisible(position, playerId);
    }

    public int faceUpCount(int playerId) {
        int count = 0;
        for (int pos = 0; pos < GolfSixParameters.GRID_SIZE; pos++)
            if (isFaceUp(playerId, pos)) count++;
        return count;
    }

    public boolean allFaceUp(int playerId) {
        return faceUpCount(playerId) == GolfSixParameters.GRID_SIZE;
    }

    /**
     * The player who turned up the last card of their grid this deal, or -1 if nobody has yet.
     */
    public int getFinisher() {
        return finisher;
    }

    /**
     * The dealer of the current deal. The first deal is dealt by the last player, so player 0 plays first, and the
     * deal then passes to the next player.
     */
    public int getDealer() {
        return (getRoundCounter() + getNPlayers() - 1) % getNPlayers();
    }

    /**
     * The points scored in the deals completed so far.
     */
    public int getScore(int playerId) {
        return scores[playerId];
    }

    @Override
    protected GolfSixGameState _copy(int playerId) {
        GolfSixGameState copy = new GolfSixGameState(gameParameters, getNPlayers());
        copy.grids = new ArrayList<>();
        for (PartialObservableDeck<FrenchCard> grid : grids)
            copy.grids.add(grid.copy());
        copy.drawDeck = drawDeck.copy();
        copy.discardPile = discardPile.copy();
        copy.drawnCard = drawnCard.copy();
        copy.drawnFromDiscard = drawnFromDiscard;
        copy.finisher = finisher;
        copy.scores = scores.clone();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // face-down grid cards, the draw deck, and a card someone else drew from the draw deck
            List<Deck<FrenchCard>> decks = new ArrayList<>(copy.grids);
            decks.add(copy.drawDeck);
            decks.add(copy.drawnCard);
            DeterminisationUtilities.reshuffle(playerId, decks, c -> true, redeterminisationRnd);
        }
        return copy;
    }

    /**
     * The final result once the game is over; otherwise the points scored in completed deals, scaled so that the
     * best possible total is 1 and the worst is 0.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        GolfSixParameters params = (GolfSixParameters) gameParameters;
        int best = GolfSixUtils.minDealScore(params) * params.nDeals;
        int worst = GolfSixUtils.maxDealScore(params) * params.nDeals;
        return (worst - scores[playerId]) / (double) (worst - best);
    }

    /**
     * Minus the points scored in completed deals, as the lowest score wins.
     */
    @Override
    public double getGameScore(int playerId) {
        return -scores[playerId];
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GolfSixGameState that)) return false;
        return drawnFromDiscard == that.drawnFromDiscard && finisher == that.finisher &&
                Objects.equals(grids, that.grids) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(discardPile, that.discardPile) &&
                Objects.equals(drawnCard, that.drawnCard) &&
                Arrays.equals(scores, that.scores);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), grids, drawDeck, discardPile, drawnCard, drawnFromDiscard, finisher)
                + 31 * Arrays.hashCode(scores);
    }
}
