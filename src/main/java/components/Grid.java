package components;

import java.lang.reflect.Array;
import java.util.Arrays;

public class Grid<T> {

    private int width;
    private int height;

    private T[][] grid;
    private final Class typeParameterClass;

    @SuppressWarnings({"unchecked"})
    private Grid(int width, int height, Class typeParameterClass){
        this.width = width;
        this.height = height;
        this.typeParameterClass = typeParameterClass;
        this.grid = (T[][])Array.newInstance(typeParameterClass, width, height);
    }

    @SuppressWarnings({"unchecked"})
    public Grid(int width, int height, T defaultValue){
        this(width, height, defaultValue.getClass());
        for (int x = 0; x < width; x++)
            Arrays.fill(grid[x], defaultValue);
    }

    public Grid(T[][] grid, Class<T> typeParameterClass){
        this.width = grid.length;
        this.height = grid[0].length;
        this.grid = grid;
        this.typeParameterClass = typeParameterClass;
    }

    public Grid(Grid<T> orig){
        this.width = orig.getWidth();
        this.height = orig.getHeight();
        this.grid = orig.grid.clone();
        this.typeParameterClass = orig.typeParameterClass;
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

    public T[][] getGridValues(){
        return grid;
    }

}
