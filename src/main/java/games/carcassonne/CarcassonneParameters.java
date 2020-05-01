package games.carcassonne;

import core.GameParameters;

import java.util.HashMap;

import static games.carcassonne.CarcassonneGameState.CarcassonneType;

public class CarcassonneParameters extends GameParameters {
    HashMap<CarcassonneType[][], Integer> tiles = new HashMap<>();
    public CarcassonneParameters(int nPlayers){
        super(nPlayers);

        // church
         tiles.put(new CarcassonneType[][]{
                 {CarcassonneType.Grass, CarcassonneType.Grass, CarcassonneType.Grass},
                 {CarcassonneType.Grass, CarcassonneType.Church, CarcassonneType.Grass},
                 {CarcassonneType.Grass, CarcassonneType.Grass, CarcassonneType.Grass},
         }, 4);

        // church with street
        tiles.put(new CarcassonneType[][]{
                {CarcassonneType.Grass, CarcassonneType.Grass, CarcassonneType.Grass},
                {CarcassonneType.Grass, CarcassonneType.Church, CarcassonneType.Grass},
                {CarcassonneType.Grass, CarcassonneType.Street, CarcassonneType.Grass},
        }, 2);

        // straight street
        tiles.put(new CarcassonneType[][]{
                {CarcassonneType.Grass, CarcassonneType.Grass, CarcassonneType.Grass},
                {CarcassonneType.Street, CarcassonneType.Street, CarcassonneType.Street},
                {CarcassonneType.Grass, CarcassonneType.Grass, CarcassonneType.Grass},
        }, 8);

        // street 90° turn
        tiles.put(new CarcassonneType[][]{
                {CarcassonneType.Grass, CarcassonneType.Grass, CarcassonneType.Grass},
                {CarcassonneType.Street, CarcassonneType.Street, CarcassonneType.Grass},
                {CarcassonneType.Grass, CarcassonneType.Street, CarcassonneType.Grass},
        }, 9);

        // street with side road
        tiles.put(new CarcassonneType[][]{
                {CarcassonneType.Grass, CarcassonneType.Grass, CarcassonneType.Grass},
                {CarcassonneType.Street, CarcassonneType.StreetEnd, CarcassonneType.Street},
                {CarcassonneType.Grass, CarcassonneType.Street, CarcassonneType.Grass},
        }, 4);

        // four-way street
        tiles.put(new CarcassonneType[][]{
                {CarcassonneType.Grass, CarcassonneType.Street, CarcassonneType.Grass},
                {CarcassonneType.Street, CarcassonneType.StreetEnd, CarcassonneType.Street},
                {CarcassonneType.Grass, CarcassonneType.Street, CarcassonneType.Grass},
        }, 1);

    }

}
