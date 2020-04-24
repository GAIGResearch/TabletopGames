package updated_core.components;

import java.lang.reflect.Array;
import java.util.Arrays;

public class Grid<T> {

    private int width;
    private int height;

    private T[][] grid;

    @SuppressWarnings({"unchecked"})
    public Grid(int width, int height){
        this.width = width;
        this.height = height;
        this.grid = (T[][])Array.newInstance(getClass(), width, height);
    }

    @SuppressWarnings({"unchecked"})
    public Grid(int width, int height, T defaultValue){
        this(width, height);
        Arrays.fill(this.grid, defaultValue);
    }

    public Grid(T[][] grid){
        this.width = grid.length;
        this.height = grid[0].length;
        this.grid = grid;
    }

    public Grid(Grid<T> orig){
        this.width = orig.getWidth();
        this.height = orig.getHeight();
        this.grid = orig.grid.clone();
    }

    public int getWidth(){return width; }
    public int getHeight(){return height; }

    public boolean setElement(int x, int y, T value){
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid[x][y] = value;
            return true;
        }
        else
            return false;
    }

    public T getElement(int x, int y)
    {
        if (x >= 0 && x < width && y >= 0 && y < height)
            return grid[x][y];
        return null;
    }


}
