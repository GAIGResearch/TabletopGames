package games.battlelore.gui;

import core.components.Component;
import core.components.GridBoard;
import core.components.Token;
import games.battlelore.components.Unit;
import gui.views.ComponentView;

import java.awt.*;

import static core.AbstractGUI.defaultItemSize;

public class BattleloreBoardView extends ComponentView
{
    public BattleloreBoardView(GridBoard<Unit> hexBoard)
    {
        super(hexBoard, hexBoard.getWidth() * defaultItemSize, hexBoard.getHeight() * defaultItemSize);
        
    }

    public void drawGridBoard(Graphics2D g, GridBoard<Unit> gridBoard, int x, int y)
    {
        int width = gridBoard.getWidth() * defaultItemSize;
        int height = gridBoard.getHeight() * defaultItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++)
        {
            for (int j = 0; j < gridBoard.getWidth(); j++)
            {
                int xC = x + j * defaultItemSize;
                int yC = y + i * defaultItemSize;
                drawCell(g, gridBoard.getElement(j, i), xC, yC);

            }
        }
    }
    @Override
    protected void paintComponent(Graphics g)
    {
        drawGridBoard((Graphics2D)g, (GridBoard<Unit>) component, 0, 0);
    }

    private void drawCell(Graphics2D g, Unit element, int x, int y) {
        // Paint cell background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, defaultItemSize, defaultItemSize);
        g.setColor(Color.black);
        g.drawRect(x, y, defaultItemSize, defaultItemSize);

        // Paint element in cell
        if (element != null)
        {
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, defaultItemSize * 3 / 2));
            g.drawString(element.toString(), x + defaultItemSize / 16, y + defaultItemSize - defaultItemSize / 16);
            g.setFont(f);
        }
    }
}
