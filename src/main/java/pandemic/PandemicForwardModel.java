package pandemic;

import core.ForwardModel;
import core.GameState;

public class PandemicForwardModel implements ForwardModel {
    @Override
    public void setup(GameState firstState) {
        PandemicGameState state = (PandemicGameState) firstState;
        state.setActivePlayer(0); // TODO: player with city card of highest population

        // TODO: initial setup for the game
    }

    @Override
    public void next(GameState currentState, int[] actions) {
        // TODO: advance current state for one turn, given actions
        playerActions(currentState, actions);
        drawCards(currentState);
        infectCities(currentState);
    }

    private void playerActions(GameState currentState, int[] actions) {
        int activePlayer = currentState.getActivePlayer();
        // TODO: apply player actions, should be 4 of them.
    }

    private void drawCards(GameState currentState) {
        int activePlayer = currentState.getActivePlayer();
        // TODO: give top 2 cards of player deck to active player, unless epidemic cards
    }

    private void epidemic(GameState currentState) {
        // TODO: make epidemic happen
        // 1. infection counter idx ++
        // 2. 3 cubes on bottom card in infection deck, then add this card on top of infection discard
        // 3. shuffle infection discard deck, add back on top of infection deck
    }

    private void infectCities(GameState currentState) {
        // TODO: draw infection cards according to infection counter in the state and infect them. discard cards.
    }
}
