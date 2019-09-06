package pandemic;

import actions.Action;
import actions.AddCardToDeck;
import actions.DrawCard;
import components.*;
import content.PropertyBoolean;
import content.PropertyString;
import core.Area;
import core.ForwardModel;
import core.GameState;
import utilities.Hash;

import static pandemic.PandemicGameState.*;

public class PandemicForwardModel implements ForwardModel {
    int epidemicCard = Hash.GetInstance().hash("epidemic");  // TODO: is this right?
    int researchStationHash = Hash.GetInstance().hash("researchStation");

    @Override
    public void setup(GameState firstState) {  // TODO: difficulty?
        PandemicGameState state = (PandemicGameState) firstState;

        // 1 research station in Atlanta
        Board pb = (Board) state.getAreas().get(-1).getComponent(pandemicBoardHash);
        for (BoardNode bn: pb.getBoardNodes()) {
            PropertyString name = (PropertyString) bn.getProperty(Hash.GetInstance().hash("name"));
            if (name.value.equals("Atlanta")) {
                // It's Atlanta!
                bn.addProperty(researchStationHash, new PropertyBoolean(true));
                state.findCounter(researchStationCounterHash).decrement(1); // We have one less research station

                // Place players here too
            }
        }


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
        // drawCards(2);

        int noCardsDrawn = 2;
        int activePlayer = currentState.getActivePlayer();

        int tempDeckID = currentState.tempDeck();
        DrawCard action = new DrawCard(playerDeckHash, tempDeckID);
        for (int i = 0; i < noCardsDrawn; i++) {  // Draw cards for active player from player deck into a new deck
            action.execute(currentState);
        }
        Deck tempDeck = currentState.findDeck(tempDeckID);
        for (Card c : tempDeck.getCards()) {  // Check the drawn cards
            if (c.getCardType() == epidemicCard) {  // If epidemic card, do epidemic
                epidemic(currentState);
            } else {  // Otherwise, give card to player
                Area area = currentState.getAreas().get(activePlayer);
                Deck deck = (Deck) area.getComponent(playerHandHash);
                if (deck != null) {
                    AddCardToDeck action2 = new AddCardToDeck(c, deck);
                    action2.execute(currentState);
                }
            }
        }
        currentState.clearTemp();
    }

    private void epidemic(GameState currentState) {
        // 1. infection counter idx ++
        Counter infectCounter = currentState.findCounter(infectionCounterHash);
        infectCounter.increment(1);

        // 2. 3 cubes on bottom card in infection deck, then add this card on top of infection discard
        Card c = currentState.findDeck(infectionDeckHash).pickLast();
        // TODO: add cube tokens depending on card c location/color
        AddCardToDeck addCardToDeck = new AddCardToDeck(c, currentState.findDeck(infectionDeckDiscardHash));
        addCardToDeck.execute(currentState);

        // 3. shuffle infection discard deck, add back on top of infection deck
        Deck infectionDiscard = currentState.findDeck(infectionDeckDiscardHash);
        infectionDiscard.shuffle();
        for (Card card: infectionDiscard.getCards()) {
            addCardToDeck = new AddCardToDeck(card, currentState.findDeck(infectionDeckHash));
            addCardToDeck.execute(currentState);
        }
    }

    private void infectCities(GameState currentState) {
        // TODO: draw infection cards according to infection counter in the state and infect them. discard cards.
    }
}
