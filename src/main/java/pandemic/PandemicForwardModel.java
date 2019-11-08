package pandemic;

import actions.*;
import components.*;
import content.*;
import core.Area;
import core.ForwardModel;
import core.GameState;
import utilities.Hash;

import java.util.Random;

import static actions.MovePlayer.placePlayer;
import static pandemic.PandemicGameState.*;

public class PandemicForwardModel implements ForwardModel {
    int epidemicCard = Hash.GetInstance().hash("epidemic");
    public static int playersBNHash = Hash.GetInstance().hash("players");
    int[] infectionRate = new int[]{2, 2, 2, 3, 3, 4, 4};  // TODO: json

    @Override
    public void setup(GameState firstState) {
        PandemicGameState state = (PandemicGameState) firstState;

        // 1 research station in Atlanta
        new AddResearchStation("Atlanta").execute(state);  // TODO maybe static

        // init counters
        state.findCounter(outbreaksHash).setValue(0);
        state.findCounter(infectionCounterHash).setValue(0);
        for (int hash: diseaseHash) {
            state.findCounter(hash).setValue(0);
        }

        // infection
        Deck infectionDeck = state.findDeck(infectionDeckHash);
        Deck infectionDiscard = state.findDeck(infectionDeckDiscardHash);
        infectionDeck.shuffle();
        int nCards = 3;  // TODO json
        int nTimes = 3;  // TODO json
        for (int j = 0; j < nTimes; j++) {
            for (int i = 0; i < nCards; i++) {
                Card c = infectionDeck.draw();

                // Place matching color (nTimes - j) cubes and place on matching city
                new InfectCity(c, nTimes - j).execute(state);

                // Discard card
                new AddCardToDeck(c, infectionDiscard);
            }
        }

        // give players cards
        Deck playerCards = state.findDeck(playerCardDeckHash);
        Deck playerDeck = state.findDeck(playerDeckHash);  // TODO: assuming contains city & event cards
        playerCards.shuffle();
        int nCardsPlayer = 6 - state.nPlayers();  // TODO: params
        int maxPop = 0;
        int startingPlayer = -1;

        for (int i = 0; i < state.nPlayers(); i++) {
            // Draw a player card
            Card c = playerCards.draw();

            // Give the card to this player
            Area playerArea = state.getAreas().get(i);
            playerArea.setComponent(playerCardHash, c);

            // Also add this player in Atlanta
            placePlayer(state, "Atlanta", i);

            // Give players cards
            playerDeck.shuffle();
            for (int j = 0; j < nCardsPlayer; j++) {
                new DrawCard(state.findDeck(playerDeckHash), (Deck) playerArea.getComponent(playerHandHash)).execute(state);
            }

            Deck playerHandDeck = (Deck) playerArea.getComponent(playerHandHash);
            for (Card card: playerHandDeck.getCards()) {
                int pop = ((PropertyInt) card.getProperty(Hash.GetInstance().hash("population"))).value;
                if (pop > maxPop) {
                    startingPlayer = i;
                    maxPop = pop;
                }
            }
        }

        // Epidemic cards
        playerDeck.shuffle();
        int noCards = playerDeck.getCards().size();
        int noEpidemicCards = 4;  // TODO: json
        int range = noCards / noEpidemicCards;
        for (int i = 0; i < noEpidemicCards; i++) {
            int index = i * range + i + new Random().nextInt(range);  // TODO seed
            Card card = new Card("epidemic", epidemicCard);
            new AddCardToDeck(card, playerDeck, index).execute(state);
        }

        // Player with highest population starts
        state.setActivePlayer(startingPlayer);
    }

    @Override
    public void next(GameState currentState, Action[] actions) {
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
            if (c.getCardType() == epidemicCard) {  // If epidemic card, do epidemic  // TODO: if 2 in a row, reshuffle second
                epidemic(currentState);
            } else {  // Otherwise, give card to player
                Area area = currentState.getAreas().get(activePlayer);
                Deck deck = (Deck) area.getComponent(playerHandHash);
                if (deck != null) {
                    new AddCardToDeck(c, deck).execute(currentState);
                }
            }
        }
        currentState.clearTemp();
    }

    private void epidemic(GameState currentState) {
        // 1. infection counter idx ++
        currentState.findCounter(infectionCounterHash).increment(1);

        // 2. 3 cubes on bottom card in infection deck, then add this card on top of infection discard
        Card c = currentState.findDeck(infectionDeckHash).pickLast();
        new InfectCity(c, 3).execute(currentState);
        new AddCardToDeck(c, currentState.findDeck(infectionDeckDiscardHash)).execute(currentState);

        // TODO: wanna play event card?

        // 3. shuffle infection discard deck, add back on top of infection deck
        Deck infectionDiscard = currentState.findDeck(infectionDeckDiscardHash);
        infectionDiscard.shuffle();
        for (Card card: infectionDiscard.getCards()) {
            new AddCardToDeck(card, currentState.findDeck(infectionDeckHash)).execute(currentState);
        }
    }

    private void infectCities(GameState currentState) {
        Counter infectionCounter = currentState.findCounter(infectionCounterHash);
        int noCardsDrawn = infectionRate[infectionCounter.getCounter()];
        int tempDeckID = currentState.tempDeck();
        DrawCard action = new DrawCard(infectionDeckHash, tempDeckID);
        for (int i = 0; i < noCardsDrawn; i++) {  // Draw cards for active player from player deck into a new deck
            action.execute(currentState);
        }
        Deck tempDeck = new Deck(); //TODO: This new deck object should be replaced by an actual, consisitent temp deck.
        for (Card c : tempDeck.getCards()) {  // Check the drawn cards
            new InfectCity(c, 1).execute(currentState);
            new AddCardToDeck(c, currentState.findDeck(infectionDeckDiscardHash)).execute(currentState);
        }
        currentState.clearTemp();
    }
}
