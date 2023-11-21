package core;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

public class MethodReferenceTest {

    public static void main(String[] args){

        Random rnd = new Random();

        // firstly we create a list of 10000 random number
        List<Number> numbers = IntStream.generate(rnd::nextInt).limit(10000).boxed().collect(toList());

        MethodReferenceTest test = new MethodReferenceTest();

        long start = System.nanoTime();
        test.useUglyJava(numbers);
        long methodRef = System.nanoTime();
        test.useMethodReference(numbers);
        long lambda = System.nanoTime();
        test.useLambda(numbers);
        long java = System.nanoTime();
        System.out.printf("%d; %d; %d\n", (methodRef - start)/1000, (lambda - methodRef) /1000, (java - lambda)/1000);

        numbers = IntStream.generate(rnd::nextInt).limit(10000).boxed().collect(toList());

        start = System.nanoTime();
        test.useUglyJava(numbers);
        methodRef = System.nanoTime();
        test.useMethodReference(numbers);
        lambda = System.nanoTime();
        test.useLambda(numbers);
        java = System.nanoTime();

        System.out.printf("%d; %d; %d\n", (methodRef - start)/1000, (lambda - methodRef) /1000, (java - lambda)/1000);

        numbers = IntStream.generate(rnd::nextInt).limit(10000).boxed().collect(toList());

        start = System.nanoTime();
        test.useUglyJava(numbers);
        methodRef = System.nanoTime();
        test.useMethodReference(numbers);
        lambda = System.nanoTime();
        test.useLambda(numbers);
        java = System.nanoTime();

        System.out.printf("%d; %d; %d\n", (methodRef - start)/1000, (lambda - methodRef) /1000, (java - lambda)/1000);

        numbers = IntStream.generate(rnd::nextInt).limit(10000).boxed().collect(toList());

        start = System.nanoTime();
        test.useUglyJava(numbers);
        methodRef = System.nanoTime();
        test.useMethodReference(numbers);
        lambda = System.nanoTime();
        test.useLambda(numbers);
        java = System.nanoTime();

        System.out.printf("%d; %d; %d\n", (methodRef - start)/1000, (lambda - methodRef) /1000, (java - lambda)/1000);

        numbers = IntStream.generate(rnd::nextInt).limit(10000).boxed().collect(toList());

        start = System.nanoTime();
        test.useUglyJava(numbers);
        methodRef = System.nanoTime();
        test.useMethodReference(numbers);
        lambda = System.nanoTime();
        test.useLambda(numbers);
        java = System.nanoTime();

        System.out.printf("%d; %d; %d\n", (methodRef - start)/1000, (lambda - methodRef) /1000, (java - lambda)/1000);

        numbers = IntStream.generate(rnd::nextInt).limit(10000).boxed().collect(toList());

        start = System.nanoTime();
        test.useUglyJava(numbers);
        methodRef = System.nanoTime();
        test.useMethodReference(numbers);
        lambda = System.nanoTime();
        test.useLambda(numbers);
        java = System.nanoTime();

        System.out.printf("%d; %d; %d\n", (methodRef - start)/1000, (lambda - methodRef) /1000, (java - lambda)/1000);

        numbers = IntStream.generate(rnd::nextInt).limit(10000).boxed().collect(toList());

        start = System.nanoTime();
        test.useUglyJava(numbers);
        methodRef = System.nanoTime();
        test.useMethodReference(numbers);
        lambda = System.nanoTime();
        test.useLambda(numbers);
        java = System.nanoTime();

        System.out.printf("%d; %d; %d\n", (methodRef - start)/1000, (lambda - methodRef) /1000, (java - lambda)/1000);

        numbers = IntStream.generate(rnd::nextInt).limit(10000).boxed().collect(toList());

        start = System.nanoTime();
        test.useUglyJava(numbers);
        methodRef = System.nanoTime();
        test.useMethodReference(numbers);
        lambda = System.nanoTime();
        test.useLambda(numbers);
        java = System.nanoTime();

        System.out.printf("%d; %d; %d\n", (methodRef - start)/1000, (lambda - methodRef) /1000, (java - lambda)/1000);
    }

    double useMethodReference(List<Number> input) {
        return input.stream().mapToDouble(Number::doubleValue).sum();
    }

    double useLambda(List<Number> input) {
        return input.stream().mapToDouble(i -> i.doubleValue()).sum();
    }

    double useUglyJava(List<Number> input) {
        double retValue = 0.0;
        for (Number n : input) {
            retValue += n.doubleValue();
        }
        return retValue;
    }
}
