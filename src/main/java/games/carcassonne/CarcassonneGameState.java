package games.carcassonne;

import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import core.GameParameters;
import core.observations.Observation;

import java.awt.*;
import java.util.*;
import java.util.List;


public class CarcassonneGameState extends AbstractGameState {

    private int[] points;
    private int[] unusedMeeple;
    private int numAvailableActions;

    private CarcassonneBoard gameBoard;
    private CarcassonneGamePhase gamePhase;

    public CarcassonneGameState(GameParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        setComponents();
    }

    public void setComponents() {
        points = new int[getNPlayers()];
        unusedMeeple = new int[getNPlayers()];
        Arrays.fill(unusedMeeple, 7);
        gameBoard = new CarcassonneBoard();

        Deck<CarcassonneTile> drawPile = new Deck<>();

        //drawPile.add(new CarcassonneTile());
    }

    private List<IAction> tileActions(){

        return new ArrayList<>();
    }

    private List<IAction> meepleActions(){

        return new ArrayList<>();
    }


    public String toString(){
        return this.gameBoard.toString();
    }

    @Override
    public Observation getObservation(int player) {
        return null;
    }

    @Override
    public void endGame() {

    }

    @Override
    public List<IAction> computeAvailableActions(int player) {

        List<IAction> actions;
        switch (gamePhase){
            case PlaceTile:
                actions = tileActions();
                break;
            case PlaceCharacter:
                actions = meepleActions();
                break;
            default:
                actions = new ArrayList<>();
                break;
        }

        this.numAvailableActions = actions.size();
        return actions;
    }

    private class CarcassonneBoard{
        HashMap<Point, CarcassonneTile> placedTiles = new HashMap<>();
        HashSet<Point> openPositions = new HashSet<>();

        private CarcassonneBoard(){
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

    private class CarcassonneTile extends Card {
        private CarcassonneTile[] neighbors = new CarcassonneTile[4];
        private CarcassonneType[][] type; //3x3 array
        private int rotation = 0;
        private boolean fixedRotation = false;

        private CarcassonneTile(CarcassonneType[][] type){
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
