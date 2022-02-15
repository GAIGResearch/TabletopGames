package games.stratego;

import games.stratego.components.Piece;

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
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.RED, new int[]{6,0}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{8,2}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{6,2}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{8,1}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{6,1}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{5,0}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{7,0}));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.RED, new int[]{3,1}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{0,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{3,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{5,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{8,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{1,2}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{4,2}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{1,0}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{9,0}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{7,3}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{0,0}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{2,0}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{3,0}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{8,0}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{1,1}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{7,1}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{9,1}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{4,0}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{2,3}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{7,2}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{9,2}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{5,1}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new int[]{4,3}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new int[]{9,3}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new int[]{5,2}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new int[]{0,1}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new int[]{2,2}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new int[]{2,1}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new int[]{4,1}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new int[]{1,3}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new int[]{3,2}));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.RED, new int[]{6,3}));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.RED, new int[]{0,2}));
            }};

            final ArrayList<Piece> bluePieces = new ArrayList<Piece>(){{
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.BLUE, new int[]{3,9}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{1,7}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{1,8}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{2,9}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{3,7}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{3,8}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{4,9}));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.BLUE, new int[]{6,8}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{0,9}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{1,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{4,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{5,7}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{6,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{8,7}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{8,9}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{9,6}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{1,9}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{2,6}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{6,9}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{7,9}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{9,9}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{0,8}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{2,8}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{5,9}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{8,8}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{0,7}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{2,7}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{4,8}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{7,6}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{0,6}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{4,7}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{5,6}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{9,8}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new int[]{5,8}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new int[]{7,7}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new int[]{7,8}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new int[]{6,7}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new int[]{8,6}));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.BLUE, new int[]{3,6}));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.BLUE, new int[]{9,7}));
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
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{0,0}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{1,0}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{2,0}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{3,0}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{4,0}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{5,0}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{6,0}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{7,0}));
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.RED, new int[]{8,0}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{9,0}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{0,1}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new int[]{1,1}));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.RED, new int[]{2,1}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new int[]{3,1}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{4,1}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{5,1}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new int[]{6,1}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{7,1}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{8,1}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{9,1}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{0,2}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{1,2}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new int[]{2,2}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new int[]{3,2}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{4,2}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{5,2}));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.RED, new int[]{6,2}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new int[]{7,2}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{8,2}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new int[]{9,2}));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.RED, new int[]{0,3}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new int[]{1,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{2,3}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{3,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{4,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{5,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{6,3}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{7,3}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{8,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{9,3}));
            }};

            final ArrayList<Piece> bluePieces = new ArrayList<Piece>(){{
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{9,9}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{8,9}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{7,9}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{6,9}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{5,9}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{4,9}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{3,9}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{2,9}));
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.BLUE, new int[]{1,9}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{0,9}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{9,8}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{8,8}));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.BLUE, new int[]{7,8}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new int[]{6,8}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{5,8}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{4,8}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{3,8}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{2,8}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{1,8}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{0,8}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{9,7}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{8,7}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new int[]{7,7}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new int[]{6,7}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{5,7}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{4,7}));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.BLUE, new int[]{3,7}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new int[]{2,7}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{1,7}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new int[]{0,7}));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.BLUE, new int[]{9,6}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{8,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{7,6}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{6,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{5,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{4,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{3,6}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{2,6}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{1,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{0,6}));
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
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{0,0}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{1,0}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{2,0}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{3,0}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{4,0}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{5,0}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{6,0}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{7,0}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{8,0}));
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.RED, new int[]{9,0}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{0,1}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{1,1}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{2,1}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{3,1}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new int[]{4,1}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.RED, new int[]{5,1}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{6,1}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.RED, new int[]{7,1}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{8,1}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{9,1}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new int[]{0,2}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{1,2}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.RED, new int[]{2,2}));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.RED, new int[]{3,2}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new int[]{4,2}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{5,2}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{6,2}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.RED, new int[]{7,2}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.RED, new int[]{8,2}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{9,2}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{0,3}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{1,3}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.RED, new int[]{2,3}));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.RED, new int[]{3,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{4,3}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{5,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{6,3}));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.RED, new int[]{7,3}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.RED, new int[]{8,3}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.RED, new int[]{9,3}));
            }};

            final ArrayList<Piece> bluePieces = new ArrayList<Piece>(){{
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{9,9}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{8,9}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{7,9}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{6,9}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{5,9}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{4,9}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{3,9}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{2,9}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{1,9}));
                add(new Piece(Piece.PieceType.FLAG, Piece.Alliance.BLUE, new int[]{0,9}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{9,8}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{8,8}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{7,8}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{6,8}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new int[]{5,8}));
                add(new Piece(Piece.PieceType.MINER, Piece.Alliance.BLUE, new int[]{4,8}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{3,8}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{2,8}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{1,8}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{0,8}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new int[]{9,7}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{8,7}));
                add(new Piece(Piece.PieceType.BOMB, Piece.Alliance.BLUE, new int[]{7,7}));
                add(new Piece(Piece.PieceType.SPY, Piece.Alliance.BLUE, new int[]{6,7}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new int[]{5,7}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{4,7}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{3,7}));
                add(new Piece(Piece.PieceType.MAJOR, Piece.Alliance.BLUE, new int[]{2,7}));
                add(new Piece(Piece.PieceType.COLONEL, Piece.Alliance.BLUE, new int[]{1,7}));
                add(new Piece(Piece.PieceType.SERGEANT, Piece.Alliance.BLUE, new int[]{0,7}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{9,6}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{8,6}));
                add(new Piece(Piece.PieceType.LIEUTENANT, Piece.Alliance.BLUE, new int[]{7,6}));
                add(new Piece(Piece.PieceType.GENERAL, Piece.Alliance.BLUE, new int[]{6,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{5,6}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{4,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{3,6}));
                add(new Piece(Piece.PieceType.MARSHAL, Piece.Alliance.BLUE, new int[]{2,6}));
                add(new Piece(Piece.PieceType.CAPTAIN, Piece.Alliance.BLUE, new int[]{1,6}));
                add(new Piece(Piece.PieceType.SCOUT, Piece.Alliance.BLUE, new int[]{0,6}));
            }};
        }
        ;

        protected abstract ArrayList<Piece> getRedSetup();
        protected abstract ArrayList<Piece> getBlueSetup();

    }
}
