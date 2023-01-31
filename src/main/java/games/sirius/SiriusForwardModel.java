package games.sirius;

import core.*;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import games.sirius.SiriusConstants.SiriusPhase;
import games.sirius.SiriusParameters.SmugglerType;
import games.sirius.actions.*;
import utilities.Pair;

import java.util.*;
import java.util.stream.IntStream;

import static games.sirius.SiriusConstants.MoonType.*;
import static games.sirius.SiriusConstants.SiriusCardType.*;
import static games.sirius.SiriusConstants.SiriusPhase.*;
import static java.util.stream.Collectors.*;

public class SiriusForwardModel extends StandardForwardModel {

    public static TakeCard takeCard = new TakeCard();

    @Override
    protected void _setup(AbstractGameState firstState) {
        SiriusGameState state = (SiriusGameState) firstState;
        SiriusParameters params = (SiriusParameters) state.getGameParameters();
        state._reset();
        for (int i = 0; i < params.favour; i++) {
            state.favourDeck.add(new SiriusCard("Favour", FAVOUR, 1));
        }
        for (int i = 0; i < params.ammonia; i++) {
            state.ammoniaDeck.add(new SiriusCard("Ammonia", AMMONIA, 1));
        }
        for (int i = 0; i < params.superAmmonia; i++) {
            state.ammoniaDeck.add(new SiriusCard("Super Ammonia", AMMONIA, 2));
        }
        for (int i = 0; i < params.hyperAmmonia; i++) {
            state.ammoniaDeck.add(new SiriusCard("Hyper Ammonia", AMMONIA, 3));
        }
        state.ammoniaDeck.shuffle(state.rnd);
        for (int i = 0; i < params.brokenContraband; i++) {
            state.contrabandDeck.add(new SiriusCard("Broken Contraband", CONTRABAND, 1));
        }
        for (int i = 0; i < params.contraband; i++) {
            state.contrabandDeck.add(new SiriusCard("Contraband", CONTRABAND, 3));
        }
        for (int i = 0; i < params.glowingContraband; i++) {
            state.contrabandDeck.add(new SiriusCard("Glowing Contraband", CONTRABAND, 0));
        }
        state.contrabandDeck.shuffle(state.rnd);

        for (SmugglerType st : SmugglerType.values()) {
            for (int i = 0; i < params.cardsPerSmugglerType; i++)
                state.smugglerDeck.add(new SiriusCard(st.name(), SMUGGLER, 1));
        }
        state.smugglerDeck.shuffle(state.rnd);

        state.corruptionTrack = params.startingCorruption;

        state.moons.add(new Moon("Sirius", TRADING, state.getNPlayers()));
        state.moons.add(new Moon("Mining Colony", MINING, state.getNPlayers()));
        state.moons.add(new Moon("Processing Station", PROCESSING, state.getNPlayers()));
        state.moons.add(new Moon("Outpost", OUTPOST, state.getNPlayers()));
        state.moons.add(new Metropolis("Metropolis", state.getNPlayers()));
        for (Moon moon : state.getAllMoons()) {
            switch (moon.moonType) {
                case MINING:
                    for (int i = 0; i < params.cardsPerEmptyMoon; i++) {
                        moon.addCard(state.ammoniaDeck.draw());
                    }
                    break;
                case PROCESSING:
                    for (int i = 0; i < params.cardsPerEmptyMoon; i++) {
                        moon.addCard(state.contrabandDeck.draw());
                    }
                    break;
                case TRADING:
                case METROPOLIS:
                    break; // no deck
                case OUTPOST:
                    for (int i = 0; i < params.cardsPerEmptyMoon; i++) {
                        moon.addCard(state.smugglerDeck.draw());
                    }
                    break;
            }
        }

        state.playerLocations = new int[state.getNPlayers()];
        state.moveSelected = new int[state.getNPlayers()];
        Arrays.fill(state.moveSelected, -1);
        // All players start on Sirius
        state.playerAreas = IntStream.range(0, state.getNPlayers()).mapToObj(PlayerArea::new).collect(toList());

        state.medalCount = 0;
        state.setGamePhase(Move);
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        SiriusParameters params = (SiriusParameters) state.getGameParameters();

        // check game end
        if (state.ammoniaTrack >= params.ammoniaTrack.length - 1 ||
                state.contrabandTrack >= params.contrabandTrack.length - 1 ||
                state.corruptionTrack <= 0 ||
                state.getRoundCounter() >= params.maxRounds) {
            endGame(state);
        }

        if (state.isActionInProgress())
            return;

        Pair<Integer, SiriusPhase> nextPlayerAndPhase = nextPlayerAndPhase(state); // record this before we change the phase
        SiriusPhase nextPhase = nextPlayerAndPhase.b;
        int nextPlayer = nextPlayerAndPhase.a;

        endPlayerTurn(state);

        if (nextPlayerAndPhase.b != phase) {
            // we end the phase here
            state.initialiseActions();
            switch (phase) {
                case Move:
                    if (nextPhase != Draw) {
                        throw new AssertionError("Impossible Phase to follow Move : " + nextPhase);
                    }
                    state.applyChosenMoves();
                    state.setGamePhase(SiriusPhase.Draw);
                    break;
                case Draw:
                    if (nextPhase != Move && nextPhase != Favour) {
                        throw new AssertionError("Impossible Phase to follow Draw : " + nextPhase);
                    }
                    // move first rank player to last, and shuffle the others
                    state.setRank(state.playerByRank[1], state.getNPlayers());
                    state.updatePlayerOrder();
                    state.setGamePhase(nextPhase);
                    if (nextPhase == Move) { // this could happen if no-one has a Favour card
                        endRound(state);
                        _endRound(state);
                        Arrays.fill(state.moveSelected, -1);
                    }
                    break;
                case Favour:
                    if (nextPhase != Move) {
                        throw new AssertionError("Impossible Phase to follow Favour : " + nextPhase);
                    }
                    state.updatePlayerOrder(); // after Favour cards played
                    endRound(state);
                    _endRound(state);
                    Arrays.fill(state.moveSelected, -1);
                    state.setGamePhase(Move);
            }
        }

        state.setTurnOwner(nextPlayer);
    }


