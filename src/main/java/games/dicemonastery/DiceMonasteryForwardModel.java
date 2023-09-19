package games.dicemonastery;

import core.*;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Card;
import core.components.Deck;
import games.dicemonastery.actions.*;
import games.dicemonastery.components.*;
import utilities.Pair;

import java.util.*;
import java.util.stream.IntStream;

import static core.CoreConstants.VisibilityMode.FIRST_VISIBLE_TO_ALL;
import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.*;

public class DiceMonasteryForwardModel extends StandardForwardModel {

    // just create this once for performance - could also manually write out the array
    private static final List<Pair<Integer, Integer>> bidCombinations = IntStream.rangeClosed(0, 3)
            .boxed()
            .flatMap(b -> IntStream.rangeClosed(0, 3)
                    .mapToObj(m -> new Pair<>(b, m)))
            .collect(toList());
    public final AbstractAction SOW_WHEAT = new SowWheat();
    public final AbstractAction HARVEST_WHEAT = new HarvestWheat(1);
    public final AbstractAction PLACE_SKEP = new PlaceSkep();
    public final AbstractAction COLLECT_SKEP = new CollectSkep(1);
    public final AbstractAction PASS = new Pass();
    public final AbstractAction WEAVE_SKEP = new WeaveSkep();
    public final AbstractAction BEG_1 = new BegForAlms(1);
    public final AbstractAction BEG_5 = new BegForAlms(5);

    @Override
    protected void _setup(AbstractGameState firstState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) firstState;
        state._reset();
        DiceMonasteryParams params = (DiceMonasteryParams) state.getGameParameters();
        AbstractGameData _data = new AbstractGameData();
        _data.load(params.getDataPath());

        state.season = SPRING;
        state.year = 1;
        state.currentAreaBeingExecuted = null;
        state.actionPointsLeftForCurrentPlayer = 0;
        state.playersToMakeVikingDecisions = new ArrayList<>();

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

        Deck<Card> rawDeck = _data.findDeck("Short Pilgrimages");
        for (Card c : rawDeck.getComponents()) {
            Deck<Pilgrimage> deck = state.pilgrimageDecks.get(0);
            deck.add(Pilgrimage.create(c));
        }
        rawDeck = _data.findDeck("Long Pilgrimages");
        for (Card c : rawDeck.getComponents()) {
            Deck<Pilgrimage> deck = state.pilgrimageDecks.get(1);
            deck.add(Pilgrimage.create(c));
        }

        state.pilgrimageDecks.get(0).shuffle(state.rnd);
        state.pilgrimageDecks.get(1).shuffle(state.rnd);

        state.marketCards.clear();
        rawDeck = _data.findDeck("Market");
        for (Card c : rawDeck.getComponents()) {
            state.marketCards.add(MarketCard.create(c));
        }
        state.marketCards.shuffle(state.rnd);

        int playerCount = state.getNPlayers();
        state.forageCards.clear();
        rawDeck = _data.findDeck("Forage");
        for (Card c : rawDeck.getComponents()) {
            state.forageCards.add(ForageCard.create(c, playerCount));
        }
        state.forageCards.shuffle(state.rnd);

        rawDeck = _data.findDeck("Texts");
        for (Card c : rawDeck.getComponents()) {
            state.writtenTexts.put(IlluminatedText.create(c), 0);
        }
        rawDeck = _data.findDeck("Treasures");
        for (Card c : rawDeck.getComponents()) {
            state.treasuresCommissioned.put(Treasure.create(c), 0);
        }

        state.drawBonusTokens();
        state.replenishPigmentInMeadow();

