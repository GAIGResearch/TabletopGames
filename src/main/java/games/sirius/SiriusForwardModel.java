package games.sirius;

import core.*;
import core.actions.*;
import core.components.Deck;
import games.sirius.SiriusConstants.SiriusPhase;
import games.sirius.actions.MoveToMoon;
import games.sirius.actions.SellCards;
import games.sirius.actions.TakeCard;
import utilities.Utils;
import weka.core.pmml.jaxbbindings.SimpleRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static games.sirius.SiriusConstants.MoonType.*;
import static games.sirius.SiriusConstants.SiriusCardType.AMMONIA;
import static games.sirius.SiriusConstants.SiriusPhase.Move;
import static java.util.stream.Collectors.toList;

public class SiriusForwardModel extends AbstractForwardModel {

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
        for (int i = 0; i < params.ammonia; i++) {
            state.ammoniaDeck.add(new SiriusCard("Hyper Ammonia", AMMONIA, 3));
        }
        state.ammoniaDeck.shuffle(state.rnd);
        state.moons.add(new Moon("Sirius", TRADING, state.rnd));
        state.moons.add(new Moon("Mining_1", MINING, state.rnd));
        state.moons.add(new Moon("Mining_2", MINING, state.rnd));
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == MINING) {
                for (int i = 0; i < params.cardsPerEmptyMoon; i++) {
                    moon.addCard((SiriusCard) state.ammoniaDeck.draw());
                }
            }
        }

        state.playerLocations = new int[state.getNPlayers()];
        state.moveSelected = new int[state.getNPlayers()];
        Arrays.fill(state.moveSelected, -1);
        // All players start on Sirius
        state.playerAreas = IntStream.range(0, state.getNPlayers()).mapToObj(PlayerArea::new).collect(toList());
        state.contrabandMedals = params.contrabandTrack.clone();
        state.ammoniaMedals = params.ammoniaTrack.clone();
        state.setGamePhase(Move);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        SiriusGameState state = (SiriusGameState) currentState;
        action.execute(state);

        SiriusTurnOrder turnOrder = (SiriusTurnOrder) state.getTurnOrder();
        turnOrder.endPlayerTurn(state);
        // check game end
        if (state.ammoniaTrack == state.ammoniaMedals.length - 1) {
            state.setGameStatus(Utils.GameResult.GAME_END);
            int[] finalScores = new int[state.getNPlayers()];
            for (int p = 0; p < state.getNPlayers(); p++) {
                finalScores[p] = (int) state.getGameScore(p);
            }
            int winningScore = Arrays.stream(finalScores).max().orElseThrow(() -> new AssertionError("No MAX score found"));
            for (int p = 0; p < state.getNPlayers(); p++) {
                state.setPlayerResult(finalScores[p] == winningScore ? Utils.GameResult.WIN : Utils.GameResult.LOSE, p);
            }
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
                    case MINING:
                        retValue = currentMoon.deck.stream().map(c -> new TakeCard(c.value)).distinct().collect(toList());
                        break;
                    case TRADING:
                        // TODO: For the moment we just sell all our Ammonia cards, without doing anything more subtle
                        Deck<SiriusCard> hand = state.getPlayerHand(player);
                        if (hand.getSize() > 0) {
                            retValue.add(
                                    new SellCards(hand.stream().filter(c -> c.cardType == AMMONIA).collect(toList()))
                            );
                        }
                        break;
                }
                if (retValue.isEmpty())
                    retValue.add(new DoNothing());
                break;
        }
        return retValue;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return this; // immutable
    }
}
