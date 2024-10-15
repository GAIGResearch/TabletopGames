package games.conquest;

import core.actions.AbstractAction;
import games.conquest.actions.*;
import games.conquest.components.Cell;
import games.conquest.components.Command;
import games.conquest.components.Troop;
import utilities.Vector2D;

import java.util.*;

public class CQUtility {
    /**
     * Compares highlighted cell and/or command with the expected highlights for a given action.
     * @param action The action for which to check the highlights required
     * @param highlight The highlighted cell
     * @param cmdHighlight The highlighted command
     * @return `true` if highlight(s) correspond to action's highlight(s); `false` otherwise
     */
    public static boolean compareHighlight(AbstractAction action, Vector2D highlight, Command cmdHighlight) {
        if (action instanceof EndTurn) return true;
        assert action instanceof CQAction; // Only EndTurn is a basic AbstractAction; the rest extend CQAction
        if (action instanceof ApplyCommand) return ((CQAction) action).checkHighlight(highlight, cmdHighlight);
        else return ((CQAction) action).checkHighlight(highlight);
    }

    public static double getRelativeCost(HashSet<Troop> troops) {
        double cost = 0;
        for (Troop troop : troops) {
            cost += troop.getTroopType().cost * troop.getUnboostedHealth() / (double) troop.getTroopType().health;
        }
        return cost;
    }
    public static int getTotalCost(HashSet<Troop> troops) {
        int cost = 0;
        for (Troop troop : troops) {
            cost += troop.getTroopType().cost;
        }
        return cost;
    }

    /**
     * Calculate the distance between two points, given some game state.
     * @param cqgs The current game state; used to access troop locations and cell matrix
     * @param from The cell from which to calculate
     * @param to The cell to which to calculate
     * @return Distance between the two cells; returns 9999 (~inf) when unreachable
     */
    public static int astar(CQGameState cqgs, Cell from, Cell to) {
        if (from.equals(to)) return 0; // only cell with a troop we can walk on is to our own cell.
        if (!to.isWalkable(cqgs))
            return 9999; // if target is not walkable, it's not reachable; avoid calculations
        Cell[][] board = cqgs.cells;
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(node -> node.f));
        Map<Cell, Integer> gScores = new HashMap<>();
        Set<Cell> closedSet = new HashSet<>();

        Node startNode = new Node(from, 0, from.getChebyshev(to));
        openSet.add(startNode);
        gScores.put(from, 0);
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            Cell currentCell = current.cell;
            if (currentCell.equals(to)) // If we reach the destination, return the distance
                return current.g;
            closedSet.add(currentCell);

            for (Cell neighbor : currentCell.getNeighbors(board)) {
                if (closedSet.contains(neighbor) || !neighbor.isWalkable(cqgs)) {
                    continue;
                }
                int heuristicG = current.g + 1; // Cost from start to neighbor
                if (heuristicG < gScores.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    gScores.put(neighbor, heuristicG);
                    int fScore = heuristicG + neighbor.getChebyshev(to);
                    openSet.add(new Node(neighbor, heuristicG, fScore));
                }
            }
        }
        // If no path was found
        return 9999;
    }
    private static class Node {
        Cell cell;
        int g;
        int f;
        Node(Cell cell, int g, int f) {
            this.cell = cell;
            this.g = g;
            this.f = f;
        }
    }

    /**
     * Flood fill to generate distances to the whole board. (currently only used in GUI)
     * Used in case where all distances need to be known, due to being cheaper than performing A* on all cells.
     * @param cqgs Current game state, including troop positions
     * @param source Source cell from which to calculate distances
     * @return 2d integer array containing distances to the target square, or 9999 if unreachable.
     */
    public static int[][] floodFill (CQGameState cqgs, Cell source) {
        int w = cqgs.cells.length, h = cqgs.cells[0].length;
        List<Cell> openSet = source.getNeighbors(cqgs.cells); // initial set of neighbors
        Set<Cell> closedSet = new HashSet<>();
        int[][] board = new int[w][h];
        for (int[] row : board) // fill all with 'unreachable' initially
            Arrays.fill(row, 9999);
        board[source.position.getX()][source.position.getY()] = 0;
        int distance = 0;
        while (!openSet.isEmpty()) {
            distance++; // first iteration is distance 1, etc
            for (Cell c : openSet) {
                // First go through all cells currently listed and add their distance
                if (!c.isWalkable(cqgs)) // can't visit; set value to infty
                    board[c.position.getX()][c.position.getY()] = 9999;
                else // can visit; set
                    board[c.position.getX()][c.position.getY()] = distance;
                closedSet.add(c);
            }
            Set<Cell> newSet = new HashSet<>();
            for (Cell c : openSet) {
                // After going through all neighbors once, replace them by their neighbor set
                if (c.isWalkable(cqgs)) // ignore if this cell wasn't reachable anyway
                    newSet.addAll(c.getNeighbors(cqgs.cells));
            }
            openSet.clear(); // all items have been checked for their neighbors, so make place
            openSet.addAll(newSet); // copy new set of all neighbours to open set
            openSet.removeAll(closedSet); // remove cells that have been visited before
        }
        return board;
    }
}
