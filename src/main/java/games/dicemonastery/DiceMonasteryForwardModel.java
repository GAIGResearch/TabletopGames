package games.dicemonastery;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import games.dicemonastery.actions.*;
import utilities.Pair;

import java.util.*;
import java.util.stream.IntStream;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase.BID;
import static games.dicemonastery.DiceMonasteryConstants.Phase.SACRIFICE;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.SPRING;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class DiceMonasteryForwardModel extends AbstractForwardModel {

    public final AbstractAction FORAGE_1 = new Forage(1);
    public final AbstractAction FORAGE_5 = new Forage(5);
    public final AbstractAction SOW_WHEAT = new SowWheat();
    public final AbstractAction HARVEST_WHEAT = new HarvestWheat(1);
    public final AbstractAction PLACE_SKEP = new PlaceSkep();
    public final AbstractAction COLLECT_SKEP = new CollectSkep(1);
    public final AbstractAction PASS = new Pass();
    public final AbstractAction BAKE_BREAD = new BakeBread();
    public final AbstractAction BREW_BEER = new BrewBeer();
    public final AbstractAction BREW_MEAD = new BrewMead();
    public final AbstractAction WEAVE_SKEP = new WeaveSkep();
    public final AbstractAction MAKE_CANDLE = new MakeCandle();
    public final AbstractAction PREPARE_VELLUM = new PrepareVellum();
    public final AbstractAction BEG_1 = new BegForAlms(1);
    public final AbstractAction BEG_5 = new BegForAlms(5);
    public final AbstractAction HIRE_NOVICE = new HireNovice();

    @Override
    protected void _setup(AbstractGameState firstState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) firstState;

        for (int p = 0; p < state.getNPlayers(); p++) {
            state.createMonk(4, p);
            state.createMonk(3, p);
            state.createMonk(2, p);
            state.createMonk(2, p);
            state.createMonk(1, p);
            state.createMonk(1, p);

            state.addResource(p, GRAIN, 2);
            state.addResource(p, HONEY, 2);
            state.addResource(p, WAX, 2);
            state.addResource(p, SKEP, 2);
            state.addResource(p, BREAD, 2);

            state.addResource(p, SHILLINGS, 6);
            state.addResource(p, PRAYER, 1);
        }

        for (Pilgrimage.DESTINATION destination : Pilgrimage.DESTINATION.values()) {
            Deck<Pilgrimage> deck = state.pilgrimageDecks.get(destination);
            deck.clear();
            // add all four cards
            for (int i = 0; i < 3; i++) {
                deck.add(new Pilgrimage(destination, false));
            }
            deck.add(new Pilgrimage(destination, true));
            deck.shuffle(state.rnd);
        }

        state.marketCards.clear();
        state.marketCards.add(new MarketCard(2, 2, 1, 2, 3, 4, PALE_GREEN_PIGMENT));
        state.marketCards.add(new MarketCard(3, 2, 1, 3, 3, 5, PALE_BLUE_PIGMENT));
        state.marketCards.add(new MarketCard(2, 2, 1, 3, 4, 5, PALE_RED_PIGMENT));
        state.marketCards.add(new MarketCard(3, 2, 1, 2, 4, 9, null));
        state.marketCards.add(new MarketCard(2, 2, 2, 2, 3, 4, PALE_RED_PIGMENT));
        state.marketCards.add(new MarketCard(3, 2, 2, 3, 3, 5, PALE_GREEN_PIGMENT));
        state.marketCards.add(new MarketCard(2, 2, 2, 3, 4, 9, null));
        state.marketCards.add(new MarketCard(3, 2, 2, 2, 4, 4, PALE_BLUE_PIGMENT));
        state.marketCards.shuffle(state.rnd);

        state.drawBonusTokens();

        state.setGamePhase(Phase.PLACE_MONKS);
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        super.endGame(gameState);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) currentState;
        DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder)state.getTurnOrder();

        action.execute(state);

        // We do this here to get direct access to TurnOrder (we could do this in the Actions..but that adds extra
        // set() options on State/TurnOrder that really shouldn't be publicly accessible
        // and since this is core to the whole game loop, the muddying of responsibilities is acceptable
        if (action instanceof Pray)
            dmto.turnOwnerPrayed = true;
        if (action instanceof TakeToken) {
            dmto.turnOwnerTakenReward = true;
            if (((TakeToken) action).token == BONUS_TOKEN.DEVOTION)
                dmto.turnOwnerPrayed = false; // in case they had none previously
        }
        if (state.isActionInProgress())
            return;

        // We only consider the next phase once any extended actions are complete
        state.getTurnOrder().endPlayerTurn(state);
        //       DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder)state.getTurnOrder();
        //       System.out.printf("After %s, end turn gives next player %d with %d action points\n", action, state.getCurrentPlayer(), dmto.getActionPointsLeft());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;

        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        int currentPlayer = turnOrder.getCurrentPlayer(state);
        switch (turnOrder.season) {
            case SPRING:
            case AUTUMN:
                if (state.getGamePhase() == Phase.PLACE_MONKS) {
                    // we place monks
                    List<Monk> availableMonks = state.monksIn(DORMITORY, currentPlayer);
                    if (availableMonks.isEmpty()) {
                        throw new AssertionError("We have no monks left for player " + currentPlayer);
                    }
                    int mostPiousMonk = availableMonks.stream().mapToInt(Monk::getPiety).max().getAsInt();
                    return Arrays.stream(ActionArea.values())
                            .filter(a -> a.dieMinimum > 0 && a.dieMinimum <= mostPiousMonk)
                            .map(a -> new PlaceMonk(currentPlayer, a)).collect(toList());
                } else if (state.getGamePhase() == Phase.USE_MONKS) {
                    List<AbstractAction> retValue = new ArrayList<>();
                    if (!turnOrder.turnOwnerTakenReward) {
                        // The first action of a player must be to take a BONUS_TOKEN, if there are any left
                        return state.availableBonusTokens(turnOrder.currentAreaBeingExecuted).stream().distinct()
                                .map(token -> new TakeToken(token, turnOrder.currentAreaBeingExecuted, currentPlayer))
                                .collect(toList());
                    }
                    if (!turnOrder.turnOwnerPrayed) {
                        return IntStream.rangeClosed(0, state.getResource(currentPlayer, PRAYER, STOREROOM))
                                .mapToObj(Pray::new).collect(toList());
                    }
                    if (turnOrder.actionPointsLeftForCurrentPlayer <= 0) {
                        throw new AssertionError("We have no action points left for player " + currentPlayer);
                    }

                    retValue.add(PASS);
                    switch (turnOrder.currentAreaBeingExecuted) {
                        case MEADOW:
                            retValue.add(FORAGE_1);
                            if (turnOrder.getActionPointsLeft() >= 5)
                                retValue.add(FORAGE_5);
                            else if (turnOrder.getActionPointsLeft() > 1)
                                retValue.add(new Forage(turnOrder.getActionPointsLeft()));
                            if (turnOrder.season == SPRING) {
                                retValue.add(SOW_WHEAT);
                                if (state.getResource(currentPlayer, SKEP, STOREROOM) > 0)
                                    retValue.add(PLACE_SKEP);
                            } else {
                                int grainInField = state.actionAreas.get(MEADOW).count(GRAIN, currentPlayer);
                                if (grainInField > 0) {
                                    retValue.add(HARVEST_WHEAT);
                                    if (grainInField > 1 && turnOrder.getActionPointsLeft() > 1)
                                        retValue.add(new HarvestWheat(Math.min(grainInField, turnOrder.getActionPointsLeft())));
                                }
                                int skepsOut = state.actionAreas.get(MEADOW).count(SKEP, currentPlayer);
                                if (skepsOut > 0) {
                                    retValue.add(COLLECT_SKEP);
                                    if (skepsOut > 1 && turnOrder.getActionPointsLeft() > 1)
                                        retValue.add(new CollectSkep(Math.min(skepsOut, turnOrder.getActionPointsLeft())));
                                }
                            }
                            break;
                        case KITCHEN:
                            if (state.getResource(currentPlayer, GRAIN, STOREROOM) > 0)
                                retValue.add(BAKE_BREAD);
                            if (turnOrder.getActionPointsLeft() > 1) {
                                Map<Resource, Integer> allPalePigments = state.getStores(currentPlayer, r -> r.isPigment && !r.isVivid);
                                for (Resource pigment : allPalePigments.keySet())
                                    retValue.add(new PrepareInk(pigment));
                                if (state.getResource(currentPlayer, GRAIN, STOREROOM) > 0)
                                    retValue.add(BREW_BEER);
                                if (state.getResource(currentPlayer, HONEY, STOREROOM) > 0)
                                    retValue.add(BREW_MEAD);
                            }
                            break;
                        case WORKSHOP:
                            retValue.add(WEAVE_SKEP);
                            if (turnOrder.getActionPointsLeft() > 1) {
                                Map<Resource, Integer> allVividPigments = state.getStores(currentPlayer, r -> r.isPigment && r.isVivid);
                                for (Resource pigment : allVividPigments.keySet())
                                    retValue.add(new PrepareInk(pigment));
                                if (state.getResource(currentPlayer, WAX, STOREROOM) > 0)
                                    retValue.add(MAKE_CANDLE);
                                if (state.getResource(currentPlayer, CALF_SKIN, STOREROOM) > 0)
                                    retValue.add(PREPARE_VELLUM);
                            }
                            break;
                        case GATEHOUSE:
                            retValue.add(BEG_1);
                            if (turnOrder.getActionPointsLeft() >= 5)
                                retValue.add(BEG_5);
                            else if (turnOrder.getActionPointsLeft() > 1)
                                retValue.add(new BegForAlms(turnOrder.getActionPointsLeft()));
                            retValue.add(new VisitMarket());
                            if (turnOrder.getActionPointsLeft() > 1) {
                                int shillings = state.getResource(currentPlayer, SHILLINGS, STOREROOM);
                                for (TREASURE item : TREASURE.values()) {
                                    if (item.buyable && item.vp * state.getParams().COST_PER_TREASURE_VP <= shillings && state.getNumberCommissioned(item) < item.limit)
                                        retValue.add(new BuyTreasure(item));
                                }
                            }
                            if (turnOrder.getActionPointsLeft() > 2 &&
                                    state.getResource(currentPlayer, SHILLINGS, STOREROOM) >= state.monksIn(null, currentPlayer).size())
                                retValue.add(HIRE_NOVICE);
                            List<Monk> eligibleMonks = state.monksIn(GATEHOUSE, currentPlayer);
                            // it is possible to have no monks in the gatehouse with AP remaining if a Devotion token was used
                            int highestPiety = eligibleMonks.isEmpty() ? 0 : eligibleMonks.stream()
                                    .max(comparingInt(Monk::getPiety))
                                    .orElseThrow(() -> new AssertionError("No Monks in Gatehouse?"))
                                    .piety;
                            for (Pilgrimage.DESTINATION destination : Pilgrimage.DESTINATION.values()) {
                                if (turnOrder.getActionPointsLeft() >= destination.minPiety && highestPiety >= destination.minPiety
                                        && state.getResource(currentPlayer, SHILLINGS, STOREROOM) >= destination.cost) {
                                    Set<Integer> validPieties = eligibleMonks.stream()
                                            .map(Monk::getPiety)
                                            .filter(piety -> piety >= destination.minPiety && piety <= turnOrder.getActionPointsLeft()).collect(toSet());

                                    Pilgrimage topCard = state.peekAtNextPilgrimageTo(destination);
                                    if (topCard != null) {
                                        validPieties.forEach(p -> retValue.add(new GoOnPilgrimage(destination, p)));
                                    }
                                }
                            }
                            break;
                        case LIBRARY:
                            for (ILLUMINATED_TEXT text : ILLUMINATED_TEXT.values()) {
                                // do we meet the minimum requirements
                                if (text.ap > turnOrder.getActionPointsLeft()) // enough AP
                                    continue;
                                if (state.textsWritten.get(text) == text.rewards.length)  // have they all been written
                                    continue;
                                if (!WriteText.meetsRequirements(text, state.playerTreasuries.get(currentPlayer)))  // vellum, candles and inks
                                    continue;
                                retValue.add(new WriteText(text));
                            }
                            break;
                        case CHAPEL:
                            retValue.remove(0); // remove Pass
                            retValue.add(new PromoteAllMonks(CHAPEL));
                            break;
                        default:
                            throw new AssertionError("Unknown area : " + turnOrder.currentAreaBeingExecuted);
                    }
                    return retValue;
                }
                break;
            case SUMMER:
                if (state.getGamePhase() == SACRIFICE) {
                    // in this case we have sacrifice decisions to make - Monk or Treasure
                    List<AbstractAction> retValue = new ArrayList<>();
                    List<TREASURE> treasure = state.getTreasures(currentPlayer);
                    if (!treasure.isEmpty())
                        retValue.add(new PayTreasure(treasure.stream().max(comparingInt(t -> t.vp)).get()));
                    int[] pietyLevels = state.monksIn(DORMITORY, currentPlayer).stream().mapToInt(Monk::getPiety).distinct().toArray();
                    for (int piety : pietyLevels)
                        retValue.add(new KillMonk(piety));
                    if (retValue.isEmpty()) {
                        // somehow no penalty is due!
                        retValue.add(new DoNothing());
                    }
                    return retValue;
                } else {
                    if (state.getGamePhase() != BID)
                        throw new AssertionError("We are in an invalid Phase " + state.getGamePhase());
                    // in this case we are still in the bidding phase to determine who the Vikings raid
                    // we generate up to 16 SummerBids for every possibility of 0% to 100% of total stuff in 33% increments
                    // taking each of beer and mead independently
                    //  (removing any duplicate bids, so in practise the actual number will be rather lower)
                    // to avoid silliness, we also remove any bid of 10VP or higher
                    int totalBeer = state.getResource(currentPlayer, BEER, STOREROOM);
                    int totalMead = state.getResource(currentPlayer, MEAD, STOREROOM);
                    return bidCombinations.stream().map(pair -> new SummerBid((int) (pair.a / 3.0 * totalBeer), (int) (pair.b / 3.0 * totalMead)))
                            .distinct().filter(bid -> bid.beer + bid.mead * 2 < 10).collect(toList());
                }
            case WINTER:
                List<AbstractAction> retValue = state.monksIn(DORMITORY, state.getCurrentPlayer()).stream()
                        .mapToInt(Monk::getPiety)
                        .distinct()
                        .mapToObj(piety -> new PromoteMonk(piety, DORMITORY))
                        .collect(toList());
                if (retValue.isEmpty())
                    retValue.add(new DoNothing());
                return retValue;
        }
        throw new AssertionError("Not yet implemented combination " + turnOrder.season + " : " + state.getGamePhase());
    }

    // just create this once for performance - could also manually write out the array
    private static final List<Pair<Integer, Integer>> bidCombinations = IntStream.rangeClosed(0, 3)
            .boxed()
            .flatMap(b -> IntStream.rangeClosed(0, 3)
                    .mapToObj(m -> new Pair<>(b, m)))
            .collect(toList());

    @Override
    protected DiceMonasteryForwardModel _copy() {
        // no mutable state
        return this;
    }
}
