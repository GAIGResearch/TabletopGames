package pandemic;

import actions.*;
import components.*;
import content.*;
import core.*;
import pandemic.actions.*;
import turnorder.TurnOrder;
import utilities.Hash;

import java.util.Random;

import static pandemic.Constants.*;
import static pandemic.actions.MovePlayer.placePlayer;

public class PandemicForwardModel extends ForwardModel {

    /**
     * Random generator for this game.
     */
    protected Random rnd;
    
    public void setup(AbstractGameState firstState) {

        PandemicGameState state = (PandemicGameState) firstState;
        PandemicParameters gameParameters = (PandemicParameters) ((PandemicGameState) firstState).getGameParameters();
        rnd = new Random(gameParameters.game_seed);

        // 1 research station in Atlanta
        new AddResearchStation("Atlanta").Execute(state, null);

        // init counters
        Counter outbreaksCounter = (Counter) state.getComponent(outbreaksHash);
        outbreaksCounter.setValue(0);

        Counter rStationCounter = (Counter) state.getComponent(researchStationHash);
        rStationCounter.setValue(gameParameters.n_research_stations);

        Counter infectionRateCounter = (Counter) state.getComponent(infectionRateHash);
        infectionRateCounter.setValue(0);

        for (String color : Constants.colors) {
            Counter diseaseCounter = (Counter) state.getComponent(Hash.GetInstance().hash("Disease " + color));
            diseaseCounter.setValue(0);
        }

        // infection
        Deck<Card> infectionDeck =  (Deck<Card>) state.getComponent(infectionHash);
        Deck<Card> infectionDiscard =  (Deck<Card>) state.getComponent(infectionDiscardHash);
        infectionDeck.shuffle(rnd);
        int nCards = gameParameters.n_infection_cards_setup;
        int nTimes = gameParameters.n_infections_setup;
        for (int j = 0; j < nTimes; j++) {
            for (int i = 0; i < nCards; i++) {
                Card c = infectionDeck.draw();

                // Place matching color (nTimes - j) cubes and place on matching city
                new InfectCity(gameParameters.max_cubes_per_city, c, nTimes - j).Execute(state, null);

                // Discard card
                new DrawCard(infectionDeck, infectionDiscard).Execute(state, null);
            }
        }

        // give players cards;
        Deck<Card> playerRoles =  (Deck<Card>) state.getComponent(playerRolesHash);
        Deck<Card> playerDeck =  (Deck<Card>) state.getComponent(playerDeckHash);
        int nCardsPlayer = gameParameters.n_cards_per_player.get(state.getNPlayers());
        playerRoles.shuffle();
        long maxPop = 0;
        int startingPlayer = -1;

        for (int i = 0; i < state.getNPlayers(); i++) {
            // Draw a player card
            Card c = playerRoles.draw();

            // Give the card to this player
            Area playerArea = state.getArea(i);
            playerArea.setComponent(playerCardHash, c);

            // Also add this player in Atlanta
            placePlayer(state, "Atlanta", i);

            // Give players cards
            IDeck<Card> playerHandDeck = (IDeck<Card>) playerArea.getComponent(Constants.playerHandHash);

            playerDeck.shuffle(rnd);
            for (int j = 0; j < nCardsPlayer; j++) {
                new DrawCard(playerDeck, playerHandDeck).Execute(state, null);
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
        playerDeck.shuffle(rnd);
        int noCards = playerDeck.getCards().size();
        int noEpidemicCards = gameParameters.n_epidemic_cards;
        int range = noCards / noEpidemicCards;
        for (int i = 0; i < noEpidemicCards; i++) {
            int index = i * range + i + rnd.nextInt(range);

            Card card = new Card();
            card.setProperty(Hash.GetInstance().hash("name"), new PropertyString("epidemic"));
            new AddCardToDeck(card, playerDeck, index).Execute(state, null);

        }

        // Player with highest population starts
        state.setActivePlayer(startingPlayer);
    }

    @Override
    public void next(AbstractGameState currentState, TurnOrder turnOrder, IAction action) {
        PandemicGameState pgs = (PandemicGameState)currentState;
        PandemicParameters gameParameters = (PandemicParameters) ((PandemicGameState) currentState).getGameParameters();

        if (pgs.getReactivePlayers().size() == 0) {
            // Only advance round step if no one is reacting
            pgs.roundStep += 1;
        }
        playerActions(pgs, action);

        if (action instanceof CureDisease) {
            // Check win condition
            boolean all_cured = true;
            for (String c : Constants.colors) {
                Counter diseaseCounter = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease " + c));
                if (diseaseCounter.getValue() < 1) all_cured = false;
            }
            if (all_cured) {
                pgs.setGameOver(GameResult.GAME_WIN);
                System.out.println("WIN!");
            }
        }

        boolean reacted = pgs.removeReactivePlayer();  // Reaction (if any) done

        if (!reacted && pgs.roundStep >= gameParameters.n_actions_per_turn || reacted && pgs.wasModelInterrupted()) {
            pgs.roundStep = 0;
            drawCards(pgs, gameParameters);

            if (pgs.getReactivePlayers().size() == 0) {
                // It's possible drawCards() method caused an interruption resulting in discard card reactions
                pgs.setModelInterrupted(false);
                if (!pgs.isQuietNight()) {
                    // Only do this step if Quiet Night event card was not played
                    infectCities(pgs, gameParameters);
                    pgs.setQuietNight(false);
                }

                // Set the next player as active
                pgs.nextPlayer();

            } else {
                pgs.setModelInterrupted(false);
            }
        }

        // TODO: wanna play event card?
    }

    public ForwardModel copy() {
        PandemicForwardModel fm = new PandemicForwardModel();
        fm.rnd = rnd; //TODO: revisit this, we may not want the same random generator.
        return fm;
    }

    private void playerActions(PandemicGameState currentState, IAction action) {
        action.Execute(currentState, null);
        PandemicParameters gameParameters = (PandemicParameters) currentState.getGameParameters();
        if (action instanceof QuietNight) {
            currentState.setQuietNight(true);
        } else if (action instanceof MovePlayer){
            // if player is Medic and a disease has been cured, then it should remove all cubes when entering the city
            int playerIdx = currentState.getActivePlayer();
            Card playerCard = (Card) currentState.getComponent(Constants.playerCardHash, playerIdx);
            String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;

            if (roleString.equals("Medic")){
                for (String color: Constants.colors){
                    Counter diseaseCounter = (Counter) currentState.getComponent(Hash.GetInstance().hash("Disease " + color));
                    String city = ((MovePlayer)action).getDestination();

                    boolean disease_cured = diseaseCounter.getValue() > 0;
                    if (disease_cured){
                        new TreatDisease(gameParameters.n_initial_disease_cubes, color, city, true);
                    }
                }
            }
        }
    }

    private void drawCards(PandemicGameState currentState, PandemicParameters gameParameters) {
        int noCardsDrawn = gameParameters.n_cards_draw;
        int activePlayer = currentState.getActingPlayer();

        currentState.clearTempDeck();
        Deck<Card> tempDeck = currentState.getTempDeck();
        Deck<Card> playerDeck = (Deck<Card>) currentState.getComponent(playerDeckHash);

        DrawCard action = new DrawCard(playerDeck, tempDeck);
        for (int i = 0; i < noCardsDrawn; i++) {  // Draw cards for active player from player deck into a new deck
            boolean canDraw = playerDeck.getCards().size() > 0;

            // if player cannot draw it means that the deck is empty -> GAME OVER
            if (!canDraw){
                currentState.setGameOver(GameResult.GAME_LOSE);
                System.out.println("No more cards to draw");
            }
            action.Execute(currentState, null);

        }

        boolean epidemic = false;

        Deck<Card> playerHandDeck = (Deck<Card>) currentState.getComponent(Constants.playerHandHash, activePlayer);

        for (Card c : tempDeck.getCards()) {  // Check the drawn cards
            // If epidemic card, do epidemic, only one per draw
            if (((PropertyString)c.getProperty(nameHash)).value.hashCode() == Constants.epidemicCard) {
                if (!epidemic) {
                    epidemic(currentState, gameParameters);
                    epidemic = true;
                }
            } else {  // Otherwise, give card to player
                if (playerHandDeck != null) {
                    // deck size doesn't go beyond 7
                    new AddCardToDeck(c, playerHandDeck).Execute(currentState, null);
                }
            }
        }
        currentState.clearTempDeck();

        // If player's deck size went over capacity, player needs to discard
        if (playerDeck != null && playerDeck.isOverCapacity()){
            // player needs to discard N cards TODO: action list should only contain discard card action
            int nDiscards = playerDeck.getCards().size() - playerDeck.getCapacity();
            for (int i = 0; i < nDiscards; i++) {
                currentState.addReactivePlayer(activePlayer);
            }
        }
    }

    private void epidemic(PandemicGameState currentState, PandemicParameters gameParameters) {

        // 1. infection counter idx ++
        Counter infRateCounter = (Counter) currentState.getComponent(infectionRateHash);
        infRateCounter.increment(1);

        // 2. 3 cubes on bottom card in infection deck, then add this card on top of infection discard
        Deck<Card> infectionDeck = (Deck) currentState.getComponent(infectionHash);
        Card c = (Card) infectionDeck.pickLast();
        if (c == null){
            // cannot draw card
            currentState.setGameOver(GameResult.GAME_LOSE);
            System.out.println("No more cards to draw");
            return;
        }
        new InfectCity(gameParameters.max_cubes_per_city, c, gameParameters.n_cubes_epidemic).Execute(currentState, null);
        if (checkInfectionGameEnd(currentState, gameParameters, c)) return;

        // 3. shuffle infection discard deck, add back on top of infection deck
        Deck<Card> infectionDiscardDeck = (Deck<Card>) currentState.getComponent(infectionDiscardHash);
        infectionDiscardDeck.shuffle(rnd);
        for (Card card: infectionDiscardDeck.getCards()) {
            new AddCardToDeck(card, infectionDeck).Execute(currentState, null);
        }
    }

    private void infectCities(PandemicGameState currentState, PandemicParameters gameParameters) {

        Counter infRateCounter = (Counter) currentState.getComponent(infectionRateHash);
        int noCardsDrawn = gameParameters.infection_rate[infRateCounter.getValue()];

        currentState.clearTempDeck();
        Deck<Card> tempDeck = currentState.getTempDeck();
        Deck<Card> infections = (Deck<Card>) currentState.getComponent(infectionHash);
        DrawCard action = new DrawCard(infections, tempDeck);
        for (int i = 0; i < noCardsDrawn; i++) {  // Draw cards for active player from player deck into a new deck
            action.Execute(currentState, null);
        }

        for (Card c : tempDeck.getCards()) {  // Check the drawn cards
            new InfectCity(gameParameters.max_cubes_per_city, c, gameParameters.n_cubes_infection).Execute(currentState, null);
            if (checkInfectionGameEnd(currentState, gameParameters, c)) return;
        }
        currentState.clearTempDeck();
    }

    private boolean checkInfectionGameEnd(PandemicGameState currentState, PandemicParameters gameParameters, Card c) {

        Counter outbreaksCounter = (Counter) currentState.getComponent(outbreaksHash);

        if (outbreaksCounter.getValue() >= gameParameters.lose_max_outbreak) {
            currentState.setGameOver(GameResult.GAME_LOSE);
            System.out.println("Too many outbreaks");
            return true;
        }
        String dc = "Disease Cube " + ((PropertyColor)c.getProperty(colorHash)).valueStr;
        Counter diseaseCube = (Counter) currentState.getComponent(Hash.GetInstance().hash(dc));
        if (diseaseCube.getValue() < 0) {
            currentState.setGameOver(GameResult.GAME_LOSE);
            System.out.println("Ran out of disease cubes");
            return true;
        }

        // Discard this infection card
        Deck<Card> infectionDiscardDeck = (Deck<Card>) currentState.getComponent(infectionDiscardHash);
        new AddCardToDeck(c, infectionDiscardDeck).Execute(currentState, null);
        return false;
    }
}
