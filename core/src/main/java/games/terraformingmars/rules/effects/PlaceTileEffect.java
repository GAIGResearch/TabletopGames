package games.terraformingmars.rules.effects;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.PlaceTile;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMMapTile;

import java.util.Arrays;
import java.util.HashSet;

public class PlaceTileEffect extends Effect {
    public TMTypes.Tile tile;
    public TMTypes.Resource[] resourceTypeGained;
    public boolean onMars; // tile must've been placed on mars

    public PlaceTileEffect(boolean mustBeCurrentPlayer, TMAction effectAction, boolean onMars, TMTypes.Tile tile, TMTypes.Resource[] resourceGained) {
        super(mustBeCurrentPlayer, effectAction);
        this.onMars = onMars;
        this.tile = tile;
        this.resourceTypeGained = resourceGained;
    }

    @Override
    public boolean canExecute(TMGameState gameState, TMAction actionTaken, int player) {
        if (!(actionTaken instanceof PlaceTile) &&
                !(actionTaken instanceof PayForAction && (((PayForAction) actionTaken).action instanceof PlaceTile))
                || !super.canExecute(gameState, actionTaken, player)) return false;

        PlaceTile action;
        if (actionTaken instanceof PayForAction) action = (PlaceTile) ((PayForAction) actionTaken).action;
        else action = (PlaceTile) actionTaken;

        boolean marsCondition = !onMars || action.onMars;
        boolean tileCondition = tile == null || action.tile == tile;

        HashSet<TMTypes.Resource> gained = new HashSet<>();
        if (action.mapTileID != -1 && action.onMars) {
            TMMapTile mt = (TMMapTile) gameState.getComponentById(action.mapTileID);
            gained.addAll(Arrays.asList(mt.getResources()));
        }
        boolean resourceTypeCondition = resourceTypeGained == null;
        if (resourceTypeGained != null) {
            for (TMTypes.Resource r: resourceTypeGained) {
                if (gained.contains(r))  {
                    resourceTypeCondition = true;
                    break;
                }
            }
        }
        return marsCondition && tileCondition && resourceTypeCondition;
    }

    @Override
    public Effect copy() {
        PlaceTileEffect ef = new PlaceTileEffect(mustBeCurrentPlayer, effectAction.copy(), onMars, tile, resourceTypeGained);
        if (resourceTypeGained != null) {
            ef.resourceTypeGained = resourceTypeGained.clone();
        }
        return ef;
    }

    @Override
    public Effect copySerializable() {
        PlaceTileEffect ef = new PlaceTileEffect(mustBeCurrentPlayer, effectAction.copySerializable(), onMars, tile, resourceTypeGained);
        if (resourceTypeGained != null && resourceTypeGained.length > 0) {
            ef.resourceTypeGained = resourceTypeGained.clone();
        }
        return ef;
    }
}
