package core.interfaces;

public interface IPrintable {
    default void printToConsole(){
        System.out.println(toString());
    }
}
