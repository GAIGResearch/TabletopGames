package games.catan;

import core.components.GraphBoard;

public class CatanBoard extends GraphBoard {
    // CatanBoard stores everything on the board
    // tiles, cards, resources
    CatanTile[][] board;
    int[] robberLocation = new int[2];

    public CatanBoard(CatanParameters params){
        board = generateBoard(params);
    }

    public CatanTile[][] generateBoard(CatanParameters params){
        // todo steps:
        // 2, distribute all the resource tiles with number tokens
        // 3, distribute sea tiles
        board = new CatanTile[params.n_tiles_per_row][params.n_tiles_per_row];
        for (int x = 0; x < board.length; x++){
            for (int y = 0; y < board[x].length; y++){
                board[x][y] = new CatanTile(x, y);
            }
        }

//        ArrayList<CatanParameters.TileType> tileList = new ArrayList<>();
//        for (Map.Entry tileCount : params.tileCounts.entrySet()){
//            // todo create component ids
//            for (int i = 0; i < (int)tileCount.getValue(); i++) {
//                tileList.add((CatanParameters.TileType)tileCount.getKey());
//            }
//        }
//        Collections.shuffle(tileList);
//
//        int count = 0;
//        for (int row = 1; row < 6; row++) {
//            switch (row) {
//                case 1:
//                    for (int col = 1; col < 4; col++) {
//                        board[col][row] = new CatanBoardNode(tileList.get(count));
//                        board[col][row].setCoords(col, row);
//                        count++;
//                    }
//                    break;
//                case 2:
//                    for (int col = 1; col < 5; col++) {
//                        board[col][row] = new CatanBoardNode(tileList.get(count));
//                        board[col][row].setCoords(col, row);
//                        count++;
//                    }
//                    break;
//                case 3:
//                    for (int col = 1; col < 6; col++) {
//                        board[col][row] = new CatanBoardNode(tileList.get(count));
//                        board[col][row].setCoords(col, row);
//                        count++;
//                    }
//                    break;
//                case 4:
//                    for (int col = 2; col < 6; col++) {
//                        board[col][row] = new CatanBoardNode(tileList.get(count));
//                        board[col][row].setCoords(col, row);
//                        count++;
//                    }
//                    break;
//                case 5:
//                    for (int col = 3; col < 6; col++) {
//                        board[col][row] = new CatanBoardNode(tileList.get(count));
//                        board[col][row].setCoords(col, row);
//                        count++;
//                    }
//                    break;
//            }
//
////            robberLoc = desert.getLocation();
//            System.out.println("");
////            robberLocation[0] = row;
////            robberLocation[1] = col;
//        }
        return board;



    }

    public CatanBoard copy(){
        // TODO implement copy method
        return this;
    }
}