        state.setGamePhase(Phase.PLACE_MONKS);
    }

    public void startSeason(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        state.season = state.season.next();
        if (state.season == SPRING) {
            //  int newFirstPlayer = (state.getFirstPlayer() + 1 + state.getNPlayers()) % state.getNPlayers();
            //   state.setFirstPlayer(newFirstPlayer); this is done in the main StandardForwardModel.endRound()
            state.year++;
            //  System.out.printf("Setting Abbot to p%d in year %d%n", state.getFirstPlayer(), state.year);
        }
        if (state.season == SUMMER && state.year == 1)
            state.season = state.season.next(); // we skip Summer in the first year
        state.checkAtLeastOneMonk();
        if (state.season == SUMMER)
            state.setGamePhase(BID);
        else
            state.setGamePhase(Phase.PLACE_MONKS);
        //  TODO: Account for this special case: int nextPlayer = state.firstPlayerWithMonks();
        if (state.season == WINTER)
            state.winterHousekeeping();  // this occurs at the start of WINTER, as it includes the Christmas Feast
    }

    public void endSeason(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        DiceMonasteryParams params = (DiceMonasteryParams) state.getGameParameters();
        switch (state.season) {
            case SPRING:
            case AUTUMN:
                state.springAutumnHousekeeping();
                endRound(gs);
                break;
            case SUMMER:
                endRound(gs);
                break;
            case WINTER:
                if (state.year == params.YEARS)
                    endGame(state);
                else {
                    int newAbbot = (state.getFirstPlayer() + 1) % state.getNPlayers();
                    endRound(state, newAbbot);
                }
                break;
        }
        if (state.isNotTerminal())
            startSeason(state);
    }

    private void initialiseUseMonkBooleans(DiceMonasteryGameState state, int nextPlayer) {
        state.turnOwnerTakenReward = false;
        if (state.availableBonusTokens(state.currentAreaBeingExecuted).isEmpty())
            playerTakesReward(state); // none to take
        state.actionPointsLeftForCurrentPlayer = state.actionPoints(state.currentAreaBeingExecuted, nextPlayer);
      //  System.out.printf("Initialising for Player %d with %d AP in %s%n", nextPlayer, state.actionPointsLeftForCurrentPlayer, state.currentAreaBeingExecuted);
    }

    void playerTakesReward(DiceMonasteryGameState state) {
        state.turnOwnerTakenReward = true;
        state.turnOwnerPrayed = false;
        if (state.currentAreaBeingExecuted == CHAPEL || state.currentAreaBeingExecuted == LIBRARY ||
                state.getResource(state.getCurrentPlayer(), Resource.PRAYER, STOREROOM) == 0)
            state.turnOwnerPrayed = true; // No prayers in CHAPEL or LIBRARY as they cannot be used on anything, and if we don't have any
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) currentState;

        // We could do this in the Actions...but
        // since this is core to the whole game loop, the muddying of responsibilities is acceptable
        if (action instanceof Pray)
            state.turnOwnerPrayed = true;
        if (action instanceof TakeToken)
            playerTakesReward(state);

        if (state.isActionInProgress())
            return;

        // The following code is hacked in from the old TurnOrder.endPlayerTurn...
        // ... once we have selected (for each season) who the next player is then we need to call
        // endPlayerTurn, and possibly endRound.

        int nextPlayer = -1;
        int currentPlayer = state.getCurrentPlayer();
        boolean newRound = false;

        switch (state.season) {
            case SPRING:
            case AUTUMN:
                if (state.getGamePhase() == Phase.PLACE_MONKS) {
                    // we move to the next player who still has monks to place
                    if (state.actionAreas.get(ActionArea.DORMITORY).size() == 0) {
                        // no monks left, so we move on to the next phase (which always starts with the MEADOW)
                        state.setGamePhase(Phase.USE_MONKS);
                        state.currentAreaBeingExecuted = ActionArea.MEADOW;
                        state.setUpPlayerOrderForCurrentArea(); // impossible to have no monks placed at this point
                        nextPlayer = state.playerOrderForCurrentArea.get(0);
                        initialiseUseMonkBooleans(state, nextPlayer);
                    } else {
                        nextPlayer = state.nextPlayer();
                    }
                } else if (state.getGamePhase() == Phase.USE_MONKS) {
                    // first we check to see if we have finished using all monks; in which case move to next player
                    if (state.actionPointsLeftForCurrentPlayer == 0) {
                        // first move all Monks back to dormitory for the current player
                        for (Monk m : state.monksIn(state.currentAreaBeingExecuted, currentPlayer)) {
                            state.moveMonk(m.getComponentID(), state.currentAreaBeingExecuted, DORMITORY);
                        }

                        if (state.monksIn(state.currentAreaBeingExecuted, -1).isEmpty()) {
                            // we have completed all actions for that area
                            if (state.setUpPlayerOrderForCurrentArea()) {
                                nextPlayer = state.playerOrderForCurrentArea.get(0);
                                initialiseUseMonkBooleans(state, nextPlayer);
                            } else {
                                newRound = true;  // new season
                                nextPlayer = state.getFirstPlayer();
                            }
                        } else {
                            // then move to next player
                            nextPlayer = state.nextPlayer();
                            initialiseUseMonkBooleans(state, nextPlayer);
                        }
                    } else {
                        nextPlayer = currentPlayer;
                    }
                }
                break;
            case SUMMER:
                switch ((DiceMonasteryConstants.Phase) state.getGamePhase()) {
                    case BID:
                        nextPlayer = (currentPlayer + 1) % state.getNPlayers();
                        if (state.allBidsIn()) {
                            // we have completed SUMMER bidding
                            state.playersToMakeVikingDecisions = state.executeBids();
                            state.setGamePhase(SACRIFICE);
                            if (!state.playersToMakeVikingDecisions.isEmpty())
                                nextPlayer = state.playersToMakeVikingDecisions.get(0);
                        }
                        break;
                    case SACRIFICE:
                        state.playersToMakeVikingDecisions.remove(0);
                        if (!state.playersToMakeVikingDecisions.isEmpty())
                            nextPlayer = state.playersToMakeVikingDecisions.get(0);
                        else
                            nextPlayer = currentPlayer;
                        break;
                }
                if (state.getGamePhase() == SACRIFICE && state.playersToMakeVikingDecisions.isEmpty()) {
                    // we have finished the raids, and all players have made sacrifice decisions
                    newRound = true;
                    nextPlayer = state.getFirstPlayer();
                }
                break;
            case WINTER:
                nextPlayer = (currentPlayer + 1) % state.getNPlayers();
                // round over if we get back to abbot as first player
                if (nextPlayer == state.getFirstPlayer()) {
                    newRound = true;
                    nextPlayer = (state.getFirstPlayer() + 1) % state.getNPlayers();
                }
                break;
            default:
                throw new AssertionError(String.format("Unknown Game Phase of %s in %s", state.getGamePhase(), state.season));
        }

        if (nextPlayer == -1)
            throw new AssertionError("No next player has been allocated for some reason");

        endPlayerTurn(state, nextPlayer);
        if (newRound) {
            endSeason(state);
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        DiceMonasteryParams params = state.getParams();
        int currentPlayer = state.getCurrentPlayer();
        int actionPointsAvailable = state.actionPointsLeftForCurrentPlayer;
        switch (state.season) {
            case SPRING:
            case AUTUMN:
                if (state.getGamePhase() == Phase.PLACE_MONKS) {
                    // we place monks
                    List<Monk> availableMonks = state.monksIn(DORMITORY, currentPlayer);
                    if (availableMonks.isEmpty()) {
                        throw new AssertionError("We have no monks left for player " + currentPlayer);
                    }
                    int mostPiousMonk = availableMonks.stream().mapToInt(Monk::getPiety).max().getAsInt();
                    String condition = "";
                    if (params.libraryWritingSets) {
                        int vellum = state.getResource(currentPlayer, VELLUM, STOREROOM);
                        int candles = state.getResource(currentPlayer, CANDLE, STOREROOM);
                        condition = String.valueOf(Math.min(vellum, candles));
                    }

                    String finalCondition = condition;
                    return Arrays.stream(ActionArea.values())
                            .filter(a -> a.dieMinimum > 0 && a.dieMinimum <= mostPiousMonk)
                            .map(a -> new PlaceMonk(currentPlayer, a, a == LIBRARY ? finalCondition : "")).collect(toList());
                } else if (state.getGamePhase() == Phase.USE_MONKS) {
                    List<AbstractAction> retValue = new ArrayList<>();
                    if (!state.turnOwnerTakenReward) {
                        // The first action of a player must be to take a BONUS_TOKEN, if there are any left
                        return state.availableBonusTokens(state.currentAreaBeingExecuted).stream().distinct()
                                .map(token -> new TakeToken(token, state.currentAreaBeingExecuted, currentPlayer))
                                .collect(toList());
                    }
                    if (!state.turnOwnerPrayed) {
                        return IntStream.rangeClosed(0, state.getResource(currentPlayer, PRAYER, STOREROOM))
                                .mapToObj(Pray::new).collect(toList());
                    }
                    if (actionPointsAvailable <= 0) {
                        throw new AssertionError("We have no action points left for player " + currentPlayer);
                    }

                    retValue.add(PASS);
                    switch (state.currentAreaBeingExecuted) {
                        case MEADOW:
                            DMArea meadow = state.actionAreas.get(MEADOW);
                            if (actionPointsAvailable >= params.takePigmentCost)
                                for (Resource pigment : new Resource[]{PALE_BLUE_PIGMENT, PALE_GREEN_PIGMENT, PALE_RED_PIGMENT}) {
                                    if (meadow.count(pigment, -1) > 0)
                                        retValue.add(new TakePigment(pigment, params.takePigmentCost));
                                }
                            if (state.season == SPRING) {
                                retValue.add(SOW_WHEAT);
                                if (state.getResource(currentPlayer, SKEP, STOREROOM) > 0)
                                    retValue.add(PLACE_SKEP);
                            } else {
                                int grainInField = meadow.count(GRAIN, currentPlayer);
                                if (grainInField > 0) {
                                    retValue.add(HARVEST_WHEAT);
                                    if (grainInField > 1 && state.getActionPointsLeft() > 1)
                                        retValue.add(new HarvestWheat(Math.min(grainInField, state.getActionPointsLeft())));
                                }
                                int skepsOut = meadow.count(SKEP, currentPlayer);
                                if (skepsOut > 0) {
                                    retValue.add(COLLECT_SKEP);
                                    if (skepsOut > 1 && actionPointsAvailable > 1)
                                        retValue.add(new CollectSkep(Math.min(skepsOut, actionPointsAvailable)));
                                }
                            }
                            break;
                        case KITCHEN:
                            if (state.getResource(currentPlayer, GRAIN, STOREROOM) > 0 && actionPointsAvailable >= params.bakeBreadCost)
                                retValue.add(new BakeBread(params.bakeBreadCost));
                            if (actionPointsAvailable >= params.prepareInkCost) {
                                Map<Resource, Integer> allPalePigments = state.getStores(currentPlayer, r -> r.isPigment && !r.isVivid);
                                for (Resource pigment : allPalePigments.keySet())
                                    retValue.add(new PrepareInk(pigment, params.prepareInkCost));
                            }
                            if (state.getResource(currentPlayer, GRAIN, STOREROOM) > 0 && actionPointsAvailable >= params.brewBeerCost)
                                retValue.add(new BrewBeer(params.brewBeerCost));
                            if (state.getResource(currentPlayer, HONEY, STOREROOM) > 0 && actionPointsAvailable >= params.brewMeadCost)
                                retValue.add(new BrewMead(params.brewMeadCost));
                            break;
                        case WORKSHOP:
                            retValue.add(WEAVE_SKEP);
                            if (actionPointsAvailable >= params.prepareInkCost) {
                                Map<Resource, Integer> allVividPigments = state.getStores(currentPlayer, r -> r.isPigment && r.isVivid);
                                for (Resource pigment : allVividPigments.keySet())
                                    retValue.add(new PrepareInk(pigment, params.prepareInkCost));
                            }
                            if (actionPointsAvailable >= params.makeCandleCost)
                                if (state.getResource(currentPlayer, WAX, STOREROOM) >= params.waxPerCandle)
                                    retValue.add(new MakeCandle(params.makeCandleCost));
                            if (actionPointsAvailable >= params.prepareVellumCost)
                                if (state.getResource(currentPlayer, CALF_SKIN, STOREROOM) > 0)
                                    retValue.add(new PrepareVellum(params.prepareVellumCost));
                            break;
                        case GATEHOUSE:
                            retValue.add(BEG_1);
                            if (actionPointsAvailable >= 5)
                                retValue.add(BEG_5);
                            else if (actionPointsAvailable > 1)
                                retValue.add(new BegForAlms(state.getActionPointsLeft()));
                            retValue.add(new VisitMarket());
                            if (actionPointsAvailable > 1) {
                                int shillings = state.getResource(currentPlayer, SHILLINGS, STOREROOM);
                                for (Treasure item : state.availableTreasures()) {
                                    if (item.cost <= shillings)
                                        retValue.add(new BuyTreasure(item));
                                }
                            }
                            if (actionPointsAvailable >= params.hireNoviceCost &&
                                    state.getResource(currentPlayer, SHILLINGS, STOREROOM) >= state.monksIn(null, currentPlayer).size())
                                retValue.add(new HireNovice(params.hireNoviceCost));

                            int highestPiety = state.getHighestPietyMonk(GATEHOUSE, currentPlayer);
                            List<Monk> eligibleMonks = state.monksIn(GATEHOUSE, currentPlayer);

                            for (int pilgrimDeck = 0; pilgrimDeck < 2; pilgrimDeck++) {
                                Deck<Pilgrimage> deck = state.pilgrimageDecks.get(pilgrimDeck);
                                if (deck.getSize() > 0) {
                                    Pilgrimage topCard = deck.peek();
                                    if (state.getActionPointsLeft() >= topCard.minPiety && highestPiety >= topCard.minPiety
                                            && state.getResource(currentPlayer, SHILLINGS, STOREROOM) >= topCard.cost) {
                                        Set<Integer> validPieties = eligibleMonks.stream()
                                                .map(Monk::getPiety)
                                                .filter(piety -> piety >= topCard.minPiety && piety <= state.getActionPointsLeft()).collect(toSet());

                                        validPieties.forEach(p -> retValue.add(new GoOnPilgrimage(topCard.copy(), p)));
                                    }
                                }
                            }
                            break;
                        case LIBRARY:
                            highestPiety = state.getHighestPietyMonk(LIBRARY, currentPlayer);
                            eligibleMonks = state.monksIn(LIBRARY, currentPlayer);
                            for (IlluminatedText text : state.writtenTexts.keySet()) {
                                // do we meet the minimum requirements
                                if (text.minPiety > highestPiety) // no advanced enough monk
                                    continue;
                                if (state.writtenTexts.get(text) == text.rewards.length)  // have they all been written
                                    continue;
                                if (!WriteText.meetsRequirements(text, state.playerTreasuries.get(currentPlayer)))  // vellum, candles and inks
                                    continue;
                                Set<Integer> validPieties = eligibleMonks.stream()
                                        .map(Monk::getPiety)
                                        .filter(piety -> piety >= text.minPiety && piety <= state.getActionPointsLeft()).collect(toSet());

                                validPieties.forEach(p -> retValue.add(new WriteText(text, p)));
                            }
                            break;
                        case CHAPEL:
                            retValue.remove(0); // remove PASS
                            eligibleMonks = state.monksIn(CHAPEL, currentPlayer);
                            Set<Integer> validPieties = eligibleMonks.stream().map(Monk::getPiety).collect(toSet());
                            retValue.addAll(validPieties.stream().map(piety -> new PromoteMonk(piety, CHAPEL, true)).collect(toList()));
                            if (retValue.isEmpty())
                                retValue.add(new Pass()); // might be that last monk was promoted with a DEVOTION token and RETIRED
                            break;
                        default:
                            throw new AssertionError("Unknown area : " + state.currentAreaBeingExecuted);
                    }
                    return retValue;
                }
                break;
            case SUMMER:
                if (state.getGamePhase() == SACRIFICE) {
                    // in this case we have sacrifice decisions to make - Monk or Treasure
                    List<AbstractAction> retValue = new ArrayList<>();
                    List<Treasure> treasure = state.getTreasures(currentPlayer);
                    if (!treasure.isEmpty())
                        retValue.add(new PayTreasure(treasure.stream().max(comparingInt(t -> t.vp)).get()));
                    if (treasure.isEmpty() || !params.mandateTreasureLoss) {
                        int[] pietyLevels = state.monksIn(DORMITORY, currentPlayer).stream().mapToInt(Monk::getPiety).distinct().toArray();
                        for (int piety : pietyLevels)
                            retValue.add(new KillMonk(piety));
                        if (retValue.isEmpty()) {
                            // somehow no penalty is due!
                            retValue.add(new DoNothing());
                        }
                    }
                    return retValue;
                } else {
                    if (state.getGamePhase() != BID)
                        throw new AssertionError("We are in an invalid Phase " + state.getGamePhase());
                    // in this case we are still in the bidding phase to determine who the Vikings raid
                    // we generate up to 16 SummerBids for every possibility of 0% to 100% of total stuff in 33% increments
                    // taking each of beer and mead independently
                    //  (removing any duplicate bids, so in practise the actual number will be rather lower)
                    // to avoid silliness, we also remove any bid of more than 8VP or higher
                    int totalBeer = state.getResource(currentPlayer, BEER, STOREROOM);
                    int totalMead = state.getResource(currentPlayer, MEAD, STOREROOM);
                    return bidCombinations.stream().map(pair -> new SummerBid((int) (pair.a / 3.0 * totalBeer), (int) (pair.b / 3.0 * totalMead)))
                            .distinct().filter(bid -> bid.beer + bid.mead * 2 < 33).collect(toList());
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
        throw new AssertionError("Not yet implemented combination " + state.season + " : " + state.getGamePhase());
    }

}
