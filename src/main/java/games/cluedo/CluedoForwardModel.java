package games.cluedo;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.PartialObservableDeck;
import games.cluedo.actions.*;
import games.cluedo.cards.*;

import java.util.*;

public class CluedoForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        CluedoGameState cgs = (CluedoGameState) firstState;

        cgs.gameBoard = new GraphBoard("Game Board");
        cgs.characterToPlayerMap = new HashMap<>();
        cgs.characterLocations = new ArrayList<>();
        cgs.playerHandCards = new ArrayList<>();
        cgs.caseFile = new PartialObservableDeck<>("Case File", -1, new boolean[cgs.getNPlayers()]);
        cgs.currentGuess = new PartialObservableDeck<>("Current Guess", -1, new boolean[cgs.getNPlayers()]);

        // Initialise the board // TODO change the initialisation so that we can include 'corridor' segments
        BoardNode startNode = new BoardNode(CluedoConstants.Room.values().length, "START");
        cgs.gameBoard.addBoardNode(startNode);
        // For each room, create a BoardNode
        for (CluedoConstants.Room room : CluedoConstants.Room.values()) {
            BoardNode roomNode = new BoardNode(CluedoConstants.Room.values().length, room.name());
            cgs.gameBoard.addBoardNode(roomNode);
            // Add an edge to all previous nodes in the GraphBoard
            for (BoardNode node : cgs.gameBoard.getBoardNodes()) {
                if (!node.equals(roomNode)) {
                    cgs.gameBoard.addConnection(node, roomNode);
                }
            }
        }

        // All characters start in the startNode node
        // Replaces the fact that in the original game, characters start in given positions in the corridor
        // Including a startNode ensures characters don't start in a room
        for (CluedoConstants.Character character : CluedoConstants.Character.values()) {
            cgs.characterLocations.add(startNode.getComponentName());
        }

        // Randomly choose the Character, Weapon and Room to go into the caseFile
        CluedoConstants.Character randomCharacter = CluedoConstants.Character.values()[cgs.getRnd().nextInt(CluedoConstants.Character.values().length)];
        CluedoConstants.Weapon randomWeapon = CluedoConstants.Weapon.values()[cgs.getRnd().nextInt(CluedoConstants.Weapon.values().length)];
        CluedoConstants.Room randomRoom = CluedoConstants.Room.values()[cgs.getRnd().nextInt(CluedoConstants.Room.values().length)];

        CluedoCard randomCharacterCard = new CharacterCard(randomCharacter);
        CluedoCard randomWeaponCard = new WeaponCard(randomWeapon);
        CluedoCard randomRoomCard = new RoomCard(randomRoom);

        cgs.caseFile.add(randomRoomCard, new boolean[cgs.getNPlayers()]);
        cgs.caseFile.add(randomCharacterCard, new boolean[cgs.getNPlayers()]);
        cgs.caseFile.add(randomWeaponCard, new boolean[cgs.getNPlayers()]);

        // Create the deck of all cards (Characters, Weapons and Rooms) without the 3 cards in the caseFile
        cgs.allCards = new PartialObservableDeck<>("All Cards", -1, new boolean[cgs.getNPlayers()]);
        for (CluedoConstants.Character character : CluedoConstants.Character.values()) {
            if (!character.equals(randomCharacter)) {
                cgs.allCards.add(new CharacterCard(character));
            }
        }
        for (CluedoConstants.Weapon weapon : CluedoConstants.Weapon.values()) {
            if (!weapon.equals(randomWeapon)) {
                cgs.allCards.add(new WeaponCard(weapon));
            }
        }
        for (CluedoConstants.Room room : CluedoConstants.Room.values()) {
            if (!room.equals(randomRoom)) {
                cgs.allCards.add(new RoomCard(room));
            }
        }

        // Shuffle the deck of cards and deal them out to players
        cgs.allCards.shuffle(cgs.getRnd());

        for (int i=0; i<cgs.getNPlayers(); i++) {
            boolean[] visible = new boolean[cgs.getNPlayers()];
            visible[i] = true;
            PartialObservableDeck<CluedoCard> playerCards = new PartialObservableDeck<>("Player Cards", i, visible);
            cgs.playerHandCards.add(playerCards);
        }

        int cardCount = 0;
        while (cardCount < cgs.allCards.getSize()) {
            int playerId = cardCount % cgs.getNPlayers();
            CluedoCard c = cgs.allCards.peek(cardCount);
            c.setOwnerId(playerId);
            cgs.playerHandCards.get(playerId).add(c);
            cardCount += 1;
        }

        // Add the cards in the caseFile to the deck of all cards for completeness
        cgs.allCards.add(randomRoomCard, new boolean[cgs.getNPlayers()]);
        cgs.allCards.add(randomCharacterCard, new boolean[cgs.getNPlayers()]);
        cgs.allCards.add(randomWeaponCard, new boolean[cgs.getNPlayers()]);

        // Set the first gamePhase to be choosing characters
        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.chooseCharacter);

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();

        System.out.println("========== "+ cgs.getGamePhase()+" "+cgs.turnOrder.getTurnOwner()+ " ==========");

        if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.chooseCharacter) {
            // Player can choose any unchosen character
            for (int i=0; i<CluedoConstants.Character.values().length; i++) {
                if (!cgs.characterToPlayerMap.containsKey(i)) {
                    actions.add(new ChooseCharacter(cgs.turnOrder.getTurnOwner(), i));
                }
            }

        } else if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.makeSuggestion
                    || cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.makeAccusation) {
            int currentGuessSize = cgs.currentGuess.getSize();

            if (currentGuessSize == 0) { // First, the player guesses a room
                for (CluedoConstants.Room room : CluedoConstants.Room.values()) {
                    actions.add(new GuessPartOfCaseFile(cgs, room));
                }
            } else if (currentGuessSize == 1) { // Second, the player guesses a character
                for (CluedoConstants.Character character : CluedoConstants.Character.values()) {
                    actions.add(new GuessPartOfCaseFile(cgs, character));
                }
            } else if (currentGuessSize == 2) { // Finally, the player guesses a weapon
                for (CluedoConstants.Weapon weapon : CluedoConstants.Weapon.values()) {
                    actions.add(new GuessPartOfCaseFile(cgs, weapon));
                }
            }

        } else if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.revealCards) {
            if (cgs.turnOrder.getTurnOwner() == cgs.currentTurnPlayerId) {
                actions.add(new DoNothing());
            } else {
                PartialObservableDeck<CluedoCard> playerHand = cgs.getPlayerHandCards().get(cgs.turnOrder.getTurnOwner());
                for (int i=0; i<playerHand.getSize(); i++) {
                    if (cgs.currentGuess.contains(playerHand.get(i))) {
                        actions.add(new ShowHintCard(cgs.turnOrder.getTurnOwner(), cgs.currentTurnPlayerId, i));
                    }
                }
                if (actions.isEmpty()) {
                    actions.add(new DoNothing());
                }
            }
        }

        if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.makeAccusation
                && cgs.currentGuess.getSize() == 0) {
            actions.add(new DoNothing()); // choose to not accuse
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        CluedoGameState cgs = (CluedoGameState) gameState;

        if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.chooseCharacter) {
            cgs.turnOrder.setTurnOwner(cgs.turnOrder.nextPlayer(cgs));
            if (cgs.characterToPlayerMap.size() == cgs.getNPlayers()) {
                // Change turn order to be in order of character index, rather than playerId
                cgs.turnOrder.setTurnOrder(cgs.characterToPlayerMap);

                // set the next player
                cgs.turnOrder.setTurnOwner(cgs.turnOrder.getNextPlayer());

                cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeSuggestion);

                System.out.println(cgs.characterToPlayerMap);
            }

        } else if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.makeSuggestion) {
            cgs.currentTurnPlayerId = cgs.turnOrder.getTurnOwner();
            if (cgs.currentGuess.getSize() == 1) { // last action was to suggest a room
                // Get current character index
                int currentCharacterIndex = -1;
                for (int key : cgs.characterToPlayerMap.keySet()) {
                    if (cgs.characterToPlayerMap.get(key) == cgs.turnOrder.getTurnOwner()) {
                        currentCharacterIndex = key;
                    }
                }
                // Move the character to the room
                cgs.characterLocations.set(currentCharacterIndex, cgs.currentGuess.get(0).toString());
            }
            if (cgs.currentGuess.getSize() == 2) { // last action was to suggest a character
                // Get suggested character index
                int suggestedCharacterIndex = CluedoConstants.Character.valueOf(cgs.currentGuess.get(0).toString()).ordinal();
                // Move the character to the room
                cgs.characterLocations.set(suggestedCharacterIndex, cgs.currentGuess.get(1).toString());
            }
            if (cgs.currentGuess.getSize() == 3) { // last action was to suggest a weapon
                // add all players to reactive turn order (excluding current player) in order of character index
                cgs.turnOrder.addReactivePlayer(cgs.turnOrder.playerQueue);
                // set the next player
                cgs.turnOrder.setTurnOwner(cgs.turnOrder.getNextPlayer());

                cgs.setGamePhase(CluedoGameState.CluedoGamePhase.revealCards);
            }

        } else if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.revealCards) {
            if (cgs.turnOrder.getTurnOwner() == cgs.currentTurnPlayerId) {
                cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeAccusation);
                cgs.currentGuess.clear();
            } else {
                if (action instanceof DoNothing) {
                    cgs.turnOrder.setTurnOwner(cgs.turnOrder.getNextPlayer());
                } else {
                    cgs.turnOrder.resetReactivePlayers();
                    cgs.turnOrder.setTurnOwner(cgs.currentTurnPlayerId);
                    cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeAccusation);
                    cgs.currentGuess.clear();
                }
            }

        } else if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.makeAccusation) {
            if (action instanceof DoNothing) {
                cgs.turnOrder.setTurnOwner(cgs.turnOrder.getNextPlayer());
                cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeSuggestion);
                cgs.currentGuess.clear();
            }
            if (cgs.currentGuess.getSize() == 3) {
                System.out.println(guessMatchesCaseFile(cgs));
                if (guessMatchesCaseFile(cgs)) {
                    cgs.setGameStatus(CoreConstants.GameResult.GAME_END);
                    for (int i=0; i < cgs.getNPlayers(); i++) {
                        cgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, i);
                    }
                    cgs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, cgs.turnOrder.getTurnOwner());
                    System.out.println("WIN");
                } else {
                    System.out.println("in failed accusation");
                    cgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, cgs.turnOrder.getTurnOwner());

                    if (!Arrays.asList(cgs.getPlayerResults()).contains(CoreConstants.GameResult.GAME_ONGOING)) {
                        cgs.setGameStatus(CoreConstants.GameResult.GAME_END);
                        System.out.println("in gameEnd");
                        for (int i=0; i < cgs.getNPlayers(); i++) {
                            cgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, i);
                        }
                    } else {
                        System.out.println("in gameContinue");
                        int nextPlayer = cgs.turnOrder.getNextPlayer();
                        while (!cgs.getPlayerResults()[nextPlayer].equals(CoreConstants.GameResult.GAME_ONGOING)) {
                            nextPlayer = cgs.turnOrder.getNextPlayer();
                            System.out.println("in while");
                        }
                        cgs.turnOrder.setTurnOwner(nextPlayer);
                        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeSuggestion);
                        cgs.currentGuess.clear();
                    }
                }
            }
        }
    }

    private boolean guessMatchesCaseFile(AbstractGameState gameState) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        return cgs.currentGuess.equals(cgs.caseFile);
    }

}
