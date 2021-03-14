package games.dicemonastery;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.actions.*;
import utilities.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.SPRING;
import static java.util.stream.Collectors.toList;

public class DiceMonasteryForwardModel extends AbstractForwardModel {

    public final AbstractAction FORAGE = new Forage();
    public final AbstractAction SOW_WHEAT = new SowWheat();
    public final AbstractAction HARVEST_WHEAT = new HarvestWheat();
    public final AbstractAction PLACE_SKEP = new PlaceSkep();
    public final AbstractAction COLLECT_SKEP = new CollectSkep();
    public final AbstractAction PASS = new Pass();
    public final AbstractAction BAKE_BREAD = new BakeBread();
    public final AbstractAction PREPARE_INK = new PrepareInk();
    public final AbstractAction BREW_BEER = new BrewBeer();
    public final AbstractAction BREW_MEAD = new BrewMead();
    public final AbstractAction WEAVE_SKEP = new WeaveSkep();
    public final AbstractAction MAKE_CANDLE = new MakeCandle();
    public final AbstractAction PREPARE_VELLUM = new PrepareVellum();
    public final AbstractAction BEG = new BegForAlms();
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
        if (action instanceof TakeToken)
            dmto.turnOwnerTakenReward = true;

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
                            retValue.add(FORAGE);
                            if (turnOrder.season == SPRING) {
                                retValue.add(SOW_WHEAT);
                                if (state.getResource(currentPlayer, SKEP, STOREROOM) > 0)
                                    retValue.add(PLACE_SKEP);
                            } else {
                                if (state.actionAreas.get(MEADOW).count(GRAIN, currentPlayer) > 0)
                                    retValue.add(HARVEST_WHEAT);
                                if (state.actionAreas.get(MEADOW).count(SKEP, currentPlayer) > 0)
                                    retValue.add(COLLECT_SKEP);
                            }
                            break;
                        case KITCHEN:
                            if (state.getResource(currentPlayer, GRAIN, STOREROOM) > 0)
                                retValue.add(BAKE_BREAD);
                            if (turnOrder.getActionPointsLeft() > 1) {
                                if (state.getResource(currentPlayer, PIGMENT, STOREROOM) > 0)
                                    retValue.add(PREPARE_INK);
                                if (state.getResource(currentPlayer, GRAIN, STOREROOM) > 0)
                                    retValue.add(BREW_BEER);
                                if (state.getResource(currentPlayer, HONEY, STOREROOM) > 0)
                                    retValue.add(BREW_MEAD);
                            }
                            break;
                        case WORKSHOP:
                            retValue.add(WEAVE_SKEP);
                            if (turnOrder.getActionPointsLeft() > 1) {
                                if (state.getResource(currentPlayer, PIGMENT, STOREROOM) > 0)
                                    retValue.add(PREPARE_INK);
                                if (state.getResource(currentPlayer, WAX, STOREROOM) > 0)
                                    retValue.add(MAKE_CANDLE);
                                if (state.getResource(currentPlayer, CALF_SKIN, STOREROOM) > 0)
                                    retValue.add(PREPARE_VELLUM);
                            }
                            break;
                        case GATEHOUSE:
                            retValue.add(BEG);
                            retValue.add(new VisitMarket());
                            if (turnOrder.getActionPointsLeft() > 2 &&
                                    state.getResource(currentPlayer, SHILLINGS, STOREROOM) >= state.monksIn(null, currentPlayer).size())
                                retValue.add(HIRE_NOVICE);
                            // TODO: "Go on pilgrimage" not yet implemented
                            // TODO: Buying Treasures not yet implemented
                            break;
                        case LIBRARY:
                            // TODO: "Write Text" not yet implemented
                            break;
                        case CHAPEL:
                            // No actions required....we just auto-pip up every Monk in the Chapel
/*                           This was the old code when the rule said that you could pip up one monk. Now they all pip up!
                             Left as cruft in case we want to tune this.
                              retValue.addAll(state.monksIn(CHAPEL, state.getCurrentPlayer()).stream()
                                    .mapToInt(Monk::getPiety)
                                    .distinct()
                                    .mapToObj(piety -> new PromoteMonk(piety, CHAPEL, false))
                                    .collect(toList())); */
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
                // we generate up to 16 SummerBids for every possibility of 0% to 100% of total stuff in 33% increments
                // taking each of beer and mead independently
                //  (removing any duplicate bids, so in practise the actual number will be rather lower)
                int totalBeer = state.getResource(currentPlayer, BEER, STOREROOM);
                int totalMead = state.getResource(currentPlayer, MEAD, STOREROOM);
                return bidCombinations.stream().map(pair -> new SummerBid((int) (pair.a / 3.0 * totalBeer), (int) (pair.b / 3.0 * totalMead)))
                        .distinct().collect(toList());
            case WINTER:
                List<AbstractAction> retValue = state.monksIn(DORMITORY, state.getCurrentPlayer()).stream()
                        .mapToInt(Monk::getPiety)
                        .distinct()
                        .mapToObj(piety -> new PromoteMonk(piety, DORMITORY))
                        .collect(toList());
                if (retValue.isEmpty())
                    retValue.add(new HireNovice(0)); // hire a free novice if no monks left (careless!)
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
