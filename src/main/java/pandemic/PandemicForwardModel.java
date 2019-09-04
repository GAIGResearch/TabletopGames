package pandemic;

import actions.Action;
import actions.AddCardToDeck;
import actions.DrawCard;
import components.Card;
import components.Counter;
import components.Deck;
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
    public void next(GameState currentState, Action[] actions) {
        // TODO: advance current state for one turn, given actions
        playerActions(currentState, actions);
        drawCards(currentState);
        infectCities(currentState);
    }

    private void playerActions(GameState currentState, Action[] actions) {
        int activePlayer = currentState.getActivePlayer();
        for (Action a: actions) {
            a.execute(currentState);
        }
    }

    // TODO: parameterized game rule?
    // TODO: how are decks identified, how are epidemic cards identified, how are player hands identified
    // TODO: create new temporary decks, add to gamestate, remove afterwards.
    private void drawCards(GameState currentState) {
        int noCardsDrawn = 2;
        int activePlayer = currentState.getActivePlayer();

        int tempDeckID = currentState.tempDeck();
        DrawCard action = new DrawCard(playerDeck, tempDeckID);
        for (int i = 0; i < noCardsDrawn; i++) {  // Draw cards for active player from player deck into a new deck
            action.execute(currentState);
        }
        Deck tempDeck = currentState.findDeck(tempDeckID);
        for (Card c : tempDeck.getCards()) {  // Check the drawn cards
            if (c.getCardType() == epidemicCard) {  // If epidemic card, do epidemic
                epidemic(currentState);
            } else {  // Otherwise, give card to player
                AddCardToDeck action2 = new AddCardToDeck(c, activePlayerHand);
                action2.execute(currentState);
            }
        }
        currentState.clearTemp();
    }

    private void epidemic(GameState currentState) {
        // TODO: make epidemic happen
        // 1. infection counter idx ++
        Counter infectCounter = currentState.findCounter(infectionCounter);
        infectionCounter.increment(1);

        // 2. 3 cubes on bottom card in infection deck, then add this card on top of infection discard
        Card c = currentState.findDeck(infectionDeck).pickLast();
        // TODO: add cube tokens depending on card c location/color
        AddCardToDeck addCardToDeck = new AddCardToDeck(c, infectionDiscard);
        addCardToDeck.execute(currentState);

        // 3. shuffle infection discard deck, add back on top of infection deck
        Deck infectionDiscard = currentState.findDeck(infectDiscard);
        infectionDiscard.shuffle();
        for (Card card: infectionDiscard.getCards()) {
            addCardToDeck = new AddCardToDeck(card, infectionDeck);
            addCardToDeck.execute(currentState);
        }
    }

    private void infectCities(GameState currentState) {
        // TODO: draw infection cards according to infection counter in the state and infect them. discard cards.
    }
}
