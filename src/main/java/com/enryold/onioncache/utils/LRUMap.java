package com.enryold.onioncache.utils;

import java.util.LinkedHashMap;
import java.util.Map;


public class LRUMap<K, V> extends LinkedHashMap<K, V> {
    private int capacity;

    public LRUMap(int capacity) {
        super(10, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
