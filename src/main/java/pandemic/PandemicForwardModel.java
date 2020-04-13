package pandemic;

import actions.*;
import components.*;
import content.*;
import core.*;
import pandemic.actions.*;
import utilities.Hash;

import java.util.ArrayList;
import java.util.Random;

import static pandemic.Constants.*;
import static pandemic.actions.MovePlayer.placePlayer;

public class PandemicForwardModel implements ForwardModel {
    private Game game;
    private PandemicParameters gameParameters;

    @Override
    public void setup(GameState firstState, Game game, GameParameters gameParameters) {
        PandemicGameState state = (PandemicGameState) firstState;
        this.game = game;
        this.gameParameters = (PandemicParameters) gameParameters;

        // 1 research station in Atlanta
        new AddResearchStation("Atlanta").execute(state);

        // init counters
        game.findCounter("Outbreaks").setValue(0);
        game.findCounter("Research Stations").setValue(this.gameParameters.n_research_stations);
        game.findCounter("Infection Rate").setValue(0);
        for (String color : Constants.colors) {
            game.findCounter("Disease " + color).setValue(0);
        }

        // infection
        Deck infectionDeck = game.findDeck("Infections");
        Deck infectionDiscard = game.findDeck("Infection Discard");
        infectionDeck.shuffle();
        int nCards = this.gameParameters.n_infection_cards_setup;
        int nTimes = this.gameParameters.n_infections_setup;
        for (int j = 0; j < nTimes; j++) {
            for (int i = 0; i < nCards; i++) {
                Card c = infectionDeck.draw();

                // Place matching color (nTimes - j) cubes and place on matching city
                new InfectCity(gameParameters, c, nTimes - j).execute(state);

                // Discard card
                new DrawCard(infectionDeck, infectionDiscard).execute(state);
            }
        }

        // give players cards
        Deck playerCards = game.findDeck("Player Roles");
        Deck playerDeck = game.findDeck("Player Deck");
        playerCards.shuffle();
        int nCardsPlayer = this.gameParameters.n_cards_per_player.get(state.nPlayers());
        long maxPop = 0;
        int startingPlayer = -1;

        for (int i = 0; i < state.nPlayers(); i++) {
            // Draw a player card
            Card c = playerCards.draw();

            // Give the card to this player
            Area playerArea = state.getAreas().get(i);
            playerArea.setComponent(Constants.playerCardHash, c);

            // Also add this player in Atlanta
            placePlayer(state, "Atlanta", i);

            // Give players cards
            Deck playerHandDeck = (Deck) playerArea.getComponent(Constants.playerHandHash);

            playerDeck.shuffle();
            for (int j = 0; j < nCardsPlayer; j++) {
                new DrawCard(playerDeck, playerHandDeck).execute(state);
            }

            for (Card card: playerHandDeck.getCards()) {
                Property property = card.getProperty(Hash.GetInstance().hash("population"));
                if (property != null){
                    long pop = ((PropertyLong) property).value;
                    if (pop > maxPop) {
                        startingPlayer = i;
                        maxPop = pop;
                    }
                }
            }
        }

        // Epidemic cards
        playerDeck.shuffle();
        int noCards = playerDeck.getCards().size();
        int noEpidemicCards = this.gameParameters.n_epidemic_cards;
        int range = noCards / noEpidemicCards;
        Random r = new Random(this.gameParameters.game_seed);
        for (int i = 0; i < noEpidemicCards; i++) {
            int index = i * range + i + r.nextInt(range);

            Card card = new Card();
            card.setProperty(Hash.GetInstance().hash("name"), new PropertyString("epidemic"));
            new AddCardToDeck(card, playerDeck, index).execute(state);

        }

        // Player with highest population starts
        state.setActivePlayer(startingPlayer);
    }

    @Override
    public void next(GameState currentState, Action action) {
        PandemicGameState pgs = (PandemicGameState)currentState;
        playerActions(pgs, action);

        if (action instanceof CureDisease) {
            // Check win condition
            boolean all_cured = true;
            for (String c : Constants.colors) {
                if (pgs.findCounter("Disease " + c).getValue() < 1) all_cured = false;
            }
            if (all_cured) {
                game.gameOver(GAME_WIN);
                System.out.println("WIN!");
            }
        }

        if (pgs.roundStep >= pgs.nInputActions()) {
            pgs.roundStep = 0;
            drawCards(pgs);

            if (!pgs.isQuietNight()) {
                infectCities(pgs);
                pgs.setQuietNight(false);
            }

            // Set the next player as active
            pgs.setActivePlayer((pgs.getActivePlayer() + 1) % pgs.nPlayers());
        }
    }

    private void playerActions(PandemicGameState currentState, Action action) {
        currentState.roundStep += 1;
        action.execute(currentState);
        if (action instanceof QuietNight) {
            currentState.setQuietNight(true);
        }
    }

