package games.cluedo;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.BoardNode;
import core.components.Deck;
import core.components.GraphBoard;
import core.components.PartialObservableDeck;
import games.cluedo.actions.*;
import games.cluedo.cards.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CluedoForwardModel extends AbstractForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {
        CluedoGameState cgs = (CluedoGameState) firstState;

        cgs.characterToPlayerMap = new HashMap<>();
        cgs.playerHandCards = new ArrayList<PartialObservableDeck<CluedoCard>>();
        cgs.caseFile = new PartialObservableDeck<CluedoCard>("Case File", -1, new boolean[cgs.getNPlayers()]);
        cgs.gameBoard = new GraphBoard("Game Board");
        cgs.characterLocations = new ArrayList<BoardNode>();
        cgs.currentGuess = new PartialObservableDeck<CluedoCard>("Current Guess", -1, new boolean[cgs.getNPlayers()]);

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
            cgs.characterLocations.add(startNode);
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
        cgs.allCards = new PartialObservableDeck<CluedoCard>("All Cards", -1, new boolean[cgs.getNPlayers()]);
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
    protected void _next(AbstractGameState gameState, AbstractAction action) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        // TODO _next
        // Note! once chooseCharacter gamePhase ends, set currentTurnPlayerId to cgs.characterToPlayerMap.get(0);
        // ie first player is the person playing Scarlett
        // OR be smarter and set it to the first character in the turn order that has an associated player
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();

        if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.chooseCharacter) {
            // Player can choose any unchosen character
            for (int i=0; i<CluedoConstants.Character.values().length; i++) {
                if (!cgs.characterToPlayerMap.containsKey(i)) {
                    actions.add(new ChooseCharacter(i, cgs.getCurrentPlayer()));
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
            PartialObservableDeck<CluedoCard> playerHand = cgs.getPlayerHandCards().get(cgs.getCurrentPlayer());
            for (int i=0; i<playerHand.getSize(); i++) {
                if (cgs.currentGuess.contains(playerHand.get(i))) {
                    actions.add(new ShowHintCard(cgs.getCurrentPlayer(), cgs.currentTurnPlayerId, i));
                }
            }
            if (actions.isEmpty()) {
                actions.add(new DoNothing());
            }
        }

        return actions;
    }

    @Override
    protected void endPlayerTurn(AbstractGameState state) {
        CluedoGameState cgs = (CluedoGameState) state;
        cgs.getTurnOrder().endPlayerTurn(cgs);
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new CluedoForwardModel();
    }
}
