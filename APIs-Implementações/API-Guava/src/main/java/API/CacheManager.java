package API;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CacheManager {

    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    public <K, V> Cache<K, V> createCache(String name, long maxSize, long expireMinutes) {
        Cache<K, V> cache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
        caches.put(name, cache);
        return cache;
    }

    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.get(name);
    }

}
