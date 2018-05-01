package de.veihelmann.closureplugin.utils;


import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * A simple map contains a list of values for each key. Only the methods actually necessary for this plugin are implemented.
 */
public class ListMap<K, V> {

    private final Map<K, List<V>> internalMap = new HashMap<>();

    public void put(K key, V value) {
        if (!internalMap.containsKey(key)) {
            internalMap.put(key, new ArrayList<>());
        }
        internalMap.get(key).add(value);
    }

    public Set<K> keys() {
        return internalMap.keySet();
    }

    public @NotNull
    List<V> getNullSafe(K key) {
        List<V> values = internalMap.get(key);
        if (values == null) {
            return new ArrayList<>();
        }
        return values;
    }

    public boolean containsKey(K key) {
        return internalMap.containsKey(key);
    }
}
