package games.catan;

import core.components.BoardNode;
import core.components.GraphBoard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class CatanBoardNode{
    int x, y;
    int[] edges;
    int[] vertices;
    CatanParameters.TileType tileType;
    int number;

    public CatanBoardNode(CatanParameters.TileType tileType){
        this.tileType = tileType;
        this.number = number;
    }

    public void setCoords(int x, int y){
        this.x = x;
        this.y = y;
    }

    public void setNumber(int number){
        this.number = number;
    }

    public boolean addRoad(int edge){
        if (this.edges[edge] == 1) return false;
        this.edges[edge] = 1;
        return true;
    }

    public boolean addSettlement(int vertex){
        if (this.vertices[vertex] >= 1) return false;
        this.vertices[vertex] = 1;
        return true;
    }

    public boolean addCity(int vertex){
        if (this.vertices[vertex] != 1) return false;
        this.vertices[vertex] = 2;
        return true;
    }

}

public class CatanBoard extends GraphBoard {
    // represent board using cube coordinates
    // more info on https://www.redblobgames.com/grids/hexagons/
    CatanBoardNode[][] board;
    int[] robberLocation = new int[2];

    public CatanBoard(CatanParameters params){
        generateBoard(params);
    }


    public void generateBoard(CatanParameters params){
        // todo get 7 from parameters
        // todo steps:
        // 2, distribute all the resource tiles with number tokens
        // 3, distribute sea tiles
        board = new CatanBoardNode[7][7];

        ArrayList<CatanParameters.TileType> tileList = new ArrayList<>();
        for (Map.Entry tileCount : params.tileCounts.entrySet()){
            // todo create component ids
            for (int i = 0; i < (int)tileCount.getValue(); i++) {
                tileList.add((CatanParameters.TileType)tileCount.getKey());
            }
        }
        Collections.shuffle(tileList);

        int count = 0;
        for (int row = 1; row < 6; row++) {
            switch (row) {
                case 1:
                    for (int col = 1; col < 4; col++) {
                        board[col][row] = new CatanBoardNode(tileList.get(count));
                        board[col][row].setCoords(col, row);
                        count++;
                    }
                    break;
                case 2:
                    for (int col = 1; col < 5; col++) {
                        board[col][row] = new CatanBoardNode(tileList.get(count));
                        board[col][row].setCoords(col, row);
                        count++;
                    }
                    break;
                case 3:
                    for (int col = 1; col < 6; col++) {
                        board[col][row] = new CatanBoardNode(tileList.get(count));
                        board[col][row].setCoords(col, row);
                        count++;
                    }
                    break;
                case 4:
                    for (int col = 2; col < 6; col++) {
                        board[col][row] = new CatanBoardNode(tileList.get(count));
                        board[col][row].setCoords(col, row);
                        count++;
                    }
                    break;
                case 5:
                    for (int col = 3; col < 6; col++) {
                        board[col][row] = new CatanBoardNode(tileList.get(count));
                        board[col][row].setCoords(col, row);
                        count++;
                    }
                    break;
            }

//            robberLoc = desert.getLocation();
            System.out.println("");
//            robberLocation[0] = row;
//            robberLocation[1] = col;
        }



    }
}
