package games.sirius;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import games.sirius.SiriusConstants.SiriusPhase;
import games.sirius.actions.*;
import utilities.Utils;

import java.util.*;
import java.util.stream.IntStream;

import static games.sirius.SiriusConstants.MoonType.*;
import static games.sirius.SiriusConstants.SiriusCardType.*;
import static games.sirius.SiriusConstants.SiriusPhase.Move;
import static java.util.stream.Collectors.toList;

public class SiriusForwardModel extends AbstractForwardModel {

    public static TakeCard takeCard = new TakeCard();

    @Override
    protected void _setup(AbstractGameState firstState) {
        SiriusGameState state = (SiriusGameState) firstState;
        SiriusParameters params = (SiriusParameters) state.getGameParameters();
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
        state.moons.add(new Moon("Sirius", TRADING, state.getNPlayers()));
        state.moons.add(new Moon("Mining Outpost", MINING, state.getNPlayers()));
        state.moons.add(new Moon("Processing Station", PROCESSING, state.getNPlayers()));
        state.moons.add(new Moon("Metropolis", METROPOLIS, state.getNPlayers()));
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
                    break; // no deck
                case METROPOLIS:
                    for (int i = 0; i < state.getNPlayers(); i++) {
                        moon.addCard(new SiriusCard("Favour", FAVOUR, 1));
                    }
                    break;
                case OUTPOST:
                    throw new AssertionError("Not yet implemented");
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
        turnOrder.endPlayerTurn(state);
        // check game end
        if (state.ammoniaTrack >= params.ammoniaTrack.length - 1 ||
                state.contrabandTrack >= params.contrabandTrack.length - 1 ||
                turnOrder.getRoundCounter() >= params.maxRounds) {
            state.setGameStatus(Utils.GameResult.GAME_END);
            for (int p = 0; p < state.getNPlayers(); p++) {
                state.setPlayerResult(state.getOrdinalPosition(p) == 1 ? Utils.GameResult.WIN : Utils.GameResult.LOSE, p);
            }
        }
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
                    case MINING:
                    case PROCESSING:
                        if (state.getPlayersAt(currentLocation).length > 0)
                            retValue.add(takeCard);
                        break;
                    case TRADING:
                        // TODO: For the moment we just sell all our Ammonia/Contraband cards, without doing anything more subtle
                        Deck<SiriusCard> hand = state.getPlayerHand(player);
                        List<SiriusCard> ammoniaInHand = hand.stream().filter(c -> c.cardType == AMMONIA).collect(toList());
                        if (ammoniaInHand.size() > 0)
                            retValue.add(new SellCards(ammoniaInHand));
                        List<SiriusCard> contrabandInHand = hand.stream().filter(c -> c.cardType == CONTRABAND).collect(toList());
                        if (contrabandInHand.size() > 0)
                            retValue.add(new SellCards(contrabandInHand));
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

    @Override
    protected AbstractForwardModel _copy() {
        return this; // immutable
    }
}
