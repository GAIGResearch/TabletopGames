package core.observations;

public interface IPrintable {
    default void printToConsole(){
        System.out.println(toString());
    }
}