    public void _endRound(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusParameters params = (SiriusParameters) state.getGameParameters();

        // add cards - for all Moons with a linked Card Type
        for (Moon moon : state.getAllMoons()) {
            int drawLimit = moon.getDeckSize() == 0 ? params.cardsPerEmptyMoon : params.cardsPerNonEmptyMoon;
            if (moon.getMoonType().linkedCardType != null) {
                Deck<SiriusCard> drawDeck = state.getDeck(moon.moonType.linkedCardType, false);
                if (moon.moonType.linkedCardType != FAVOUR) {
                    // Except for Favour cards, which are always taken direct from the draw pile
                    for (int i = 0; i < drawLimit; i++) {
                        if (drawDeck.getSize() > 0)
                            moon.addCard(drawDeck.draw());
                    }
                }
                if (moon.getCartelOwner() > -1 && drawDeck.getSize() > 0)
                    state.addCardToHand(moon.getCartelOwner(), drawDeck.draw());
            }
        }
    }


    public Pair<Integer, SiriusPhase> nextPlayerAndPhase(SiriusGameState state) {
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        switch (phase) {
            case Move:
                // In this case move selection is simultaneous
                // so the next player is the first one who has not selected a move (and is not us)
                for (int i = 0; i < state.moveSelected.length; i++)
                    if (i != state.getCurrentPlayer() && state.moveSelected[i] == -1) return new Pair<>(i, Move);
                // else we change phase, and the next player is the firstPlayer (who will always have an action available)
                return new Pair<>(state.getPlayerAtRank(1), Draw);
            case Draw:
                // The next player is whoever has a card to draw (or has not drawn one yet)
                int nextPlayerInPhase = state.getFirstMatchingPlayerFrom(state.nextPlayer[state.getCurrentPlayer()], i -> {
                    Moon moon = state.getMoon(state.getLocationIndex(i));
                    switch (moon.moonType) {
                        case TRADING:
                            boolean canSell = !state.actionsTakenByPlayers.get("Sold").get(i) && state.getPlayerHand(i).stream().anyMatch(c -> c.cardType == AMMONIA || c.cardType == CONTRABAND);
                            boolean canBetray = !state.actionsTakenByPlayers.get("Betrayed").get(i) && state.getPlayerHand(i).stream().anyMatch(c -> c.cardType == SMUGGLER);
                            return canSell || canBetray;
                        case METROPOLIS:
                            // need to have not yet acted - in these locations we just get one action
                            return !state.actionsTakenByPlayers.get("Favour").get(i) && !moon.policePresent;
                        default:
                            // needs to have cards available to take
                            return moon.getDeckSize() > 0;
                    }
                });
                // if -1, then we shift phase so the next player will be the first one with a Favour card
                // but this takes place after roiling the rank, so that the current second player will go first
                // this may be nobody...in which case we go back to Move
                if (nextPlayerInPhase > -1)
                    return new Pair<>(nextPlayerInPhase, Draw);
                nextPlayerInPhase = state.getFirstMatchingPlayerFrom(state.getPlayerAtRank(2), i -> state.getPlayerHand(i).stream().anyMatch(c -> c.cardType == FAVOUR));
                if (nextPlayerInPhase > -1)
                    return new Pair<>(nextPlayerInPhase, Favour);
                return new Pair<>(0, Move);
            case Favour:
                // if there is no next player (i.e. -1), then the next is 0 for Move
                nextPlayerInPhase = state.getFirstMatchingPlayerFrom(state.nextPlayer[state.getCurrentPlayer()], i -> !state.actionsTakenByPlayers.get("Favour").get(i) && state.getPlayerHand(i).stream().anyMatch(c -> c.cardType == FAVOUR));
                if (nextPlayerInPhase > -1)
                    return new Pair<>(nextPlayerInPhase, Favour);
                return new Pair<>(0, Move);
            default:
                throw new AssertionError("Unknown Phase " + phase);
        }
    }


    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SiriusGameState state = (SiriusGameState) gameState;
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        List<AbstractAction> retValue = new ArrayList<>();
        int player = state.getCurrentPlayer();
        int currentLocation = state.getLocationIndex(player);
        switch (phase) {
            case Move:
                retValue = IntStream.range(0, state.moons.size()).filter(i -> i != currentLocation).mapToObj(MoveToMoon::new).collect(toList());
                break;
            case Draw:
                Moon currentMoon = state.getMoon(currentLocation);
                switch (currentMoon.moonType) {
                    case METROPOLIS:
                        if (!currentMoon.policePresent)
                            retValue.add(takeCard);
                        break;
                    case MINING:
                    case PROCESSING:
                    case OUTPOST:
                        if (currentMoon.getDeckSize() > 0)
                            retValue.add(takeCard);
                        break;
                    case TRADING:
                        Deck<SiriusCard> hand = state.getPlayerHand(player);
                        if (!state.getActionTaken("Sold", player)) {
                            retValue.addAll(saleOptionsFrom(hand, AMMONIA));
                            retValue.addAll(saleOptionsFrom(hand, CONTRABAND));
                        }
                        if (!state.getActionTaken("Betrayed", player))
                            retValue.addAll(saleOptionsFrom(hand, SMUGGLER));
                        retValue.add(new NoSale()); // it is permissible to decide not to sell
                        break;
                }
                if (retValue.isEmpty())
                    retValue.add(new DoNothing());
                break;
            case Favour:
                retValue.add(new PassOnFavour());
                if (state.getPlayerHand(player).stream().anyMatch(c -> c.cardType == FAVOUR)) {
                    retValue.addAll(IntStream.rangeClosed(1, state.getNPlayers())
                            .filter(r -> r != state.getRank(player))
                            .mapToObj(FavourForRank::new).collect(toList()));
                    List<Moon> moons = state.getAllMoons();
                    retValue.addAll(IntStream.range(0, moons.size())
                            .filter(i -> moons.get(i).getMoonType() != TRADING && moons.get(i).cartelPlayer != player)
                            .mapToObj(FavourForCartel::new).collect(toList()));
                }
                break;
        }
        return retValue;
    }

    private List<SellCards> saleOptionsFrom(Deck<SiriusCard> hand, SiriusConstants.SiriusCardType type) {
        List<SiriusCard> matchingCards = hand.stream().filter(c -> c.cardType == type).collect(toList());
        // Given that players can have quite a few cards, I'll avoid a naive combinatorics approach
        // We can count how many of each type we have (1, 2, 3)
        // We can then have a x b x c options; if we have - say, 2 Super Ammonia and 6 Ammonia
        // then this will generate 3 x 7 Sales options = 21
        // while the naive approach generates 2^8 = 256
        Map<Integer, Long> countPerCardType = matchingCards.stream().collect(groupingBy(c -> c.value, counting()));
        List<SellCards> salesOptions = new ArrayList<>();
        for (int value3 = 0; value3 <= countPerCardType.getOrDefault(3, 0L); value3++) {
            for (int value2 = 0; value2 <= countPerCardType.getOrDefault(2, 0L); value2++) {
                for (int value1 = 0; value1 <= countPerCardType.getOrDefault(1, 0L); value1++) {
                    for (int value0 = 0; value0 <= countPerCardType.getOrDefault(0, 0L) / 3L; value0++) {
                        List<SiriusCard> salesOption = new ArrayList<>();
                        if (value0 > 0) { // special case as Glowing Contraband
                            salesOption.addAll(matchingCards.stream().filter(c -> c.value == 0).limit(value0 * 3L).collect(toList()));
                        }
                        if (value1 > 0)
                            salesOption.addAll(matchingCards.stream().filter(c -> c.value == 1).limit(value1).collect(toList()));
                        if (value2 > 0)
                            salesOption.addAll(matchingCards.stream().filter(c -> c.value == 2).limit(value2).collect(toList()));
                        if (value3 > 0)
                            salesOption.addAll(matchingCards.stream().filter(c -> c.value == 3).limit(value3).collect(toList()));
                        if (!salesOption.isEmpty())
                            salesOptions.add(new SellCards(salesOption));
                    }
                }
            }
        }
        Map<Integer, List<SellCards>> uniqueValueSales = salesOptions.stream().collect(groupingBy(SellCards::getTotalValue));
        // we then reduce the branching factor a bit by considering the option that sells most cards for each value.
        List<SellCards> retValue = new ArrayList<>();
        for (List<SellCards> so : uniqueValueSales.values()) {
            if (so.size() >= 1)
                so.sort(Comparator.comparingInt(sc -> -sc.getTotalCards())); //reverse order
            retValue.add(so.get(0));
        }
        if (type == SMUGGLER) {
            // in this case we can Sell in two ways - to move the Corruption Track up or down
            List<SellCards> decreaseTrackOptions = retValue.stream().map(SellCards::reverseDirection).collect(toList());
            retValue.addAll(decreaseTrackOptions);
        }
        return retValue;
    }
}
