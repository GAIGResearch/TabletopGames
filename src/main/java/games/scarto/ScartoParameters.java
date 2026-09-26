package games.scarto;

import core.components.TarotCard;
import evaluation.optimisation.TunableParameters;
import games.tricktaking.ITrickTakingParameters;

import java.util.Arrays;
import java.util.List;

/**
 * <p>Parameters for Scarto. The defaults follow the RECYCLE code on the Valet page
 * (https://mgoadric.github.io/valet/post/scarto.html); the Wikipedia rules (https://en.wikipedia.org/wiki/Scarto)
 * are available as options: several deals, and the dealer's exchange with the scarto.</p>
 */
public class ScartoParameters extends TunableParameters<ScartoParameters> implements ITrickTakingParameters {

    // Cards dealt to each player; the rest of the 78-card pack is the dealer's scarto
    public int handSize = 25;

    // Card points: Kings, the Pagat and the Angel; Queens; the Fool; Cavaliers; Knaves. All other cards score 0
    public int kingPoints = 4;
    public int honourTrumpPoints = 4;
    public int queenPoints = 3;
    public int foolPoints = 3;
    public int cavalierPoints = 2;
    public int knavePoints = 1;

    // Deals in the game (Wikipedia: each player deals once). Scores add up over the deals
    public int nDeals = 1;

    // Whether the dealer takes the scarto into their hand and discards the same number of cards (Wikipedia)
    public boolean dealerExchange = false;

    // Whether players remember the suits others are known to be void in (used when redeterminising)
    public boolean rememberVoids = true;

    public ScartoParameters() {
        addTunableParameter("handSize", 25);
        addTunableParameter("kingPoints", 4);
        addTunableParameter("honourTrumpPoints", 4);
        addTunableParameter("queenPoints", 3);
        addTunableParameter("foolPoints", 3);
        addTunableParameter("cavalierPoints", 2);
        addTunableParameter("knavePoints", 1);
        addTunableParameter("nDeals", 1, Arrays.asList(1, 3));
        addTunableParameter("dealerExchange", false, List.of(false, true));
        addTunableParameter("rememberVoids", true, List.of(false, true));
    }

    @Override
    public void _reset() {
        handSize = (int) getParameterValue("handSize");
        kingPoints = (int) getParameterValue("kingPoints");
        honourTrumpPoints = (int) getParameterValue("honourTrumpPoints");
        queenPoints = (int) getParameterValue("queenPoints");
        foolPoints = (int) getParameterValue("foolPoints");
        cavalierPoints = (int) getParameterValue("cavalierPoints");
        knavePoints = (int) getParameterValue("knavePoints");
        nDeals = (int) getParameterValue("nDeals");
        dealerExchange = (boolean) getParameterValue("dealerExchange");
        rememberVoids = (boolean) getParameterValue("rememberVoids");
    }

    public int cardPoints(TarotCard card) {
        if (card.isFool())
            return foolPoints;
        if (card.isTrump())
            return card.number == TarotCard.PAGAT || card.number == TarotCard.ANGEL ? honourTrumpPoints : 0;
        return switch (card.number) {
            case TarotCard.KING -> kingPoints;
            case TarotCard.QUEEN -> queenPoints;
            case TarotCard.CAVALIER -> cavalierPoints;
            case TarotCard.KNAVE -> knavePoints;
            default -> 0;
        };
    }

    /**
     * The points scored by a pile of captured cards.
     */
    public int pilePoints(List<TarotCard> cards) {
        // the points of the cards, plus one for every three cards. The extra card accounts for the Fool: its player
        // keeps it, leaving the winner of its trick only 2 cards
        int points = (cards.size() + 1) / 3;
        for (TarotCard card : cards)
            points += cardPoints(card);
        return points;
    }

    /**
     * All the points scored in one deal.
     */
    public int pointsPerDeal() {
        // four suits each with a King, Queen, Cavalier and Knave; the Pagat and the Angel; the Fool; 78 cards in threes
        return 4 * (kingPoints + queenPoints + cavalierPoints + knavePoints) + 2 * honourTrumpPoints + foolPoints
                + 78 / 3;
    }

    @Override
    public boolean rememberVoids() {
        return rememberVoids;
    }

    @Override
    protected ScartoParameters _copy() {
        return new ScartoParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof ScartoParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public ScartoParameters instantiate() {
        return this;
    }
}
