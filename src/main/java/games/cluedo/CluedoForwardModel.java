package games.cluedo;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.PartialObservableDeck;
import games.cluedo.cards.CluedoCard;
import games.mastermind.MMGameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CluedoForwardModel extends StandardForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {
        CluedoGameState cgs = (CluedoGameState) firstState;

        cgs.playerHandCards = new ArrayList<PartialObservableDeck<CluedoCard>>();
        cgs.caseFile = new PartialObservableDeck<CluedoCard>("Case File", -1, new boolean[cgs.getNPlayers()]);
        cgs.gameBoard = new GraphBoard("Game Board");
        cgs.playerLocations = new ArrayList<BoardNode>();

        cgs.gameBoard.addBoardNode(new BoardNode(CluedoConstants.Room.values().length, "START"));
        for (CluedoConstants.Room room : CluedoConstants.Room.values()) {
            BoardNode roomNode = new BoardNode(CluedoConstants.Room.values().length, room.name());
            cgs.gameBoard.addBoardNode(roomNode);
        }

        for (BoardNode node1 : cgs.gameBoard.getBoardNodes()) {
            for (BoardNode node2 : cgs.gameBoard.getBoardNodes()) {

            }
        }


        /*
        1 place all 6 tokens in start node
        5 shuffle characterCards, weaponCards, and roomCards, leave hidden
        6 take top card from the 3 decks and put them into the caseFile, leave hidden
        8 shuffle remaining character, weapon and room cards together
        9a if (2 players) { put top 4 cards into 4 random rooms }
        9b Deal remaining deck amongst players (all cards dealt)
        9 give every player a detectiveNotebook
         */
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return List.of();
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {

    }

    private boolean checkAndProcessGameEnd(MMGameState gameState) {
        return false;
    }
}
