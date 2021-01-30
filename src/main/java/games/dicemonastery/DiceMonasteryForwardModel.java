package games.dicemonastery;

import core.*;
import core.actions.AbstractAction;
import games.dicemonastery.actions.*;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static java.util.stream.Collectors.*;

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
            state.addResource(p, PRAYERS, 1);
        }

        state.setGamePhase(Phase.PLACE_MONKS);
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        super.endGame(gameState);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) currentState;

        if (!state.actionsInProgress.isEmpty()) {
            // we just register the action with the currently active action
            state.actionsInProgress.peek().registerActionTaken(state, action);
        }

        action.execute(state);

        // we may be in an extended action, so update that
        if (!state.actionsInProgress.isEmpty()) {
            // we just register the action taken with the currently active action
            // and then remove anything which is now complete
            int loopCount = 0;
            while (!state.actionsInProgress.isEmpty() && state.actionsInProgress.peek().executionComplete(state)) {
                state.actionsInProgress.pop();
                loopCount++;
                if (loopCount > 100) {
                    throw new AssertionError("WTF?");
                }
            }
        }

        if (state.isActionInProgress())
            return;

        // We only consider the next phase once any extended actions are complete
        state.getTurnOrder().endPlayerTurn(state);

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;

        if (state.isActionInProgress()) {
            return state.actionsInProgress.peek().followOnActions(state);
        }

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
                    if (turnOrder.actionPointsLeftForCurrentPlayer <= 0) {
                        throw new AssertionError("We have no action points left for player " + currentPlayer);
                    }
                    List<AbstractAction> retValue = new ArrayList<>();
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
                            break;
                        default:
                            break;
                    }
                    return retValue;
                }
            case SUMMER:
            case WINTER:
        }
        throw new AssertionError("Not yet implemented combination " + turnOrder.season + " : " + state.getGamePhase());
    }

    @Override
    protected DiceMonasteryForwardModel _copy() {
        // no mutable state
        return this;
    }
}
