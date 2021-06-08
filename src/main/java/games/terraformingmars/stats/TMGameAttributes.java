package games.terraformingmars.stats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;

import java.util.function.BiFunction;

public enum TMGameAttributes implements IGameAttribute {
    GAME_ID((s, a) -> s.getGameID()),
    GENERATION((s, a) -> s.getGeneration()),
//    PLAYER((s, a) -> s.getCurrentPlayer()),
//    ACTION_TYPE((s, a) -> a == null ? "NONE" : a.getClass().getSimpleName()),
//    ACTION_DESCRIPTION((s, a) ->  a == null ? "NONE" : a.getString(s)),
    GP_OCEAN((s, a) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.OceanTiles)),
    GP_TEMPERATURE((s, a) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.Temperature)),
    GP_OXYGEN((s, a) -> s.getGlobalParameters().get(TMTypes.GlobalParameter.Oxygen)),
    N_CARDS_0((s, a) -> s.getPlayedCards()[0].getSize()),
    N_CARDS_1((s, a) -> s.getPlayedCards()[1].getSize()),
    MAP_COVERAGE((s,a) -> {
        int tilesPlaced = 0;
        int nTiles = 0;
        for (int i = 0; i < s.getBoard().getHeight(); i++) {
            for (int j = 0; j < s.getBoard().getWidth(); j++) {
                if (s.getBoard().getElement(j, i) != null) {
                    nTiles ++;
                    if (s.getBoard().getElement(j, i).getTilePlaced() != null) {
                        tilesPlaced++;
                    }
                }
            }
        }
        return tilesPlaced*1.0 / nTiles;
    });

    private final BiFunction<TMGameState, TMAction, Object> lambda;

    TMGameAttributes(BiFunction<TMGameState, TMAction, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(AbstractGameState state, AbstractAction action) {
        return lambda.apply((TMGameState) state, (TMAction) action);
    }

}
