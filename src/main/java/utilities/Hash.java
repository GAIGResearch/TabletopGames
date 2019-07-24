package utilities;

import java.util.HashMap;

public class Hash
{
    private static Hash hash;

    private HashMap<String, Integer> hashmap;

    public static Hash GetInstance()
    {
        if(hash == null)
            hash = new Hash();
        return hash;
    }

    private Hash()
    {
        hashmap = new HashMap<>();
    }


    public int hash(String key)
    {
        if(hashmap.containsKey(key))
            return hashmap.get(key);

        int newValue = key.hashCode();
        hashmap.put(key, newValue);
        return newValue;
    }

}
