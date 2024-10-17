package games.stratego;

import games.stratego.components.Piece;
import utilities.Vector2D;

import java.util.ArrayList;

public class StrategoConstants {

    public static final ArrayList<Piece.Alliance> playerMapping = new ArrayList<Piece.Alliance>(){{
       add(Piece.Alliance.RED);
       add(Piece.Alliance.BLUE);
    }};

    public static final String waterCell = "X";

    public enum PieceSetups{
        Setup1 {
            @Override
            protected ArrayList<Piece> getRedSetup() {
                return new ArrayList<>(redPieces);
            }

            @Override
            protected ArrayList<Piece> getBlueSetup() {
                return new ArrayList<>(bluePieces);
            }

            final ArrayList<Piece> redPieces = new ArrayList<Piece>(){{
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.RED, new Vector2D(6,0)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(8,2)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(6,2)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(8,1)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(6,1)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(5,0)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(7,0)));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.RED, new Vector2D(3,1)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(0,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(3,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(5,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(8,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(1,2)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(4,2)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(1,0)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(9,0)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(7,3)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(0,0)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(2,0)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(3,0)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(8,0)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(1,1)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(7,1)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(9,1)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(4,0)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(2,3)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(7,2)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(9,2)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(5,1)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new Vector2D(4,3)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new Vector2D(9,3)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new Vector2D(5,2)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new Vector2D(0,1)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new Vector2D(2,2)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new Vector2D(2,1)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new Vector2D(4,1)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new Vector2D(1,3)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new Vector2D(3,2)));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.RED, new Vector2D(6,3)));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.RED, new Vector2D(0,2)));
            }};

            final ArrayList<Piece> bluePieces = new ArrayList<Piece>(){{
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.BLUE, new Vector2D(3,9)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(1,7)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(1,8)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(2,9)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(3,7)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(3,8)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(4,9)));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.BLUE, new Vector2D(6,8)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(0,9)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(1,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(4,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(5,7)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(6,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(8,7)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(8,9)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(9,6)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(1,9)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(2,6)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(6,9)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(7,9)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(9,9)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(0,8)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(2,8)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(5,9)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(8,8)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(0,7)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(2,7)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(4,8)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(7,6)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(0,6)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(4,7)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(5,6)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(9,8)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new Vector2D(5,8)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new Vector2D(7,7)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new Vector2D(7,8)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new Vector2D(6,7)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new Vector2D(8,6)));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.BLUE, new Vector2D(3,6)));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.BLUE, new Vector2D(9,7)));
            }};
        },
        Setup2 {
            @Override
            protected ArrayList<Piece> getRedSetup() {
                return new ArrayList<>(redPieces);
            }

            @Override
            protected ArrayList<Piece> getBlueSetup() {
                return new ArrayList<>(bluePieces);
            }

            final ArrayList<Piece> redPieces = new ArrayList<Piece>(){{
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(0,0)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(1,0)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(2,0)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(3,0)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(4,0)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(5,0)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(6,0)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(7,0)));
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.RED, new Vector2D(8,0)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(9,0)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(0,1)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new Vector2D(1,1)));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.RED, new Vector2D(2,1)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new Vector2D(3,1)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(4,1)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(5,1)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new Vector2D(6,1)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(7,1)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(8,1)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(9,1)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(0,2)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(1,2)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new Vector2D(2,2)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new Vector2D(3,2)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(4,2)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(5,2)));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.RED, new Vector2D(6,2)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new Vector2D(7,2)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(8,2)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new Vector2D(9,2)));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.RED, new Vector2D(0,3)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new Vector2D(1,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(2,3)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(3,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(4,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(5,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(6,3)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(7,3)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(8,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(9,3)));
            }};

            final ArrayList<Piece> bluePieces = new ArrayList<Piece>(){{
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(9,9)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(8,9)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(7,9)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(6,9)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(5,9)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(4,9)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(3,9)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(2,9)));
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.BLUE, new Vector2D(1,9)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(0,9)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(9,8)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(8,8)));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.BLUE, new Vector2D(7,8)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new Vector2D(6,8)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(5,8)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(4,8)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(3,8)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(2,8)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(1,8)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(0,8)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(9,7)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(8,7)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new Vector2D(7,7)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new Vector2D(6,7)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(5,7)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(4,7)));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.BLUE, new Vector2D(3,7)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new Vector2D(2,7)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(1,7)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new Vector2D(0,7)));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.BLUE, new Vector2D(9,6)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(8,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(7,6)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(6,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(5,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(4,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(3,6)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(2,6)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(1,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(0,6)));
            }};
        },
        Setup3 {
            @Override
            protected ArrayList<Piece> getRedSetup() {
                return new ArrayList<>(redPieces);
            }

            @Override
            protected ArrayList<Piece> getBlueSetup() {
                return new ArrayList<>(bluePieces);
            }

            final ArrayList<Piece> redPieces = new ArrayList<Piece>(){{
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(0,0)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(1,0)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(2,0)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(3,0)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(4,0)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(5,0)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(6,0)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(7,0)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(8,0)));
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.RED, new Vector2D(9,0)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(0,1)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(1,1)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(2,1)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(3,1)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new Vector2D(4,1)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new Vector2D(5,1)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(6,1)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new Vector2D(7,1)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(8,1)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(9,1)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new Vector2D(0,2)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(1,2)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new Vector2D(2,2)));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.RED, new Vector2D(3,2)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new Vector2D(4,2)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(5,2)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(6,2)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new Vector2D(7,2)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new Vector2D(8,2)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(9,2)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(0,3)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(1,3)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new Vector2D(2,3)));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.RED, new Vector2D(3,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(4,3)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(5,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(6,3)));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.RED, new Vector2D(7,3)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new Vector2D(8,3)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new Vector2D(9,3)));
            }};

            final ArrayList<Piece> bluePieces = new ArrayList<Piece>(){{
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(9,9)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(8,9)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(7,9)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(6,9)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(5,9)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(4,9)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(3,9)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(2,9)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(1,9)));
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.BLUE, new Vector2D(0,9)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(9,8)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(8,8)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(7,8)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(6,8)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new Vector2D(5,8)));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new Vector2D(4,8)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(3,8)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(2,8)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(1,8)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(0,8)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new Vector2D(9,7)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(8,7)));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new Vector2D(7,7)));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.BLUE, new Vector2D(6,7)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new Vector2D(5,7)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(4,7)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(3,7)));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new Vector2D(2,7)));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new Vector2D(1,7)));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new Vector2D(0,7)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(9,6)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(8,6)));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new Vector2D(7,6)));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.BLUE, new Vector2D(6,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(5,6)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(4,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(3,6)));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.BLUE, new Vector2D(2,6)));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new Vector2D(1,6)));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new Vector2D(0,6)));
            }};
        }
        ;

        protected abstract ArrayList<Piece> getRedSetup();
        protected abstract ArrayList<Piece> getBlueSetup();

    }
}
