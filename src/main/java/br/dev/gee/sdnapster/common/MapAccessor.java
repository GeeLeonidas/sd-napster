package br.dev.gee.sdnapster.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MapAccessor<K, V> {
    private final MapGetter<K, V> getter;
    private final MapPutter<K, V> putter;
    
    public MapAccessor(MapGetter<K, V> getter, MapPutter<K, V> putter) {
        this.getter = getter;
        this.putter = putter;
    }

    @Nullable
    public V get(@Nonnull K key) {
        try {
            return getter.get(key);
        } catch (Exception exception) {
            return null;
        }
    }

    public void put(@Nonnull K key, @Nonnull V value) {
        putter.put(key, value);
    }

    interface MapGetter<K, V> {
        @Nullable 
        public V get(@Nonnull K key);
    }

    interface MapPutter<K, V> {
        public void put(@Nonnull K key, @Nonnull V value);
    }
}
