package games.catan.gui;

import games.catan.CatanBoard;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class CatanBoardView extends JComponent {
    // todo calculate the optimal radius for the hexagons
    CatanGameState gs;
    private int width;
    private int height;

    public CatanBoardView(CatanGameState gs){
        this.gs = gs;
        this.height = 600;
        this.width = 600;
        setPreferredSize(new Dimension(width, height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // todo set background colour to blue so it looks like sea, line below doesn't work
//        g2.setBackground(Color.BLUE);
        super.paintComponent(g);
        drawBoard(g2);
    }

    private void drawBoard(Graphics2D g) {

        CatanTile[][] board = gs.getBoard();
        for (int x = 0; x < board.length; x++){
            for (int y = 0; y < board[x].length; y++){
                g.setColor(tileColourLookup(board[x][y]));
                g.fillPolygon(board[x][y].getHexagon());
                g.setColor(Color.BLACK);
                g.drawPolygon(board[x][y].getHexagon());
            }
        }
    }

    private Color tileColourLookup(CatanTile tile){
        if (tile.getType() == CatanParameters.TileType.DESERT){
            return Color.YELLOW;
        } else if (tile.getType() == CatanParameters.TileType.SEA){
            return Color.BLUE;
        }else if (tile.getType() == CatanParameters.TileType.FIELDS){
            return Color.LIGHT_GRAY;
        }else if (tile.getType() == CatanParameters.TileType.FOREST){
            return Color.RED;
        }else if (tile.getType() == CatanParameters.TileType.MOUNTAINS){
            return Color.DARK_GRAY;
        }else if (tile.getType() == CatanParameters.TileType.PASTURE){
            return Color.GREEN;
        } else if (tile.getType() == CatanParameters.TileType.HILLS){
            return Color.BLACK;
        } else{
            return Color.WHITE;
        }
    }
}
