package games.sirius;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import games.sirius.SiriusConstants.SiriusPhase;
import games.sirius.SiriusParameters.SmugglerType;
import games.sirius.actions.*;
import utilities.Utils;

import java.util.*;
import java.util.stream.IntStream;

import static games.sirius.SiriusConstants.MoonType.*;
import static games.sirius.SiriusConstants.SiriusCardType.*;
import static games.sirius.SiriusConstants.SiriusPhase.Move;
import static java.util.stream.Collectors.*;

public class SiriusForwardModel extends AbstractForwardModel {

    public static TakeCard takeCard = new TakeCard();

    @Override
    protected void _setup(AbstractGameState firstState) {
        SiriusGameState state = (SiriusGameState) firstState;
        SiriusParameters params = (SiriusParameters) state.getGameParameters();
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
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        SiriusGameState state = (SiriusGameState) currentState;
        SiriusParameters params = (SiriusParameters) state.getGameParameters();
        action.execute(state);

        SiriusTurnOrder turnOrder = (SiriusTurnOrder) state.getTurnOrder();
        // check game end
        if (state.ammoniaTrack >= params.ammoniaTrack.length - 1 ||
                state.contrabandTrack >= params.contrabandTrack.length - 1 ||
                state.corruptionTrack <= 0 ||
                turnOrder.getRoundCounter() >= params.maxRounds) {
            state.setGameStatus(Utils.GameResult.GAME_END);
            for (int p = 0; p < state.getNPlayers(); p++) {
                state.setPlayerResult(state.getOrdinalPosition(p) == 1 ? Utils.GameResult.WIN : Utils.GameResult.LOSE, p);
            }
        }

        if (state.isActionInProgress())
            return;

        // before we end the turn we need to check if we have triggered a police phase
        // or...do as an extended action sequence...?

        turnOrder.endPlayerTurn(state);

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SiriusGameState state = (SiriusGameState) gameState;
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        SiriusTurnOrder sto = (SiriusTurnOrder) state.getTurnOrder();
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
                        retValue.add(takeCard);
                        break;
                    case MINING:
                    case PROCESSING:
                    case OUTPOST:
                        if (currentMoon.getDeck().getSize() > 0)
                            retValue.add(takeCard);
                        break;
                    case TRADING:
                        Deck<SiriusCard> hand = state.getPlayerHand(player);
                        retValue.addAll(saleOptionsFrom(hand, AMMONIA));
                        retValue.addAll(saleOptionsFrom(hand, CONTRABAND));
                        retValue.addAll(saleOptionsFrom(hand, SMUGGLER));
                        retValue.add(new DoNothing()); // it is permissible to decide not to sell
                        break;
                }
                if (retValue.isEmpty())
                    retValue.add(new DoNothing());
                break;
            case Favour:
                retValue.add(new PassOnFavour());
                if (state.getPlayerHand(player).stream().anyMatch(c -> c.cardType == FAVOUR)) {
                    retValue.addAll(IntStream.rangeClosed(1, state.getNPlayers())
                            .filter(r -> r != sto.getRank(player))
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
            // TODO: possibly move this to an ExtendedActionSequence
            List<SellCards> decreaseTrackOptions = retValue.stream().map(SellCards::reverseDirection).collect(toList());
            retValue.addAll(decreaseTrackOptions);
        }
        return retValue;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return this; // immutable
    }
}
