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

    /**
     * Random generator for this game.
     */
    protected Random rnd;
    
    @Override
    public void setup(GameState firstState) {

        PandemicGameState state = (PandemicGameState) firstState;
        PandemicParameters gameParameters = (PandemicParameters) firstState.getGameParameters();
        rnd = new Random(gameParameters.game_seed);

        // 1 research station in Atlanta
        new AddResearchStation("Atlanta").execute(state);

        // init counters
        firstState.findCounter("Outbreaks").setValue(0);
        firstState.findCounter("Research Stations").setValue(gameParameters.n_research_stations);
        firstState.findCounter("Infection Rate").setValue(0);
        for (String color : Constants.colors) {
            firstState.findCounter("Disease " + color).setValue(0);
        }

        // infection
        IDeck infectionDeck =  firstState.findDeck("Infections");
        IDeck infectionDiscard =  firstState.findDeck("Infection Discard");
        infectionDeck.shuffle(rnd);
        int nCards = gameParameters.n_infection_cards_setup;
        int nTimes = gameParameters.n_infections_setup;
        for (int j = 0; j < nTimes; j++) {
            for (int i = 0; i < nCards; i++) {
                Card c = infectionDeck.draw();

                // Place matching color (nTimes - j) cubes and place on matching city
                new InfectCity(gameParameters.max_cubes_per_city, c, nTimes - j).execute(state);

                // Discard card
                new DrawCard(infectionDeck, infectionDiscard).execute(state);
            }
        }

        // give players cards;
        IDeck playerCards =  firstState.findDeck("Player Roles");
        IDeck playerDeck = firstState.findDeck("Player Deck");
        int nCardsPlayer = gameParameters.n_cards_per_player.get(state.getNPlayers());
        long maxPop = 0;
        int startingPlayer = -1;

        for (int i = 0; i < state.getNPlayers(); i++) {
            // Draw a player card
            Card c = playerCards.draw();

            // Give the card to this player
            Area playerArea = state.getAreas().get(i);
            playerArea.setComponent(Constants.playerCardHash, c);

            // Also add this player in Atlanta
            placePlayer(state, "Atlanta", i);

            // Give players cards
            IDeck playerHandDeck = (IDeck) playerArea.getComponent(Constants.playerHandHash);

            playerDeck.shuffle(rnd);
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
        playerDeck.shuffle(rnd);
        int noCards = playerDeck.getCards().size();
        int noEpidemicCards = gameParameters.n_epidemic_cards;
        int range = noCards / noEpidemicCards;
        for (int i = 0; i < noEpidemicCards; i++) {
            int index = i * range + i + rnd.nextInt(range);

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
        PandemicParameters gameParameters = (PandemicParameters) currentState.getGameParameters();

        if (pgs.getReactivePlayers().size() == 0) {
            // Only advance round step if no one is reacting
            pgs.roundStep += 1;
        }
        playerActions(pgs, action);

        if (action instanceof CureDisease) {
            // Check win condition
            boolean all_cured = true;
            for (String c : Constants.colors) {
                if (pgs.findCounter("Disease " + c).getValue() < 1) all_cured = false;
            }
            if (all_cured) {
                pgs.setGameOver(GAME_WIN);
                System.out.println("WIN!");
            }
        }

        boolean reacted = pgs.removeReactivePlayer();  // Reaction (if any) done

        if (!reacted && pgs.roundStep >= gameParameters.n_actions_per_turn || reacted && pgs.whereModelInterrupted().equals("drawCards")) {
            pgs.roundStep = 0;
            drawCards(pgs, gameParameters);

            if (pgs.getReactivePlayers().size() == 0) {
                // It's possible drawCards() method caused an interruption resulting in discard card reactions
                pgs.setModelInterrupted("");
                if (!pgs.isQuietNight()) {
                    // Only do this step if Quiet Night event card was not played
                    infectCities(pgs, gameParameters);
                    pgs.setQuietNight(false);
                }

                // Set the next player as active
                pgs.nextPlayer();

            } else {
                pgs.setModelInterrupted("");
            }
        }
    }

    @Override
    public ForwardModel copy() {
        PandemicForwardModel fm = new PandemicForwardModel();
        fm.rnd = rnd; //TODO: revisit this, we may not want the same random generator.
        return fm;
    }

    private void playerActions(PandemicGameState currentState, Action action) {
        action.execute(currentState);
        PandemicParameters gameParameters = (PandemicParameters) currentState.getGameParameters();
        if (action instanceof QuietNight) {
            currentState.setQuietNight(true);
        } else if (action instanceof MovePlayer){
            // if player is Medic and a disease has been cured, then it should remove all cubes when entering the city
            int playerIdx = currentState.getActivePlayer();
            Card playerCard = (Card) currentState.getAreas().get(playerIdx).getComponent(Constants.playerCardHash);
            String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;

            if (roleString.equals("Medic")){
                for (String color: Constants.colors){
                    Counter diseaseToken = currentState.findCounter("Disease " + color);
                    String city = ((MovePlayer)action).getDestination();
                    boolean disease_cured = diseaseToken.getValue() > 0;
                    if (disease_cured){
                        new TreatDisease(gameParameters.n_initial_disease_cubes, color, city, true);
                    }
                }
            }
        }
    }

    // TODO: Easier if this is rewritten as player forced reactions to draw cards twice, and rules applied based on
    // what the player draws
    private void drawCards(PandemicGameState currentState, PandemicParameters gameParameters) {
        int noCardsDrawn = gameParameters.n_cards_draw;
        int activePlayer = currentState.getActingPlayer();

        String tempDeckID = currentState.tempDeck();
        DrawCard action = new DrawCard("Player Deck", tempDeckID);
        for (int i = 0; i < noCardsDrawn; i++) {  // Draw cards for active player from player deck into a new deck
            IDeck cityDeck =  currentState.findDeck("Player Deck");
            boolean canDraw = cityDeck.getCards().size() > 0;

            // if player cannot draw it means that the deck is empty -> GAME OVER
            if (!canDraw){
                currentState.setGameOver(GAME_LOSE);
                System.out.println("No more cards to draw");
            }
            action.execute(currentState);

        }
        IDeck tempDeck = currentState.findDeck(tempDeckID);
        boolean epidemic = false;

        Deck playerDeck = (Deck) currentState.getAreas().get(activePlayer).getComponent(Constants.playerHandHash);
        Deck playerDiscardDeck = (Deck) currentState.findDeck("Player Deck Discard");

        for (Card c : tempDeck.getCards()) {  // Check the drawn cards
            // If epidemic card, do epidemic, only one per draw
            if (((PropertyString)c.getProperty(nameHash)).value.hashCode() == Constants.epidemicCard) {
                if (!epidemic) {
                    epidemic(currentState, gameParameters);  // TODO: continue from here if pgs interrupted in epidemic
                    epidemic = true;
                }
            } else {  // Otherwise, give card to player
                if (playerDeck != null) {
                    // deck size doesn't go beyond 7
                    new AddCardToDeck(c, playerDeck).execute(currentState);
                }
            }
        }
        currentState.clearTempDeck();

        // If player's deck size went over capacity, player needs to discard
        if (playerDeck != null && playerDeck.isOverCapacity()){
            // player needs to discard N cards
            int nDiscards = playerDeck.getCards().size() - playerDeck.getCapacity();
            ArrayList<Action> acts = new ArrayList<>();  // Only discard card actions available
            for (int i = 0; i < playerDeck.getCards().size(); i++) {
                acts.add(new DrawCard(playerDeck, playerDiscardDeck, i));  // adding card i from player deck to player discard deck
            }
            currentState.possibleActions(acts);
            for (int i = 0; i < nDiscards; i++) {
                currentState.addReactivePlayer(activePlayer);
            }
            currentState.setModelInterrupted("drawCards");
        }
    }

    private void epidemic(PandemicGameState currentState, PandemicParameters gameParameters) {

        // 1. infection counter idx ++
        currentState.findCounter("Infection Rate").increment(1);

        // 2. 3 cubes on bottom card in infection deck, then add this card on top of infection discard
        Card c = currentState.findDeck("Infections").pickLast();
        if (c == null){
            // cannot draw card
            currentState.setGameOver(GAME_LOSE);
            System.out.println("No more cards to draw");
            return;
        }
        new InfectCity(gameParameters.max_cubes_per_city, c, gameParameters.n_cubes_epidemic).execute(currentState);
        if (checkInfectionGameEnd(currentState, gameParameters, c)) return;

        IDeck infectionDiscard = currentState.findDeck("Infection Discard");
        int nInfectDiscards = infectionDiscard.getCards().size();

        // TODO If any players have the "Resilient Population" event card, they should be asked if they want to play it here,
        // but only once. Needs to keep track of FM state to be able to resume...
//        int nPlayers = currentState.getNPlayers();
//        for (int i = 0; i < nPlayers; i++) {
//            Deck ph = (Deck) currentState.getAreas().get(i).getComponent(playerHandHash);
//            int nCards = ph.getCards().size();
//            for (int cp = 0; cp < nCards; cp++) {
//                Card card = ph.getCards().get(cp);
//                if (((PropertyString)card.getProperty(nameHash)).value.equals("Resilient Population")) {
//                    ArrayList<Action> acts = new ArrayList<>();
//                    acts.add(new DoNothing());
//                    for (int idx = 0; idx < nInfectDiscards; idx++) {
//                        acts.add(new DiscardCardWithCard(infectionDiscard, idx, card));
//                    }
//                    // Set discarding infection discarded cards (or do nothing) as the only options and ask player if they want to play their card
//                    currentState.possibleActions(acts);
//                    currentState.addReactivePlayer(i);
//                    currentState.setModelInterrupted("epidemic");
//                    return;  // TODO continue from here
//                }
//            }
//        }

        // 3. shuffle infection discard deck, add back on top of infection deck
        infectionDiscard.shuffle(rnd);
        for (Card card: infectionDiscard.getCards()) {
            new AddCardToDeck(card, currentState.findDeck("Infections")).execute(currentState);
        }
    }

    private void infectCities(GameState currentState, PandemicParameters gameParameters) {
        Counter infectionCounter = currentState.findCounter("Infection Rate");
        int noCardsDrawn = gameParameters.infection_rate[infectionCounter.getValue()];
        String tempDeckID = currentState.tempDeck();
        DrawCard action = new DrawCard("Infections", tempDeckID);
        for (int i = 0; i < noCardsDrawn; i++) {  // Draw cards for active player from player deck into a new deck
            action.execute(currentState);
        }
        IDeck tempDeck = currentState.findDeck(tempDeckID);
        for (Card c : tempDeck.getCards()) {  // Check the drawn cards
            new InfectCity(gameParameters.max_cubes_per_city, c, gameParameters.n_cubes_infection).execute(currentState);
            if (checkInfectionGameEnd(currentState, gameParameters, c)) return;
        }
        currentState.clearTempDeck();
    }

    private boolean checkInfectionGameEnd(GameState currentState, PandemicParameters gameParameters, Card c) {
        if (currentState.findCounter("Outbreaks").getValue() >= gameParameters.lose_max_outbreak) {
            currentState.setGameOver(GAME_LOSE);
            System.out.println("Too many outbreaks");
            return true;
        }
        if (currentState.findCounter("Disease Cube " + ((PropertyColor)c.getProperty(colorHash)).valueStr).getValue() < 0) {
            currentState.setGameOver(GAME_LOSE);
            System.out.println("Ran out of disease cubes");
            return true;
        }

        // Discard this infection card
        new AddCardToDeck(c, currentState.findDeck("Infection Discard")).execute(currentState);
        return false;
    }
}
