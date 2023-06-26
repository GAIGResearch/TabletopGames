package games.wonders7;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;
import games.wonders7.actions.*;
import games.wonders7.cards.Wonder7Card;
import games.wonders7.cards.Wonder7Board;
import utilities.Utils;

import java.util.*;

public class Wonders7ForwardModel extends AbstractForwardModel {
// The rationale of the ForwardModel is that it contains the core game logic, while the GameState contains the underlying game data. 
// Usually this means that ForwardModel is stateless, and this is a good principle to adopt, but as ever there will always be exceptions.

    public void _setup(AbstractGameState state){
        Wonders7GameState wgs = (Wonders7GameState) state;

        //System.out.println("THE GAME HAS STARTED");
        wgs.playerHands = new ArrayList<>();
        wgs.playedCards = new ArrayList<>();
        wgs.turnActions = new AbstractAction[wgs.getNPlayers()];
        wgs.playerWonderBoard = new Wonder7Board[wgs.getNPlayers()];
        wgs.AgeDeck = new Deck<>("Age Deck", CoreConstants.VisibilityMode.MIXED_VISIBILITY);
        wgs.wonderBoardDeck = new Deck<>("Wonder Board Deck", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        for (int i=0; i<wgs.getNPlayers(); i++){
            wgs.playerHands.add(new Deck<>("Player hand" + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            wgs.playedCards.add(new Deck<>("Played Cards", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
        }

        // Cards that have been discarded all players
        wgs.discardPile = new Deck<>("Discarded Cards", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);

        // Shuffles wonder-boards
        createWonderDeck(wgs); // Adds Wonders into game
        Random r = new Random(wgs.getGameParameters().getRandomSeed());
        wgs.wonderBoardDeck.shuffle(r);

        // Gives each player wonder board and manufactured goods from the wonder
        for (int player=0; player < wgs.getNPlayers(); player++) {
            wgs.setPlayerWonderBoard(player, wgs.wonderBoardDeck.draw());// Each player has one designated Wonder board

            // Players get their wonder board manufacturedGoods added to their resources
            Set<Wonders7Constants.resources> keys = wgs.getPlayerWonderBoard(player).resourcesProduced.keySet(); // Gets all the resources the stage provides
            for (Wonders7Constants.resources resource : keys) {  // Goes through all keys for each resource
                int stageValue = wgs.getPlayerWonderBoard(player).resourcesProduced.get(resource); // Number of resource the card provides
                int playerValue = wgs.getPlayerResources(player).get(resource); // Number of resource the player owns
                wgs.getPlayerResources(player).put(resource, stageValue + playerValue); // Adds the resources provided by the stage to the players resource count
            }
        }

        ageSetup(wgs); // Shuffles deck and fills player hands, sets the turn owner
        //System.out.println(wgs.AgeDeck.toString());
    }

    public void ageSetup(AbstractGameState state){
        Wonders7GameState wgs = (Wonders7GameState) state;
        Random r = new Random(wgs.getGameParameters().getRandomSeed());


        // Sets up the age
        createAgeDeck(wgs); // Fills Age1 deck with cards
        wgs.AgeDeck.shuffle(r);
        //System.out.println("ALL THE CARDS IN THE GAME: "+wgs.AgeDeck.getSize());
        // Give each player their 7 cards, wonderBoard and the manufactured goods from the wonder-board
        for (int player=0; player < wgs.getNPlayers(); player++){
            for (int card=0; card< ((Wonders7GameParameters) wgs.getGameParameters()).nWonderCardsPerPlayer; card++) {
                wgs.getPlayerHand(player).add(wgs.AgeDeck.draw());
            }
        }

        // Player 0 starts
        wgs.getTurnOrder().setTurnOwner(0);
    }

    public void _next(AbstractGameState state, AbstractAction action){
        Wonders7GameState wgs = (Wonders7GameState) state;
        int direction = ((Wonders7TurnOrder) wgs.getTurnOrder()).getDirection();
        //action.execute(wgs);
        /*/ Prints players hands and the sizes
         if (wgs.getCurrentPlayer() ==0 && wgs.getTurnAction(0)==null){
             System.out.println("Players resource counts and hands: ");
             for (int i = 0; i < wgs.getNPlayers(); i++) {
                 System.out.println(wgs.getPlayerWonderBoard(i).wonderName + " "+ i + " --PLAYER RESOURCES--> " + wgs.getPlayerResources(i) + " --PLAYER HAND--> " + wgs.getPlayerHand(i) + " --PLAYED CARDS--> " + wgs.getPlayedCards(i));
             }
            System.out.println("");
         }

         */
        // EVERYBODY NOW PLAYS THEIR CARDS (ACTION ROUND)
        if (checkActionRound(wgs)) {
            for (int i = 0; i < wgs.getNPlayers(); i++) {
                wgs.getTurnOrder().setTurnOwner(i); // PLAYER i DOES THE ACTION THEY SELECTED, NOT ANOTHER PLAYERS ACTION
                //System.out.println("PLAYER " + wgs.getCurrentPlayer());
                //System.out.println("PLAYER " + wgs.getCurrentPlayer() + " PLAYED: " + wgs.getTurnAction(wgs.getCurrentPlayer()).toString()); // SAYS WHAT ACTION PLAYER i CHOSE!
                wgs.getTurnAction(wgs.getCurrentPlayer()).execute(wgs); // EXECUTE THE ACTION
                wgs.setTurnAction(wgs.getCurrentPlayer(), null); // REMOVE EXECUTED ACTION
            }
            //System.out.println("--------------------------------------------------------------------                                          ");
            wgs.getTurnOrder().setTurnOwner(0);

            // PLAYER HANDS ARE NOW ROTATED AROUND EACH PLAYER
             Deck<Wonder7Card> temp = wgs.getPlayerHands().get(0);
             if (direction == 1) {
                 for (int i = 0; i < wgs.getNPlayers(); i++) {
                     if (i == wgs.getNPlayers()-1) {wgs.getPlayerHands().set(i, temp);} // makes sure the last player receives first players original hand
                     else {wgs.getPlayerHands().set(i, wgs.getPlayerHands().get(i+1));} // Rotates hands clockwise
                 }
             }
             else {
                 temp = wgs.getPlayerHand((wgs.getNPlayers()-1)% wgs.getNPlayers());
                 for (int i = (wgs.getNPlayers()-1)% wgs.getNPlayers(); i >-1; i--) {
                     if (i% wgs.getNPlayers() == 0) {wgs.getPlayerHands().set(i, temp);} // makes sure the last player receives first players original hand
                     else {wgs.getPlayerHands().set(i, wgs.getPlayerHands().get(i-1));} // Rotates hands anticlockwise
                 }
             }
             //System.out.println("ROTATING HANDS!!!!!");
             checkAgeEnd(wgs); // Check for Age end;

         }
        // PLAYERS SELECT A CARD (SELECTION ROUND)
        else if (checkActionRound(wgs)==false){ // When turn order is clockwise/anticlockwise
            wgs.setTurnAction(wgs.getCurrentPlayer(), action); // PLAYER CHOOSES ACTION
            wgs.getTurnOrder().endPlayerTurn(wgs);
        }
    }

    protected boolean checkActionRound(AbstractGameState gameState){
        Wonders7GameState wgs = (Wonders7GameState) gameState;
        for (int i=0; i<wgs.getNPlayers();i++) {
            if (wgs.turnActions[i] == null) return false;
        }
        return true;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        // If player has the prerequisite card/enough resources/the card is free/the player can pay for the resources to play the card
        for (int i=0; i<wgs.getPlayerHand(wgs.getCurrentPlayer()).getSize(); i++){ // Goes through each card in hand
            if (wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).isFree(wgs)){ // Checks if player has prerequisite
                actions.add((new FreeCard(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName)));
            }
            else if (wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).isPlayable(wgs)&&(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).constructionCost.size()!=0)){ // If player can afford the card cost
                actions.add(new PlayCard(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName));
            }
            else if (wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).isPlayable(wgs)) {
                actions.add(new FreeCard(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName)); // Checks if card has no cost
            }

            if (wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).isPayableR(wgs)&&(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).constructionCost.size()!=0)&&wgs.getNPlayers()>1){ // Checks if card can be played after buying resources from the player to the right
                actions.add(new BuyResourceR(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName));
            }
            if (wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).isPayableL(wgs)&&(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).constructionCost.size()!=0)&&wgs.getNPlayers()>1){ // Checks if card can be played after buying resources from the player to the left
                actions.add(new BuyResourceL(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName));
            }
        }

        // If next stage is playable or not
        if (wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).isPlayable(wgs)){
            for (int i=0; i<wgs.getPlayerHand(wgs.getCurrentPlayer()).getSize(); i++) { // Goes through each card in hand
                actions.add(new BuildStage(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName, wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).wonderStage));
            }
        }

        // All player can use special effect on card
        if ((wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).effectUsed==false)){
            for (int i=0; i<wgs.getPlayerHand(wgs.getCurrentPlayer()).getSize(); i++) { // Goes through each card in hand
                actions.add(new SpecialEffect(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName));
            }
        }

        // All discard-able cards in player hand
        for (int i=0; i<wgs.getPlayerHand(wgs.getCurrentPlayer()).getSize(); i++){
            actions.add(new DiscardCard(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName));
        }

        //System.out.println(wgs.getPlayerHand(wgs.getCurrentPlayer()));
        //System.out.println(wgs.getCurrentPlayer());
        //System.out.println("LIST OF ACTIONS FOR CURRENT PLAYER: "+actions);
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        // In the _copy() method, return a new instance of the Forward Model object with any variables copied.
        return new Wonders7ForwardModel();
    }

    protected void createWonderDeck(Wonders7GameState wgs){
        // Create all the possible wonders a player could be assigned
        wgs.wonderBoardDeck.add(new Wonder7Board(Wonder7Board.wonder.colossus, createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore}, new int[]{4})), createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}))));
        //.wonderBoardDeck.add(new Wonder7Board(Wonder7Board.wonder.lighthouse));
        wgs.wonderBoardDeck.add(new Wonder7Board(Wonder7Board.wonder.temple, createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.papyrus}, new int[]{2})), createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{9}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}))));
        wgs.wonderBoardDeck.add(new Wonder7Board(Wonder7Board.wonder.pyramids, createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{4})), createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{5}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}))));
        wgs.wonderBoardDeck.add(new Wonder7Board(Wonder7Board.wonder.statue, createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore}, new int[]{2})), createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{}, new int[]{}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}))));
        //wgs.wonderBoardDeck.add(new Wonder7Board(Wonder7Board.wonder.mausoleum, createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore}, new int[]{4}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.textile}, new int[]{2})), createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{}, new int[]{}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}))));

        wgs.wonderBoardDeck.add(new Wonder7Board(Wonder7Board.wonder.colossus, createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore}, new int[]{4})), createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}))));
        wgs.wonderBoardDeck.add(new Wonder7Board(Wonder7Board.wonder.temple, createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.papyrus}, new int[]{2})), createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{9}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}))));
        wgs.wonderBoardDeck.add(new Wonder7Board(Wonder7Board.wonder.pyramids, createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{4})), createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{5}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}))));
        wgs.wonderBoardDeck.add(new Wonder7Board(Wonder7Board.wonder.statue, createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore}, new int[]{2})), createHashList(createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{}, new int[]{}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}))));

    }


    protected void createAgeDeck(Wonders7GameState wgs){
        // This method will create the deck for the current Era and
        // All the hashmaps containing different number of resources
        switch (wgs.currentAge) {
            // ALL THE CARDS IN DECK 1
            case 1:

                // Maybe remove these cards
                wgs.AgeDeck.add(new Wonder7Card("Timber Yard", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{1})));
                wgs.AgeDeck.add(new Wonder7Card("Clay Pit", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay}, new int[]{1})));
                wgs.AgeDeck.add(new Wonder7Card("Excavation", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{1})));
                wgs.AgeDeck.add(new Wonder7Card("Forest Cave", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{1})));
                wgs.AgeDeck.add(new Wonder7Card("Tree Farm", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{1})));
                wgs.AgeDeck.add(new Wonder7Card("Mine", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore}, new int[]{1})));

                for (int i = 0; i < 2; i++) {
                    // Raw Materials (Brown)
                    wgs.AgeDeck.add(new Wonder7Card("Lumber Yard", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("Ore Vein", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("Clay Pool", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("Stone Pit", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{1})));
                    // Manufactured Goods (Grey)
                    wgs.AgeDeck.add(new Wonder7Card("Loom", Wonder7Card.Wonder7CardType.ManufacturedGoods, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.textile}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("GlassWorks", Wonder7Card.Wonder7CardType.ManufacturedGoods, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.glass}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("Press", Wonder7Card.Wonder7CardType.ManufacturedGoods, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.papyrus}, new int[]{1})));
                    // Civilian Structures (Blue)
                    wgs.AgeDeck.add(new Wonder7Card("Altar", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{2})));
                    wgs.AgeDeck.add(new Wonder7Card("Theatre", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{2})));
                    wgs.AgeDeck.add(new Wonder7Card("Baths", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3})));
                    wgs.AgeDeck.add(new Wonder7Card("Pawnshop", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3})));
                    // Scientific Structures (Green)
                    wgs.AgeDeck.add(new Wonder7Card("Apothecary", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.textile}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.compass}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("Workshop", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.glass}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.cog}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("Scriptorium", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.papyrus}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.tablet}, new int[]{1})));
                    // Commercial Structures (Yellow)
                    // MilitaryStructures (Red)
                    wgs.AgeDeck.add(new Wonder7Card("Stockade", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("Barracks", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("Guard Tower", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{1})));

                }
                for (int i = 0; i < 3; i++) {
                    // Commercial Structures (Yellow)
                    wgs.AgeDeck.add(new Wonder7Card("Tavern", Wonder7Card.Wonder7CardType.CommercialStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{5})));
                }
                break;

            // ALL THE CARDS IN DECK 2
            case 2:

                // Extra cards for 6 players
                wgs.AgeDeck.add(new Wonder7Card("ForumG", Wonder7Card.Wonder7CardType.CommercialStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.glass}, new int[]{1})));
                wgs.AgeDeck.add(new Wonder7Card("ForumT", Wonder7Card.Wonder7CardType.CommercialStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.textile}, new int[]{1})));
                wgs.AgeDeck.add(new Wonder7Card("ForumP", Wonder7Card.Wonder7CardType.CommercialStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay}, new int[]{2}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.papyrus}, new int[]{1})));


                for (int i = 0; i < 2; i++) {
                    // Raw Materials (Brown)
                    wgs.AgeDeck.add(new Wonder7Card("Sawmill", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood}, new int[]{2})));
                    wgs.AgeDeck.add(new Wonder7Card("Foundry", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore}, new int[]{2})));
                    wgs.AgeDeck.add(new Wonder7Card("Brickyard", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay}, new int[]{2})));
                    wgs.AgeDeck.add(new Wonder7Card("Quarry", Wonder7Card.Wonder7CardType.RawMaterials, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.coin}, new int[]{1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{2})));
                    // Manufactured Goods (Grey)
                    wgs.AgeDeck.add(new Wonder7Card("Loom", Wonder7Card.Wonder7CardType.ManufacturedGoods, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.textile}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("GlassWorks", Wonder7Card.Wonder7CardType.ManufacturedGoods, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.glass}, new int[]{1})));
                    wgs.AgeDeck.add(new Wonder7Card("Press", Wonder7Card.Wonder7CardType.ManufacturedGoods, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.papyrus}, new int[]{1})));
                    // Civilian Structures (Blue)
                    wgs.AgeDeck.add(new Wonder7Card("Temple", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.clay, Wonders7Constants.resources.glass},  new int[]{1,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{3}), "Altar"));
                    wgs.AgeDeck.add(new Wonder7Card("Courthouse", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.textile},  new int[]{2,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{4}), "Scriptorium"));
                    wgs.AgeDeck.add(new Wonder7Card("Statue", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore, Wonders7Constants.resources.wood},  new int[]{2,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{4}), "Theatre"));
                    wgs.AgeDeck.add(new Wonder7Card("Aqueduct", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone},  new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{5}), "Baths"));
                    // Scientific Structures (Green)
                    wgs.AgeDeck.add(new Wonder7Card("Library", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone, Wonders7Constants.resources.textile}, new int[]{2,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.tablet}, new int[]{1}), "Scriptorium"));
                    wgs.AgeDeck.add(new Wonder7Card("Laboratory", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.papyrus}, new int[]{2,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.cog}, new int[]{1}), "Workshop"));
                    wgs.AgeDeck.add(new Wonder7Card("Dispensary", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore, Wonders7Constants.resources.glass}, new int[]{2,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.compass}, new int[]{1}), "Apothecary"));
                    wgs.AgeDeck.add(new Wonder7Card("School", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.papyrus}, new int[]{1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.tablet}, new int[]{1})));
                    // Commercial Structures (Yellow)
                    // MilitaryStructures (Red)
                    wgs.AgeDeck.add(new Wonder7Card("Stables", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.wood, Wonders7Constants.resources.ore}, new int[]{1,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{2}), "Apothecary"));
                    wgs.AgeDeck.add(new Wonder7Card("Archery Range", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.ore}, new int[]{2,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{2}), "Workshop"));
                    wgs.AgeDeck.add(new Wonder7Card("Walls", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone}, new int[]{3}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{2})));
                }
                for (int i = 0; i < 3; i++) {
                    // MilitaryStructures (Red)
                    wgs.AgeDeck.add(new Wonder7Card("Training Ground", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore, Wonders7Constants.resources.wood}, new int[]{2,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{2})));
                }
                break;

            // ALL THE CARDS IN DECK 3
            case 3:

                //Extra cards for 6 players
                wgs.AgeDeck.add(new Wonder7Card("Gardens", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.wood}, new int[]{2,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{5}), "Statue"));
                wgs.AgeDeck.add(new Wonder7Card("Senate", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.stone, Wonders7Constants.resources.ore}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{6}),"Library"));
                wgs.AgeDeck.add(new Wonder7Card("Town Hall", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone, Wonders7Constants.resources.ore, Wonders7Constants.resources.glass}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{6})));
                wgs.AgeDeck.add(new Wonder7Card("Pantheon", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.ore, Wonders7Constants.resources.glass, Wonders7Constants.resources.papyrus, Wonders7Constants.resources.textile}, new int[]{2,1,1,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}), "Temple"));
                wgs.AgeDeck.add(new Wonder7Card("University", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.papyrus, Wonders7Constants.resources.glass}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.tablet}, new int[]{1}),"Library"));
                wgs.AgeDeck.add(new Wonder7Card("Lodge", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.papyrus, Wonders7Constants.resources.textile}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.compass}, new int[]{1}), "Dispensary"));
                wgs.AgeDeck.add(new Wonder7Card("Study", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.papyrus, Wonders7Constants.resources.textile}, new int[]{1,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.cog}, new int[]{1}), "School"));
                wgs.AgeDeck.add(new Wonder7Card("Siege Workshop", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.wood}, new int[]{3,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{3}), "Laboratory"));
                wgs.AgeDeck.add(new Wonder7Card("Arsenal", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.ore, Wonders7Constants.resources.textile}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{3})));
                wgs.AgeDeck.add(new Wonder7Card("Fortification", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore, Wonders7Constants.resources.stone}, new int[]{3,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{3}), "Walls"));
                wgs.AgeDeck.add(new Wonder7Card("Circus", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone, Wonders7Constants.resources.ore}, new int[]{3,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{3}), "Training Ground"));

                for (int i = 0; i < 2; i++) {
                    // Civilian Structures (Blue)
                    wgs.AgeDeck.add(new Wonder7Card("Gardens", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.wood}, new int[]{2,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{5}), "Statue"));
                    wgs.AgeDeck.add(new Wonder7Card("Senate", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.stone, Wonders7Constants.resources.ore}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{6}),"Library"));
                    wgs.AgeDeck.add(new Wonder7Card("Pantheon", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.ore, Wonders7Constants.resources.glass, Wonders7Constants.resources.papyrus, Wonders7Constants.resources.textile}, new int[]{2,1,1,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{7}), "Temple"));
                    wgs.AgeDeck.add(new Wonder7Card("Palace", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone, Wonders7Constants.resources.ore, Wonders7Constants.resources.wood, Wonders7Constants.resources.clay, Wonders7Constants.resources.glass, Wonders7Constants.resources.papyrus, Wonders7Constants.resources.textile}, new int[]{1,1,1,1,1,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{8})));
                    // Scientific Structures (Green)
                    wgs.AgeDeck.add(new Wonder7Card("University", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.papyrus, Wonders7Constants.resources.glass}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.tablet}, new int[]{1}), "Library"));
                    wgs.AgeDeck.add(new Wonder7Card("Observatory", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore, Wonders7Constants.resources.glass, Wonders7Constants.resources.textile}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.cog}, new int[]{1}),"Laboratory"));
                    wgs.AgeDeck.add(new Wonder7Card("Lodge", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.papyrus, Wonders7Constants.resources.textile}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.compass}, new int[]{1}), "Dispensary"));
                    wgs.AgeDeck.add(new Wonder7Card("Study", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.papyrus, Wonders7Constants.resources.textile}, new int[]{1,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.cog}, new int[]{1}), "School"));
                    wgs.AgeDeck.add(new Wonder7Card("Academy", Wonder7Card.Wonder7CardType.ScientificStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone, Wonders7Constants.resources.glass}, new int[]{3,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.compass}, new int[]{1}), "School"));
                    // MilitaryStructures (Red)
                    wgs.AgeDeck.add(new Wonder7Card("Siege Workshop", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.clay, Wonders7Constants.resources.wood}, new int[]{3,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{3}), "Laboratory"));
                    wgs.AgeDeck.add(new Wonder7Card("Fortification", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.ore, Wonders7Constants.resources.stone}, new int[]{3,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{3}), "Walls"));

                }
                for (int i = 0; i < 3; i++) {
                    // Civilian Structures (Blue)
                    wgs.AgeDeck.add(new Wonder7Card("Town Hall", Wonder7Card.Wonder7CardType.CivilianStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone, Wonders7Constants.resources.ore, Wonders7Constants.resources.glass}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.victory}, new int[]{6})));

                    // MilitaryStructures (Red)
                    wgs.AgeDeck.add(new Wonder7Card("Arsenal", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.wood, Wonders7Constants.resources.ore, Wonders7Constants.resources.textile}, new int[]{2,1,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{3})));
                    wgs.AgeDeck.add(new Wonder7Card("Circus", Wonder7Card.Wonder7CardType.MilitaryStructures, createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.stone, Wonders7Constants.resources.ore}, new int[]{3,1}), createCardHash(new Wonders7Constants.resources[]{Wonders7Constants.resources.shield}, new int[]{3}), "Training Ground"));
                }
        }

    }

    protected HashMap<Wonders7Constants.resources, Integer> createCardHash(Wonders7Constants.resources[] resource, int[] number){
        // This will have to create the resource hashmaps for each card and return them
        HashMap<Wonders7Constants.resources, Integer> card = new HashMap<>();
        for (int i=0; i < number.length; i++){
            card.put(resource[i], number[i]);
        }
        return card;
    }

    protected ArrayList<HashMap<Wonders7Constants.resources, Integer>> createHashList(HashMap<Wonders7Constants.resources, Integer>... hashmaps){
        ArrayList<HashMap<Wonders7Constants.resources, Integer>> list = new ArrayList<>();
        for (HashMap<Wonders7Constants.resources, Integer> map : hashmaps) {
            list.add(map);
        }
        return list;
    }


    protected void checkAgeEnd(AbstractGameState gameState){
        Wonders7GameState wgs = (Wonders7GameState) gameState;
        if (wgs.getPlayerHand(wgs.getCurrentPlayer()).getSize() == 1){  // If all players hands are empty

            for (int i=0; i< wgs.getNPlayers(); i++){
                wgs.getDiscardPile().add(wgs.getPlayerHand(i).get(0));
                wgs.getPlayerHand(i).remove(0);
            }

            wgs.getTurnOrder().endRound(wgs); // Ends the round,
            ((Wonders7TurnOrder) wgs.getTurnOrder()).reverse(); // Turn Order reverses at end of Age

            // Resolves military conflicts
            for (int i=0; i< wgs.getNPlayers(); i++){
                int nextplayer = (i+1)% wgs.getNPlayers();
                if(wgs.getPlayerResources(i).get(Wonders7Constants.resources.shield) > wgs.getPlayerResources(nextplayer).get(Wonders7Constants.resources.shield)){ // IF PLAYER i WINS
                    wgs.getPlayerResources(i).put(Wonders7Constants.resources.victory,  wgs.getPlayerResources(i).get(Wonders7Constants.resources.victory)+(2*wgs.currentAge-1)); // 2N-1 POINTS FOR PLAYER i
                    wgs.getPlayerResources(nextplayer).put(Wonders7Constants.resources.victory,  wgs.getPlayerResources(nextplayer).get(Wonders7Constants.resources.victory)-1); // -1 FOR THE PLAYER i+1
                }
                else if (wgs.getPlayerResources(i).get(Wonders7Constants.resources.shield) < wgs.getPlayerResources(nextplayer).get(Wonders7Constants.resources.shield)){ // IF PLAYER i+1 WINS
                    wgs.getPlayerResources(i).put(Wonders7Constants.resources.victory,  wgs.getPlayerResources(i).get(Wonders7Constants.resources.victory)-1);// -1 POINT FOR THE PLAYER i
                    wgs.getPlayerResources(nextplayer).put(Wonders7Constants.resources.victory,  wgs.getPlayerResources(nextplayer).get(Wonders7Constants.resources.victory)+(2*wgs.currentAge-1));// 2N-1 POINTS FOR PLAYER i+1
                }
            }

            wgs.getAgeDeck().clear();
            wgs.currentAge += 1; // Next age starts
            checkGameEnd(wgs); // Checks if the game has ended!
        }

    }

    protected void checkGameEnd(Wonders7GameState wgs){
        if (wgs.currentAge == 4){
            // Calculate victory points in order of:
            // treasury, scientific, commercial and finally guilds
            for (int i=0; i< wgs.getNPlayers(); i++){
                // Treasury
                wgs.getPlayerResources(i).put(Wonders7Constants.resources.victory, wgs.getPlayerResources(i).get(Wonders7Constants.resources.victory)+wgs.getPlayerResources(i).get(Wonders7Constants.resources.coin)/3);
                // Scientific
                wgs.getPlayerResources(i).put(Wonders7Constants.resources.victory, wgs.getPlayerResources(i).get(Wonders7Constants.resources.victory)+(int)Math.pow(wgs.getPlayerResources(i).get(Wonders7Constants.resources.cog),2));
                wgs.getPlayerResources(i).put(Wonders7Constants.resources.victory, wgs.getPlayerResources(i).get(Wonders7Constants.resources.victory)+(int)Math.pow(wgs.getPlayerResources(i).get(Wonders7Constants.resources.compass),2));
                wgs.getPlayerResources(i).put(Wonders7Constants.resources.victory, wgs.getPlayerResources(i).get(Wonders7Constants.resources.victory)+(int)Math.pow(wgs.getPlayerResources(i).get(Wonders7Constants.resources.tablet),2));
                wgs.getPlayerResources(i).put(Wonders7Constants.resources.victory, wgs.getPlayerResources(i).get(Wonders7Constants.resources.victory)+7*Math.min(Math.min(wgs.getPlayerResources(i).get(Wonders7Constants.resources.cog),wgs.getPlayerResources(i).get(Wonders7Constants.resources.compass)),wgs.getPlayerResources(i).get(Wonders7Constants.resources.tablet))); // Sets of different science symbols

            }

            int winner = 0;
            for (int i=0; i<wgs.getNPlayers(); i++){
                // If a player has more victory points
                if (wgs.getPlayerResources(i).get(Wonders7Constants.resources.victory) > wgs.getPlayerResources(winner).get(Wonders7Constants.resources.victory)){
                    wgs.setPlayerResult(Utils.GameResult.LOSE,winner); // SETS PREVIOUS WINNER AS LOST
                    wgs.setPlayerResult(Utils.GameResult.WIN,i); // SETS NEW WINNER AS PLAYER i
                    winner = i;
                }
                // In a tie, break with coins
                else if (wgs.getPlayerResources(i).get(Wonders7Constants.resources.victory) == wgs.getPlayerResources(winner).get(Wonders7Constants.resources.victory)){
                    if (wgs.getPlayerResources(i).get(Wonders7Constants.resources.coin) >= wgs.getPlayerResources(winner).get(Wonders7Constants.resources.coin)){
                        wgs.setPlayerResult(Utils.GameResult.LOSE,winner);
                        wgs.setPlayerResult(Utils.GameResult.WIN,i);
                        winner = i;
                    }
                    else {wgs.setPlayerResult(Utils.GameResult.LOSE,i);}
                }
                else {
                    wgs.setPlayerResult(Utils.GameResult.LOSE,i); // Sets this player as LOST
                }
            }

            wgs.setGameStatus(Utils.GameResult.GAME_END);
            /*System.out.println("");
            System.out.println("!---------------------------------------- THE FINAL AGE HAS ENDED!!! ----------------------------------------!");
            System.out.println("");
            System.out.println("The winner is Player  " + winner +"!!!!"); */
        }
        else{
            /*System.out.println("");
            System.out.println("!---------------------------------------- AGE "+wgs.currentAge+" HAS NOW STARTED!!!!! ----------------------------------------!");
            System.out.println(""); */
            ageSetup(wgs);

            for (int player=0; player < wgs.getNPlayers(); player++){
                if (wgs.getPlayerWonderBoard(player).wonderStage > 2){
                    Wonder7Board board = wgs.getPlayerWonderBoard(player);
                    switch (board.type){
                        case lighthouse:
                        case mausoleum:
                        case gardens:
                        case statue:
                            wgs.getPlayerWonderBoard(player).effectUsed = false;
                        default:
                            break;
                    }
                }
            }
        }
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        //System.out.println("");
        //for (int i = 0; i < wgs.getNPlayers(); i++) {
            //System.out.println(wgs.getPlayerWonderBoard(i).wonderName+" ["+(wgs.getPlayerWonderBoard(i).wonderStage-1) +"] "+ i + " --> " + wgs.getPlayerResources(i) + " --PLAYED CARDS--> " + wgs.getPlayedCards(i));
        //}
        // You may override the endGame() method if your game requires any extra end of game computation (e.g. to update the status of players still in the game to winners).
        // !!!
        // Note: Forward model classes can instead extend from the core.rules.AbstractRuleBasedForwardModel.java abstract class instead, if they wish to use the rule-based system instead; this class provides basic functionality and documentation for using rules and an already implemented _next() function.
        // !!!
    }
}

