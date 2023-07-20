package utilities;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.function.Function;

public class JSONUtils {

    public static final JSONParser parser = new JSONParser();

    public static JSONObject loadJSONFile(String fileName) {
        try {
            FileReader reader = new FileReader(fileName);
            return (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException e) {
            throw new AssertionError("Error processing file " + fileName + " : " + e.getMessage() + " : " + e);
        }
    }

    public static String readJSONFile(String fileName, Function<String, String> preprocessor) {
        JSONObject json = loadJSONFile(fileName);
        if (preprocessor != null)
            return preprocessor.apply(json.toJSONString());
        return json.toJSONString();
    }

    /**
     * Given a JSONObject, this will load the instance of the class.
     * this assumes that the JSON object has:
     * - a "class" attribute with the full name of the Class
     * - an (optional) "args" Array attribute with the values to feed into the class constructor
     * - only int, double, boolean and string parameters are allowed
     * - the relevant constructor of the class is then called, and the result returned
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadClassFromJSON(JSONObject json) {
        try {
            String cl = (String) json.getOrDefault("class", "");
            if (cl.isEmpty()) {
                // look for an enum
                String en = (String) json.getOrDefault("enum", "");
                String val = (String) json.getOrDefault("value", "");
                if (en.isEmpty() || val.isEmpty())
                    throw new AssertionError("No class or enum/value tags found in " + json);
                Class<? extends Enum> enumClass = (Class<? extends Enum>) Class.forName(en);
                return (T) Enum.valueOf(enumClass, val);
            }
            Class<T> outputClass = (Class<T>) Class.forName(cl);
            JSONArray argArray = (JSONArray) json.getOrDefault("args", new JSONArray());
            Class<?>[] argClasses = new Class[argArray.size()];
            Object[] args = new Object[argArray.size()];
            for (int i = 0; i < argClasses.length; i++) {
                Object arg = argArray.get(i);
                if (arg instanceof JSONObject) {
                    // we have recursion
                    // we need to instantiate this, and then stick it in
                    arg = loadClassFromJSON((JSONObject) arg);
                    argClasses[i] = arg.getClass();
                } else if (arg instanceof Long) {
                    argClasses[i] = int.class;
                    args[i] = ((Long) arg).intValue();
                } else if (arg instanceof Double) {
                    argClasses[i] = double.class;
                } else if (arg instanceof Boolean) {
                    argClasses[i] = boolean.class;
                } else if (arg instanceof String) {
                    argClasses[i] = String.class;
                } else if (arg instanceof JSONArray) {
                    Object first = ((JSONArray) arg).get(0);
                    if (first instanceof JSONObject) {
                        Class<?> arrayClass = determineArrayClass((JSONArray) arg);

                        T[] arr = (T[]) Array.newInstance(arrayClass, ((JSONArray) arg).size());
                        argClasses[i] = arr.getClass();
                        for (int j = 0; j < ((JSONArray) arg).size(); j++) {
                            arr[j] = loadClassFromJSON((JSONObject) ((JSONArray) arg).get(j));
                        }
                        arg = arr;

                    } else if (first instanceof Long) {
                        argClasses[i] = int[].class;
                        args[i] = ((Long) first).intValue();
                        arg = ((JSONArray) arg).toArray(new Long[0]);
                    } else if (first instanceof Double) {
                        argClasses[i] = double[].class;
                        arg = ((JSONArray) arg).toArray(new Double[0]);
                    } else if (first instanceof Boolean) {
                        argClasses[i] = boolean[].class;
                        arg = ((JSONArray) arg).toArray(new Boolean[0]);
                    } else if (first instanceof String) {
                        argClasses[i] = String[].class;
                        arg = ((JSONArray) arg).toArray(new String[0]);
                        int a = 0;
                    }
                } else {
                    throw new AssertionError("Unexpected arg " + arg + " in " + json.toJSONString());
                }
                args[i] = arg;
            }

            Class<?> clazz = Class.forName(cl);
            Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(clazz, argClasses);
            if (constructor == null)
                throw new AssertionError("No matching Constructor found for " + clazz);
            //       System.out.println("Invoking constructor for " + clazz + " with " + Arrays.toString(args));
            Object retValue = constructor.newInstance(args);
            return outputClass.cast(retValue);

        } catch (ClassNotFoundException e) {
            throw new AssertionError("Unknown class in " + json.toJSONString() + " : " + e.getMessage());
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            throw new AssertionError("Error constructing class using " + json.toJSONString() + " : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new AssertionError("Unknown argument in " + json.toJSONString() + " : " + e.getMessage());
        }
    }

    public static Class<?> determineArrayClass(JSONArray array) {
        if (array.size() > 1) {
            JSONObject first = (JSONObject) array.get(0);
            String firstCl = (String) first.getOrDefault("class", "");
            for (int i = 1; i < array.size(); ++i) {
                JSONObject second = (JSONObject) array.get(i);
                String secondCl = (String) second.getOrDefault("class", "");
                if (firstCl.equalsIgnoreCase(secondCl)) continue;
                // We have an array of two different classes.
                // Either the array class is a common superclass or it's wrong.
                // We return the superclass of the first one.
                Object firstClass = loadClassFromJSON(first);
                return firstClass.getClass().getSuperclass();
            }
        }

        //Either one single class or multiple repetitions of the same class.
        JSONObject first = (JSONObject) array.get(0);
        return loadClassFromJSON(first).getClass();
    }

    /**
     * Given a filename that contains only a single class, this will instantiate the class
     * This opens the file, extracts the JSONObject, and then uses Utils.loadClassFromJSON() to
     * find and call the relevant constructor
     *
     * @param filename - the filename
     * @param <T>      - the Class type that is to be instantiated
     * @return
     */
    public static <T> T loadClassFromFile(String filename) {
        try {
            FileReader reader = new FileReader(filename);
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(reader);
            // We expect a class field to tell us the Class to use
            // then a set of parameter values
            return loadClassFromJSON(rawData);

        } catch (FileNotFoundException e) {
            throw new AssertionError("File not found to load : " + filename);
        } catch (IOException e) {
            throw new AssertionError("Problem reading file " + filename + " : " + e);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new AssertionError("Problem parsing JSON in " + filename);
        }
    }

    /**
     * Given a string that contains the JSON for a single class, this will instantiate the class
     *
     * @param rawData - the JSON as a raw string
     * @param <T>     - the Class type that is to be instantiated
     * @return
     */
    public static <T> T loadClassFromString(String rawData) {
        try {
            if (!rawData.contains("{")) {
                // we assume this is a class name with a no-arg constructor as a special case
                Class<?> clazz = Class.forName(rawData);
                Constructor<?> constructor = clazz.getConstructor();
                return (T) constructor.newInstance();
            }
            Reader reader = new StringReader(rawData);
            JSONParser jsonParser = new JSONParser();
            JSONObject json = (JSONObject) jsonParser.parse(reader);
            // We expect a class field to tell us the Class to use
            // then a set of parameter values
            return loadClassFromJSON(json);

        } catch (ParseException e) {
            e.printStackTrace();
            throw new AssertionError("Problem parsing JSON in " + rawData);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Problem processing String in " + rawData);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Problem processing String as classname with no-arg constructor : " + rawData);
        }
    }
}
