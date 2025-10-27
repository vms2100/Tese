package API;

import Tables.Author;
import Tables.Book;
import Tables.Client;
import Tables.Reservation;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.*;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Expirations;
import org.ehcache.expiry.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class EhcacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("stockBookCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        String.class, String.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .offheap(25, MemoryUnit.MB)
                                )
                                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(10, TimeUnit.MINUTES)))
                )
                .withCache("BookCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        Integer.class, Book.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .offheap(150, MemoryUnit.MB)
                                )
                                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(15, TimeUnit.MINUTES)))
                )
                .withCache("AuthorCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        Integer.class, Author.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .offheap(100, MemoryUnit.MB)
                                )
                                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(10, TimeUnit.MINUTES)))
                )
                .withCache("ClientCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        Integer.class, Client.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .offheap(100, MemoryUnit.MB)
                                )
                                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(10, TimeUnit.MINUTES)))
                )
                .withCache("BookSearchCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        String.class, java.util.ArrayList.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .offheap(250, MemoryUnit.MB)
                                )
                                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(15, TimeUnit.MINUTES)))
                )
                .withCache("AuthorSearchCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        String.class,
                                        java.util.ArrayList.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .offheap(250, MemoryUnit.MB)
                                )
                                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(15, TimeUnit.MINUTES)))
                )
                .withCache("ClientSearchCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        String.class,
                                        java.util.ArrayList.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .offheap(250, MemoryUnit.MB)
                                )
                                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(15,  TimeUnit.MINUTES)))
                )
                .withCache("BookSCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        String.class,
                                        String.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .offheap(250, MemoryUnit.MB)
                                )
                                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(10, TimeUnit.MINUTES)))
                )
                .withCache("ReservationCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                Integer.class,
                                Reservation.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .offheap(25, MemoryUnit.MB)
                        )
                        .withExpiry(Expirations.timeToLiveExpiration(Duration.of(10, TimeUnit.MINUTES)))
                )
                .build(true);

        return cacheManager;
    }
}
