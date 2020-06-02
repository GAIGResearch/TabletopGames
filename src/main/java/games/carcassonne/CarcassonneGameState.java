package games.carcassonne;

import core.actions.AbstractAction;
import core.components.Card;
import core.AbstractGameState;
import core.AbstractGameParameters;
import core.components.Component;
import core.observations.VectorObservation;
import core.turnorders.AlternatingTurnOrder;
import utilities.Utils;

import java.awt.*;
import java.util.*;
import java.util.List;


public class CarcassonneGameState extends AbstractGameState {

    public enum CarcassonneGamePhase {
        PlaceTile,
        PlaceCharacter
    }

    int[] points;
    int[] unusedMeeple;
    int numAvailableActions;

    CarcassonneBoard gameBoard;
    CarcassonneGamePhase gamePhase;

    public CarcassonneGameState(AbstractGameParameters gameParameters, int nPlayers) {
        super(gameParameters, new AlternatingTurnOrder(nPlayers));
    }

    private List<AbstractAction> tileActions(){

        return new ArrayList<>();
    }

    private List<AbstractAction> meepleActions(){

        return new ArrayList<>();
    }


    public String toString(){
        return this.gameBoard.toString();
    }


    @Override
    protected List<Component> _getAllComponents() {
        // TODO
        return null;
    }

    @Override
    protected AbstractGameState copy(int playerId) {
        // TODO
        return null;
    }

    @Override
    public VectorObservation getVectorObservation() {
        // TODO
        return null;
    }

    @Override
    public double[] getDistanceFeatures(int playerId) {
        // TODO
        return new double[0];
    }

    @Override
    public HashMap<HashMap<Integer, Double>, Utils.GameResult> getTerminalFeatures(int playerId) {
        // TODO
        return null;
    }

    @Override
    public double getScore(int playerId) {
        // TODO
        return 0;
    }

    static class CarcassonneBoard{
        HashMap<Point, CarcassonneTile> placedTiles = new HashMap<>();
        HashSet<Point> openPositions = new HashSet<>();

        CarcassonneBoard(){
            placedTiles.put(new Point(0,0), new CarcassonneTile(new CarcassonneType[][]{
                    {CarcassonneType.Grass, CarcassonneType.CastleGrass, CarcassonneType.Grass},
                    {CarcassonneType.Street, CarcassonneType.Street, CarcassonneType.Street},
                    {CarcassonneType.Grass, CarcassonneType.Grass, CarcassonneType.Grass},
            }));

            openPositions.add(new Point(0,1));
            openPositions.add(new Point(0,-1));
            openPositions.add(new Point(1,0));
            openPositions.add(new Point(-1,0));
        }

        public boolean placeTileAt(Point position, CarcassonneTile tile, int rotation){
            if (!placedTiles.containsKey(position))
            {
                tile.setRotation(rotation);
                tile.fixRotation();
                placedTiles.put(position, tile);
                return true;
            }
            else return false;
        }

        public boolean CanTileBePlacedAt(Point position, CarcassonneTile tile, int rotation){
            return false;
        }

        public void print(){
            int minX = 0, maxX = 0, minY = 0, maxY = 0;
            for (Point p : placedTiles.keySet()){
                if (p.x < minX)
                    minX = p.x;
                if (p.x > maxX)
                    maxX = p.x;
                if (p.y < minY)
                    minX = p.y;
                if (p.y < maxY)
                    maxY = p.y;
            }
            StringBuilder[] stringBuilders = new StringBuilder[maxY-minY];
            for (int i = 0; i < stringBuilders.length; i++)
                stringBuilders[i] = new StringBuilder((maxX-minX)*3);


            for (StringBuilder stringBuilder : stringBuilders)
                System.out.println(stringBuilder.toString());
        }
    }

    static class CarcassonneTile extends Card {
        private CarcassonneTile[] neighbors = new CarcassonneTile[4];
        private CarcassonneType[][] type; //3x3 array
        private int rotation = 0;
        private boolean fixedRotation = false;

        private CarcassonneTile(CarcassonneType[][] type){
            super();
            this.type = type;
        }

        private int getRotation(){
            return rotation;
        }

        private boolean setRotation(int rotation){
            if (!fixedRotation)
                this.rotation = rotation;
            return !fixedRotation;
        }

        private void fixRotation(){
            fixedRotation = true;
        }

        private boolean CanConnect(CarcassonneTile tile){
            return false;
        }

        /*
        public String[] toString(){
            return new String[]{Arrays.toString(type[0]),
                    Arrays.toString(type[1]),
                    Arrays.toString(type[2])};
        }*/
    }

    public enum CarcassonneType{
        Grass('g'),
        Castle('c'),
        Street('s'),
        CastleBonus('b'),
        CastleGrass('c'),
        Church('k'),
        StreetEnd('x');

        private Character tileChar;

        CarcassonneType(Character tileChar) {
            this.tileChar = tileChar;
        }

        public Character getTileChar() {
            return tileChar;
        }
    }
}