    private void drawCards(GameState currentState) {
        int noCardsDrawn = gameParameters.n_cards_draw;
        int activePlayer = currentState.getActivePlayer();

        String tempDeckID = currentState.tempDeck();
        DrawCard action = new DrawCard("Player Deck", tempDeckID);
        for (int i = 0; i < noCardsDrawn; i++) {  // Draw cards for active player from player deck into a new deck
            Deck cityDeck = currentState.findDeck("Player Deck");
            boolean canDraw = cityDeck.getCards().size() > 0;

            // if player cannot draw it means that the deck is empty -> GAME OVER
            if (!canDraw){
                game.gameOver(GAME_LOSE);
                System.out.println("No more cards to draw");
            }
            action.execute(currentState);

        }
        Deck tempDeck = currentState.findDeck(tempDeckID);
        boolean epidemic = false;
        for (Card c : tempDeck.getCards()) {  // Check the drawn cards

            // If epidemic card, do epidemic, only one per draw
            if (((PropertyString)c.getProperty(nameHash)).value.hashCode() == Constants.epidemicCard) {
                if (!epidemic) {
                    epidemic(currentState);
                    epidemic = true;
                }
            } else {  // Otherwise, give card to player
                Area area = currentState.getAreas().get(activePlayer);
                Deck deck = (Deck) area.getComponent(Constants.playerHandHash);
                if (deck != null) {
                    // deck size doesn't go beyond 7
                    if (!new AddCardToDeck(c, deck).execute(currentState)){
                        // player needs to discard a card
                        game.getPlayers().get(activePlayer).getAction(currentState);
                    }
                }
            }
        }
        currentState.clearTempDeck();
    }

    private void epidemic(GameState currentState) {
        // 1. infection counter idx ++
        currentState.findCounter("Infection Rate").increment(1);

        // 2. 3 cubes on bottom card in infection deck, then add this card on top of infection discard
        Card c = currentState.findDeck("Infections").pickLast();
        if (c == null){
            // cannot draw card
            game.gameOver(GAME_LOSE);
            System.out.println("No more cards to draw");
            return;
        }
        new InfectCity(gameParameters, c, gameParameters.n_cubes_epidemic).execute(currentState);
        if (checkInfectionGameEnd(currentState, c)) return;


        // If any players have the "Resilient Population" event card, they should be asked if they want to play it here
        Deck infectionDiscard = currentState.findDeck("Infection Discard");
        int nInfectDiscards = infectionDiscard.getCards().size();

        int nPlayers = currentState.nPlayers();
        for (int i = 0; i < nPlayers; i++) {
            Deck ph = (Deck) currentState.getAreas().get(i).getComponent(playerHandHash);
            int nCards = ph.getCards().size();
            for (int cp = 0; cp < nCards; cp++) {
                if (((PropertyString)ph.getCards().get(cp).getProperty(nameHash)).value.equals("Resilient Population")) {
                    ArrayList<Action> acts = new ArrayList<>();
                    acts.add(new DoNothing());
                    for (int idx = 0; idx < nInfectDiscards; idx++) {
                        acts.add(new DiscardCard(infectionDiscard, idx));
                    }
                    // Set discarding infection discarded cards (or do nothing) as the only options and ask player if they want to play their card
                    currentState.possibleActions(acts);
                    Action a = game.getPlayers().get(i).getAction(currentState);
                    if (a != null && !(a instanceof DoNothing)) {
                        a.execute(currentState);
                        // Discard event card
                        new DiscardCard(ph, cp).execute(currentState);
                        break;
                    }
                }
            }
        }

        // 3. shuffle infection discard deck, add back on top of infection deck
        infectionDiscard.shuffle();
        for (Card card: infectionDiscard.getCards()) {
            new AddCardToDeck(card, currentState.findDeck("Infections")).execute(currentState);
        }
    }

    private void infectCities(GameState currentState) {
        Counter infectionCounter = currentState.findCounter("Infection Rate");
        int noCardsDrawn = gameParameters.infection_rate[infectionCounter.getValue()];
        String tempDeckID = currentState.tempDeck();
        DrawCard action = new DrawCard("Infections", tempDeckID);
        for (int i = 0; i < noCardsDrawn; i++) {  // Draw cards for active player from player deck into a new deck
            action.execute(currentState);
        }
        Deck tempDeck = currentState.findDeck(tempDeckID);
        for (Card c : tempDeck.getCards()) {  // Check the drawn cards
            new InfectCity(gameParameters, c, gameParameters.n_cubes_infection).execute(currentState);
            if (checkInfectionGameEnd(currentState, c)) return;
        }
        currentState.clearTempDeck();
    }

    private boolean checkInfectionGameEnd(GameState currentState, Card c) {
        if (currentState.findCounter("Outbreaks").getValue() >= gameParameters.lose_max_outbreak) {
            game.gameOver(GAME_LOSE);
            System.out.println("Too many outbreaks");
            return true;
        }
        if (currentState.findCounter("Disease Cube " + ((PropertyColor)c.getProperty(colorHash)).valueStr).getValue() < 0) {
            game.gameOver(GAME_LOSE);
            System.out.println("Ran out of disease cubes");
            return true;
        }

        // Discard this infection card
        new AddCardToDeck(c, currentState.findDeck("Infection Discard")).execute(currentState);
        return false;
    }
}
