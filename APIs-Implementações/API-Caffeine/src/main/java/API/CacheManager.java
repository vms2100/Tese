package API;
import Tables.Author;
import Tables.Book;
import Tables.Client;
import Tables.Reservation;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CacheManager {

    public static final Cache<String, List<Author>> AuthorSearchCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(300)
            .build();


    public static final Cache<Integer, Author> AuthorCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public static final Cache<String, List<Book>> BookSearchCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(300)
            .build();


    public static final Cache<Integer, Book> BookCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public static final Cache<String, List<Reservation>> ReservationSearchCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(300)
            .build();


    public static final Cache<Integer, Reservation> ReservationCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public static final Cache<String,String> BookCacheS = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public static final Cache<Integer, Client> ClientCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public static final Cache<String, List<Client>> ClientSearchCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public static final Cache<String, String> StockSearch = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private CacheManager() {}
}
