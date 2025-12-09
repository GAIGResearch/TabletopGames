package games.pickomino.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.pickomino.PickominoGameState;
import games.pickomino.PickominoTile;

/**
 * This action occurs when the dice values make it impossible to select a dice value.
 * The player loses his top tile and his turn is over.
 */
public class NullTurn extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        PickominoGameState pgs = (PickominoGameState) gs;
        PickominoTile tile = pgs.playerTiles.get(pgs.getCurrentPlayer()).draw();
        if (tile != null) { // if the player has a tile
            // insert the tile in the remaining tiles deck to keep it sorted
            // and remove the highest tile from the remaining tiles deck
            assert pgs.remainingTiles.getSize() > 0 : "No tiles remaining when re-inserting player's lost tile";
            PickominoTile highestTile = pgs.remainingTiles.peek(pgs.remainingTiles.getSize() - 1);
            if(highestTile.getValue() < tile.getValue()) {
                pgs.remainingTiles.addToBottom(tile);
            } else {
                for(int i = pgs.remainingTiles.getSize() - 1; i >= 0; i--) {
                    if(pgs.remainingTiles.get(i).getValue() < tile.getValue()) {
                        pgs.remainingTiles.add(tile, i + 1);
                        break;
                    }
                }
                pgs.remainingTiles.remove(pgs.remainingTiles.getSize() - 1);
            }
            if (pgs.getCoreGameParameters().verbose) {
                System.out.println("p" + pgs.getCurrentPlayer() + " loses turn and returns tile " + 
                    tile.getValue() + " (score: " + tile.getScore() + ") to remaining tiles");
            }
        } else {
            if (pgs.getCoreGameParameters().verbose) {
                System.out.println("p" + pgs.getCurrentPlayer() + " loses turn (no tile to return)");
            }
        } // if the player has no tile, do nothing

        return true;
    }

    @Override
    public NullTurn copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof NullTurn;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "NullTurn";
    }
}
