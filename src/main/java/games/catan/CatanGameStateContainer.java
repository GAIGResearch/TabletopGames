package games.catan;

import core.AbstractGameState;
import core.AbstractGameStateContainer;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import games.catan.actions.build.BuyAction;
import games.catan.components.CatanCard;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class CatanGameStateContainer extends AbstractGameStateContainer {
    protected int[] scores; // score for each player
    protected int[] victoryPoints; // secret points from victory cards
    protected int[] knights, roadLengths; // knight count and road length for each player
//    protected List<Map<CatanParameters.Resource, Counter>> exchangeRates; // exchange rate with bank for each resource
    protected int largestArmyOwner; // playerID of the player currently holding the largest army
    protected int longestRoadOwner; // playerID of the player currently holding the longest road
    protected int longestRoadLength, largestArmySize;

    List<Map<CatanParameters.Resource, Counter>> playerResources;
    List<Map<BuyAction.BuyType, Counter>> playerTokens;
    List<Deck<CatanCard>> playerDevCards;
    Map<CatanParameters.Resource, Counter> resourcePool;
    Deck<CatanCard> devCards;
//    boolean developmentCardPlayed; // Tracks whether a player has played a development card this turn

    AbstractAction tradeOffer;
//    public int nTradesThisTurn;
    public CatanGameStateContainer(CatanGameState gs) {
        super(gs);
        scores = gs.scores; // score for each player
        victoryPoints = gs.victoryPoints; // secret points from victory cards
        knights = gs.knights;
        roadLengths = gs.roadLengths; // knight count and road length for each player
//        exchangeRates = gs.exchangeRates; // exchange rate with bank for each resource
        largestArmyOwner = gs.largestArmyOwner; // playerID of the player currently holding the largest army
        longestRoadOwner = gs.longestRoadOwner; // playerID of the player currently holding the longest road
        longestRoadLength = gs.longestRoadLength;
        largestArmySize = gs.largestArmySize;

        playerResources = gs.playerResources;
        playerTokens = gs.playerTokens;
        playerDevCards = gs.playerDevCards;
        resourcePool = gs.resourcePool;
        devCards = gs.devCards;

        tradeOffer = gs.tradeOffer;
    }
}
