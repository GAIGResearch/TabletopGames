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
        // fetch the lost tile and remove it from the player's tiles deck
        PickominoTile lostTile = pgs.playerTiles.get(pgs.getCurrentPlayer()).draw();
        if (lostTile != null) { // if the player has a tile
            // Reinsert the lost tile and remove the highest-value tile from remainingTiles
            // (no ordering assumption on the deck; we search for the current highest each time)
            assert pgs.remainingTiles.getSize() > 0 : "No tiles remaining when re-inserting player's lost tile";
            // search for the tile with the highest value in the remaining tiles deck and for its index
            int highestTileIndex = -1;
            int highestTileValue = Integer.MIN_VALUE;
            for(int i = 0; i < pgs.remainingTiles.getSize(); i++){
                if(pgs.remainingTiles.peek(i).getValue() > highestTileValue) {
                    highestTileIndex = i;
                    highestTileValue = pgs.remainingTiles.peek(i).getValue();
                }
            }
            assert highestTileIndex != -1 : "No tile left in remaining tiles deck";

            if(highestTileValue < lostTile.getValue()) {
                pgs.remainingTiles.add(lostTile);
                if(pgs.getCoreGameParameters().verbose) {
                    System.out.println("p" + pgs.getCurrentPlayer() + " loses turn and returns tile (" + 
                        lostTile.toString() + ") to remaining tiles");
                }
            } else {
                // add the lost tile to the bottom of the remaining tiles deck (not keeping it sorted)
                pgs.remainingTiles.addToBottom(lostTile);
                String highestTileString = pgs.remainingTiles.peek(highestTileIndex).toString();
                pgs.remainingTiles.remove(highestTileIndex);
                if(pgs.getCoreGameParameters().verbose) {
                    System.out.println("p" + pgs.getCurrentPlayer() + " loses turn and returns tile (" + 
                        lostTile.toString() + ") to remaining tiles. Tile " + highestTileString + " is removed.");
                }
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
