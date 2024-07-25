package games.cluedo;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.BoardNode;
import games.GameType;
import games.cluedo.actions.ChooseCharacter;
import games.cluedo.actions.GuessPartOfCaseFile;
import games.cluedo.cards.CluedoCard;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.AssertJUnit.assertEquals;

public class TestCluedo {
    Game cluedo;
    CluedoForwardModel cfm = new CluedoForwardModel();
    List<AbstractPlayer> players;
    AbstractParameters gameParameters;

    @Before
    public void setup() {
        // Feel free to change how many characters this game is set up with
        // The tests should all work with different numbers of players
        players = List.of(
//                new RandomPlayer(),
//                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer()
        );
        gameParameters = new CluedoParameters();
        cluedo = GameType.Cluedo.createGameInstance(players.size(),100, gameParameters);
        cluedo.reset(players);
    }

    @Test
    public void testSetupCaseFile() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        // Assert caseFile has 3 cards: 1 Room card, 1 Character card, 1 Weapon card
        assertEquals(3, cgs.caseFile.getSize());
        assert(CluedoConstants.Weapon.contains(cgs.caseFile.get(0).getName()));
        assert(CluedoConstants.Character.contains(cgs.caseFile.get(1).getName()));
        assert(CluedoConstants.Room.contains(cgs.caseFile.get(2).getName()));

    }

    @Test
    public void testSetupPlayerHands() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        // Assert players have been dealt the expected number of cards
        // (assuming there are 21 cards per the original game)
        List<Integer> actualPlayerHandSize = new ArrayList<>();
        for (int i=0; i<cgs.getNPlayers(); i++) {
            actualPlayerHandSize.add(cgs.playerHandCards.get(i).getSize());
        }
        List<Integer> expectedPlayerHandSize = switch (cgs.getNPlayers()) {
            case 2 -> List.of(9, 9);
            case 3 -> List.of(6, 6, 6);
            case 4 -> List.of(5, 5, 4, 4);
            case 5 -> List.of(4, 4, 4, 3, 3);
            case 6 -> List.of(3, 3, 3, 3, 3, 3);
            default -> List.of();
        };
        assertEquals(expectedPlayerHandSize, actualPlayerHandSize);
    }

    @Test
    public void testSetupGameBoard() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        // Assert gameBoard is a fully connected graph with (#ofRooms + 1) nodes
        assert(cgs.gameBoard.getBoardNodes().size() == CluedoConstants.Room.values().length + 1);
        for (BoardNode node : cgs.gameBoard.getBoardNodes()) {
            // Correct number of neighbours
            assert(node.getNeighbours().size() == CluedoConstants.Room.values().length);
            // No node is a neighbour to itself
            assert(!(node.getNeighbours().contains(node)));
            // All neighbours are distinct
            assertEquals(node.getNeighbours().stream().distinct().collect(Collectors.toList()), node.getNeighbours().stream().toList());
        }
    }

    @Test
    public void testSetupCharacterLocation() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        // Assert all characters start in the START node
        List<String> actualCharacterLocations = new ArrayList<>();
        for (int i=0; i<CluedoConstants.Character.values().length; i++) {
            actualCharacterLocations.add(cgs.characterLocations.get(i).getComponentName());
        }
        List<String> expectedCharacterLocations = List.of("START", "START", "START", "START", "START", "START");
        assertEquals(expectedCharacterLocations, actualCharacterLocations);
    }

    @Test
    public void testSetupGamePhase() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();
        assertEquals(CluedoGameState.CluedoGamePhase.chooseCharacter, cgs.getGamePhase());
    }

    @Test
    public void testChoosingCharacters() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        // Assert the first player to choose a character can choose any character
        List<AbstractAction> actions0 = cfm.computeAvailableActions(cgs);
        assertEquals(List.of(0,1,2,3,4,5), computeAvailableCharacterIndexes(actions0));

        // Suppose the first player chooses REV_GREEN
        cfm.next(cgs, cfm.computeAvailableActions(cgs).get(3));

        // Assert we have moved to the second player
        assertEquals(1, cgs.turnOrder.getTurnOwner());
        // Assert the second player can choose from any character except REV_GREEN
        List<AbstractAction> actions1 = cfm.computeAvailableActions(cgs);
        assertEquals(List.of(0,1,2,4,5), computeAvailableCharacterIndexes(actions1));

        // Suppose the second player chooses COL_MUSTARD
        cfm.next(cgs, cfm.computeAvailableActions(cgs).get(1));

        if (players.size() != 2) {
            // Assert we have moved to the third player
            assertEquals(2, cgs.turnOrder.getTurnOwner());
            // Assert the third player can choose from any character except REV_GREEN and COL_MUSTARD
            List<AbstractAction> actions2 = cfm.computeAvailableActions(cgs);
            assertEquals(List.of(0,2,4,5), computeAvailableCharacterIndexes(actions2));
        }

        // Suppose the third, fourth, etc. players have chosen MISS_SCARLETT, DR_ORCHID, MRS_PEACOCK, PROF_PLUM
        while (cgs.characterToPlayerMap.size() < cgs.getNPlayers()) {
            cfm.next(cgs, cfm.computeAvailableActions(cgs).get(0));
        }

        // Assert we have moved to the next phase of the game
        assertEquals(CluedoGameState.CluedoGamePhase.makeSuggestion, cgs.getGamePhase());
        // Assert characterToPlayerMap is correctly filled
        HashMap<Integer, Integer> expectedCharacterToPlayerMap = new HashMap<>() {{
            put(0,2); put(1,1); put(2,3); put(3,0); put(4,4); put(5,5);
        }};
        for (int i=0; i<6; i++) {
            if (cgs.characterToPlayerMap.containsKey(i)) {
                assertEquals(expectedCharacterToPlayerMap.get(i), cgs.characterToPlayerMap.get(i));
            }
        }

        // Assert the next player to have a turn is the one playing the character with the lowest index
        int expectedFirstPlayer;
        if (players.size() == 2) { expectedFirstPlayer = 1; }
        else { expectedFirstPlayer = 2; }
        assertEquals(expectedFirstPlayer, cgs.turnOrder.getTurnOwner());
    }

    private List<Integer> computeAvailableCharacterIndexes(List<AbstractAction> actions) {
        List<Integer> availableCharacterIndexes = new ArrayList<>();
        for (AbstractAction availableAction : actions) {
            availableCharacterIndexes.add(((ChooseCharacter) availableAction).getCharacter());
        }
        return availableCharacterIndexes;
    }

    @Test
    public void testMakingSuggestion() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        List<Integer> playerToCharacter = List.of(3,1,0,2,4,5);
        for (int i = 0; i < players.size(); i++) {
            cgs.characterToPlayerMap.put(playerToCharacter.get(i), i);
        }
        cgs.turnOrder.setTurnOrder(cgs.characterToPlayerMap);
        if (players.size() == 2) { cgs.turnOrder.setTurnOwner(1); }
        else { cgs.turnOrder.setTurnOwner(2); }
        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeSuggestion);

        // ^^^^^ SETUP ^^^^^

        // Assert player can choose any room
        List<AbstractAction> actions0 = cfm.computeAvailableActions(cgs);
        System.out.println(computeAvailableGuessNames(actions0));
        for (CluedoConstants.Room room : CluedoConstants.Room.values()) {
            assert(computeAvailableGuessNames(actions0).contains(room.name()));
        }

        // Suppose the player chose the LOUNGE
        cfm.next(cgs, cfm.computeAvailableActions(cgs).get(2));

        // Assert player can choose any character
        List<AbstractAction> actions1 = cfm.computeAvailableActions(cgs);
        for (CluedoConstants.Character character : CluedoConstants.Character.values()) {
            assert(computeAvailableGuessNames(actions1).contains(character.name()));
        }
        // Suppose the character adds DR_ORCHID and CANDLESTICK
        cfm.next(cgs, cfm.computeAvailableActions(cgs).get(2));
        cfm.next(cgs, cfm.computeAvailableActions(cgs).get(4));

        // Assert we have moved onto the next phase of the game
        assertEquals(CluedoGameState.CluedoGamePhase.revealCards, cgs.getGamePhase());

        // Assert currentGuess contains the expected 3 cards
        assertEquals(3, cgs.currentGuess.getSize());
        assertEquals(CluedoConstants.Weapon.CANDLESTICK.name(), cgs.currentGuess.get(0).getName());
        assertEquals(CluedoConstants.Character.DR_ORCHID.name(), cgs.currentGuess.get(1).getName());
        assertEquals(CluedoConstants.Room.LOUNGE.name(), cgs.currentGuess.get(2).getName());

        // Assert the next player to have a turn is the one playing the character with the second-lowest index
        int expectedNextPlayer;
        if (players.size() == 2) { expectedNextPlayer = 0; }
        else { expectedNextPlayer = 1; }
        assertEquals(expectedNextPlayer, cgs.turnOrder.getTurnOwner());
    }

    private List<String> computeAvailableGuessNames(List<AbstractAction> actions) {
        List<String> availableGuessNames = new ArrayList<>();
        for (AbstractAction availableAction : actions) {
            availableGuessNames.add(((GuessPartOfCaseFile) availableAction).getGuessName());
        }
        return availableGuessNames;
    }


}
