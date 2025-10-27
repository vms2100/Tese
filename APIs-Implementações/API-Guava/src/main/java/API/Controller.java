package API;

import Repository.*;
import Tables.*;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.google.common.cache.Cache;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.*;





@RestController
@RequestMapping("/api")
public class Controller {
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final ClientRepository clientRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final LibraryRepository libraryRepository;
    private final ReservationRepository reservationRepository;
    private final StockRepository stockRepository;
    private final Parser parser;

    public Controller(BookRepository bookRepository, AuthorRepository authorRepository,
                      BookAuthorRepository bookAuthorRepository, LibraryRepository libraryRepository,
                      ReservationRepository reservationRepository, ClientRepository clientRepository,StockRepository stockRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.bookAuthorRepository = bookAuthorRepository;
        this.libraryRepository = libraryRepository;
        this.reservationRepository = reservationRepository;
        this.clientRepository = clientRepository;
        this.stockRepository = stockRepository;
        this.parser = new Parser();
    }

    // =============== Utility Methods ===============
    private boolean allNull(Object... objs) {
        for (Object o : objs) if (o != null) return false;
        return true;
    }

    private ResponseEntity<byte[]> badRequest(String format, boolean download, String message) {
        return returnResponse(format, download, message.getBytes(StandardCharsets.UTF_8), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<byte[]> emptyResponse(String format, boolean download) {
        return returnResponse(format, download, "Empty Response".getBytes(StandardCharsets.UTF_8), HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<byte[]> returnResponse(String format, Boolean download, byte[] responseBody, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        String mediaType = format.equalsIgnoreCase("csv") ? "text/plain" : "application/json";
        headers.setContentType(MediaType.parseMediaType(mediaType));

        ContentDisposition disposition = download ?
                ContentDisposition.attachment().filename("response." + format).build() :
                ContentDisposition.inline().filename("response." + format).build();

        headers.setContentDisposition(disposition);
        headers.setContentLength(responseBody.length);
        return new ResponseEntity<>(responseBody, headers, status);
    }

    private ResponseEntity<byte[]> returnResponse(String format, Boolean download, byte[] responseBody) {
        return returnResponse(format, download, responseBody, HttpStatus.OK);
    }

    CacheManager manager = new CacheManager();
    Cache<String, List<Author>> searchAuthorCache = manager.createCache("searchAuthorCache", 100, 10);
    Cache<String, List<Client>> searchClientCache = manager.createCache("searchClientCache", 100, 10);
    Cache<String,List<Book>> searchBookCache = manager.createCache("searchBookCache", 100, 10);

    Cache<Integer,Book> BookCache = manager.createCache("BookCache", 100, 10);
    Cache<String,String> BookCacheS = manager.createCache("BookCacheS", 100, 10);

    Cache<Integer, Author> AuthorCache = manager.createCache("AuthorCache", 100, 10);
    Cache<Integer,Client> ClientCache= manager.createCache("ClientCache", 100, 10);
    Cache<Integer,Reservation> ReservationCache = manager.createCache("ReservationCache", 100, 10);
    Cache<String,String> searchStockCache = manager.createCache("searchStockCache", 100, 10);



    // =============== Simple Endpoints ===============

    @GetMapping("/Authors/{format}/")
    public ResponseEntity<byte[]> getAuthors(@PathVariable String format, @RequestParam(required = false) Integer ID_Authors, @RequestParam(required = false) String Name, @RequestParam(required = false) Integer age, @RequestParam(required = false) String Bibliography, @RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Authors/{format}/";
        String params = String.format("ID_Authors=%s,Name=%s,Age=%d,Bibliograhy=%s", ID_Authors,Name,age,Bibliography);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime=0;

        if (allNull(ID_Authors, Name, age, Bibliography)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        String Key = cacheKeyAuhtors(ID_Authors,Name,age,Bibliography);

        List <Author> authors =searchAuthorCache.getIfPresent(Key);
        if (authors == null) {
            long startDb1 = System.nanoTime();
            authors = authorRepository.getAuthorsParamether(ID_Authors, Name, age, Bibliography);
            dbTime+= duration(startDb1);
            searchAuthorCache.put(Key, authors);
        }

        if (authors.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        long parseTime=System.nanoTime();
        String result = parser.parseAuthors(format, authors);
        parseTime=duration(parseTime);

        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Library/{format}/")
    public ResponseEntity<byte[]> getLibraries(@PathVariable String format, @RequestParam(required = false) Integer ID_Library, @RequestParam(required = false) String Name, @RequestParam(required = false) String Description, @RequestParam(required = false) String Location, @RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {

        String endpoint = "/Library/{format}/";
        String params = String.format("ID_Library=%s,Name=%s,Description=%s,Location=%s", ID_Library,Name,Description,Location);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(ID_Library, Name, Description, Location)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }
        long dbTime = System.nanoTime();
        List<Library> libraries = libraryRepository.getLibrariesParamether(ID_Library, Name, Description, Location);
        dbTime = duration(dbTime);
        if (libraries.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        long parseTime=System.nanoTime();
        String result = parser.parseLibrarys(format, libraries);
        parseTime=duration(parseTime);
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Clients/{format}/")
    public ResponseEntity<byte[]> getClients(@PathVariable String format, @RequestParam(required = false) Integer ID_Client, @RequestParam(required = false) String Name, @RequestParam(required = false) String Phone, @RequestParam(required = false) String Email, @RequestParam(required = false) String Adress, @RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Clients/{format}/";
        String params = String.format("ID_Client=%s,Name=%s,Phone=%s,Email=%s, Adress=%s", ID_Client, Name,Phone,Email,Adress);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime= 0;

        if (allNull(ID_Client, Name, Phone, Email, Adress)) {
            makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 400, 0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        String Key = cacheKeyClient(ID_Client, Name,Phone,Email,Adress);

        List<Client> clients = searchClientCache.getIfPresent(Key);
        if (clients == null) {
            long startDb1 = System.nanoTime();
            clients = clientRepository.getClientsParamether(ID_Client, Name, Phone, Email, Adress);
            dbTime += duration(startDb1);
            searchClientCache.put(Key, clients);
        }

        if (clients.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        long parseTime=System.nanoTime();
        String result = parser.parseClients(format, clients);
        parseTime=duration(parseTime);

        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Reservations/{format}/")
    public ResponseEntity<byte[]> getReservations(@PathVariable String format, @RequestParam(required = false) Integer ID_Reservations, @RequestParam(required = false) Integer ID_Library, @RequestParam(required = false) Integer ID_Book, @RequestParam(required = false) Integer ID_Clients, @RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Reservations/{format}/";
        String params = String.format("ID_Reservations=%s,ID_Library=%s,ID_Book=%s,ID_Clients=%s", ID_Reservations,ID_Library,ID_Book,ID_Clients);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(ID_Reservations, ID_Library, ID_Book, ID_Clients)) {
            makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 400, 0);
            return badRequest(format, download, "Provide at least one parameter");
        }
        long dbTime = System.nanoTime();
        List<Reservation> reservations = reservationRepository.getReservationParamether(ID_Reservations, ID_Library, ID_Book, ID_Clients);
        dbTime = duration(dbTime);

        if (reservations.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        long parseTime=System.nanoTime();
        String result = parser.parseReservations(format, reservations);
        parseTime=duration(parseTime);
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }



    // =============== Combined Endpoints =============== //

    // =============== Books ====================//

    @GetMapping("/Books/{format}/")
    public ResponseEntity<byte[]> getBooks(@PathVariable String format, @RequestParam(required = false) Integer ID_Book, @RequestParam(required = false) String ISBN, @RequestParam(required = false) String Title, @RequestParam(required = false) String Sinopse, @RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Books/{format}/";
        String params = String.format("ID_Book=%s,ISBN=%s,Title=%s,Sinopse=%s", ID_Book, ISBN, Title, Sinopse);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime = 0;

        if (allNull(ID_Book, ISBN, Title, Sinopse)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        String Key = cacheKeyBook(ID_Book, ISBN, Title, Sinopse);
        List <Book> books = searchBookCache.getIfPresent(Key);
        if (books == null) {
            long startDb1 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(ID_Book, ISBN, Title, Sinopse);
            dbTime += duration(startDb1);
            searchBookCache.put(Key, books);
        }

        if (books.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder result= new StringBuilder();
        long parseTime=0;
        for (Book book : books) {
            String key = format + book.getid();
            String s = BookCacheS.getIfPresent(key);
            if (s == null) {
                BookCache.asMap().putIfAbsent(book.getid(), book);

                long startDb2 = System.nanoTime();
                List<Author> authors = authorRepository.getAuthorsParamether(bookAuthorRepository.getAuthorIDByBook(book.getid()), null, null, null);
                dbTime += duration(startDb2);

                List <Book> books2 = new ArrayList<>();
                books2.add(book);
                String resultparser= "";
                long startParse = System.nanoTime();
                if (authors.isEmpty()) {
                    resultparser = parser.parseBooks(format, books2, "", 0);
                } else {
                    resultparser = parser.parseBooks(format, books2, authors.getFirst().getName(), authors.getFirst().getId());
                }
                parseTime += duration(startParse);
                result.append(resultparser);
            } else {
                result.append(s);
            }
        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.toString().length());
        return returnResponse(format, download, result.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Books/{format}/AuthorName/{Author_Name}/")
    public ResponseEntity<byte[]> getBooksByAuthorName(@PathVariable String format,@PathVariable("Author_Name") String Author_Name,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Books/AuthorName/";
        String params = String.format("Author_Name=%s",Author_Name);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime =0;

        if (allNull(Author_Name)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        String Key = "Name="+Author_Name;
        List <Author> authors = searchAuthorCache.getIfPresent(Key);

        if (authors == null) {
            authors = authorRepository.getAuthorsParamether(null,Author_Name,null,null);
            dbTime = duration(dbTime);
            searchAuthorCache.put(Key, authors);
        }

        if (authors.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder response= new StringBuilder();

        if (format.equalsIgnoreCase("CSV")){
            response.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }

        long parseTime=0;
        for (Author author : authors){
            AuthorCache.asMap().putIfAbsent(author.getId(),author);

            long dbTime2=System.nanoTime();
            List<Integer> bookIDs = bookAuthorRepository.getBooksIDbyAuthor(author.getId());
            dbTime+=duration(dbTime2);

            List<Book> books= new ArrayList<>();
            for (Integer bookID : bookIDs){
                Book book = BookCache.getIfPresent(bookID);
                if (book != null) {
                    books.add(book);
                    bookIDs.remove(bookID);
                }
            }

            dbTime2=System.nanoTime();
            books.addAll(bookRepository.getBooksByIDS(bookIDs));
            dbTime+=duration(dbTime2);

            long parseTime2=System.nanoTime();
            for (Book book : books){
                String key = format + book.getid();
                String s =BookCacheS.getIfPresent(key);
                if (s == null){
                    s =parser.parseBooksV2(format,book,author);
                    BookCacheS.put(key,s);
                }
                response.append(s);
            }
            parseTime+=duration(parseTime2);
        }

        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,response.toString().length());
        return returnResponse(format, download, response.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Books/{format}/AuthorID/{Author_ID}/")
    public ResponseEntity<byte[]> getBooksByAuthorID(@PathVariable String format,@PathVariable("Author_ID") Integer Author_ID,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Books/AuthorID/";
        String params = String.format("AuthorID=%s",Author_ID);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime =0;

        if (allNull(Author_ID)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        String Key = "ID_Authors="+Author_ID;
        List<Author> authors = searchAuthorCache.getIfPresent(Key);

        if (authors == null) {
            long dbTime2 = System.nanoTime();
            authors = authorRepository.getAuthorsParamether(Author_ID,null,null,null);
            dbTime += duration(dbTime2);
            searchAuthorCache.put(Key, authors);
        }

        if (authors.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder response= new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            response.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }

        long parseTime=0;
        for (Author author : authors){
            AuthorCache.asMap().putIfAbsent(author.getId(),author);

            long dbTime2=System.nanoTime();
            List<Integer> booksIds=bookAuthorRepository.getBooksIDbyAuthor(author.getId());

            List<Book> books= new ArrayList<>();
            for (Integer bookID : booksIds){
                Book book = BookCache.getIfPresent(bookID);
                if (book != null) {
                    books.add(book);
                    booksIds.remove(bookID);
                }
            }
            books.addAll(bookRepository.getBooksByIDS(booksIds));

            dbTime+=duration(dbTime2);

            long parseTime2=System.nanoTime();
            for (Book book : books){
                String key = format + book.getid();
                String s =BookCacheS.getIfPresent(key);
                if (s == null){
                    s =parser.parseBooksV2(format,book,author);
                    BookCacheS.put(key,s);
                }
                response.append(s);
            }
            parseTime+=duration(parseTime2);
        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,response.toString().length());
        return returnResponse(format, download, response.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Books/{format}/BookName/{Book_Name}/")
    public ResponseEntity<byte[]> getAuthorsByBookName(@PathVariable String format,@PathVariable("Book_Name") String Book_Name,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Authors/Book_Name/";
        String params = String.format("Book_Name=%s",Book_Name);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime =0;
        String Key = "Title="+Book_Name;


        List<Book> books = searchBookCache.getIfPresent(Key);
        if (books==null) {
            long dbTime2 = System.nanoTime();
            books =bookRepository.getBooksbyParamether(null,Book_Name,null,null);
            dbTime += duration(dbTime2);
        }

        if (books.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder res= new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            res.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }
        long parseTime=0;
        for (Book book : books) {
            BookCache.asMap().putIfAbsent(book.getid(), book);
            String key = format + book.getid();
            String s = BookCacheS.getIfPresent(key);
            if (s == null) {
                long dbTime2 = System.nanoTime();
                Integer authorID = bookAuthorRepository.getAuthorIDByBook(book.getid());
                List<Author> authors = authorRepository.getAuthorsParamether(authorID, null, null, null);
                Author author = null;
                if (!authors.isEmpty()) {
                    author = authors.getFirst();
                }
                dbTime += duration(dbTime2);
                long parseTime2 = System.nanoTime();
                String s2 = parser.parseBooksV2(format, book, author);
                res.append(s2);
                BookCacheS.put(key, s2);
                parseTime += duration(parseTime2);
            } else {
                res.append(s);
            }
        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,res.toString().length());
        return returnResponse(format, download, res.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Books/{format}/BookID/{Book_ID}/")
    public ResponseEntity<byte[]> getAuthorsByBookID(@PathVariable String format,@PathVariable("Book_ID") Integer Book_ID,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Authors/Book_ID/";
        String params = String.format("Book_ID=%s",Book_ID);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        String key = "ID_Book="+Book_ID;
        long dbTime = 0;
        List<Book> books =searchBookCache.getIfPresent(key);
        if (books==null) {
            long dbTime2 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(Book_ID,null,null,null);
            dbTime += duration(dbTime2);
            searchBookCache.put(key,books);
        }
        if (books.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder res= new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            res.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }
        long parseTime=0;
        for (Book book : books){
            String Key = format + book.getid();
            BookCache.asMap().putIfAbsent(book.getid(), book);
            String s =BookCacheS.getIfPresent(Key);
            if (s == null){
                long dbTime2=System.nanoTime();
                Integer authorID= bookAuthorRepository.getAuthorIDByBook(book.getid());
                List<Author> authors = authorRepository.getAuthorsParamether(authorID,null,null,null);
                Author author=null;
                if (!authors.isEmpty()) {
                    author=authors.getFirst();
                }
                dbTime+=duration(dbTime2);
                long parseTime2=System.nanoTime();
                String s2 =parser.parseBooksV2(format, book,author);
                res.append(s2);
                parseTime+=duration(parseTime2);
                BookCacheS.put(Key,s2);
            }else{
                res.append(s);
            }

        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,res.toString().length());
        return returnResponse(format, download, res.toString().getBytes(StandardCharsets.UTF_8));
    }


    // ============= Reservations ======== //
    @GetMapping("/Reservations/{format}/ID_Reservation/{ID_Reservation}/Info")
    public ResponseEntity<?> getReservationInfoByReservationID(@PathVariable String format, @PathVariable Integer ID_Reservation) throws JsonProcessingException {
        String endpoint = "/Reservations/ID_Reservation/";
        String params = String.format("ID_Reservation=%s",ID_Reservation);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        List<Reservation> reservations = new ArrayList<>();
        long dbTime = 0;

        if (allNull(ID_Reservation)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        Reservation reservation = ReservationCache.getIfPresent(ID_Reservation);

        if (reservation==null) {
            long dbTime2 = System.nanoTime();
            reservations = reservationRepository.getReservationParamether(ID_Reservation, null, null, null);
            dbTime+= duration(dbTime2);
            if (reservations.isEmpty()) {
                makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
                Reservation r1 = new Reservation();
                ReservationCache.put(ID_Reservation,r1);
                return  emptyResponse(format,false);
            }
            reservation= reservations.getFirst();
        }

        List<Library> libraries = libraryRepository.getLibrariesParamether(reservation.getIdLibrary(), null, null, null);
        List<Client> clients = new ArrayList<>();
        List<Book> books = new ArrayList<>();
        List<Author> authors = new ArrayList<>();
        Client client = ClientCache.getIfPresent(reservation.getIdClients());
        Book book = BookCache.getIfPresent(reservation.getIdBook());

        if (client == null){
            long dbtime2=System.nanoTime();
            clients = clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
            dbTime += duration(dbtime2);
            ClientCache.put(reservation.getIdClients(),clients.getFirst());
        }else{
            clients.add(client);
        }

        if (book == null){
            long dbtime2=System.nanoTime();
            books = bookRepository.getBooksbyParamether(reservation.getIdBook(), null, null, null);
            dbTime += duration(dbtime2);
            BookCache.put(reservation.getIdClients(),books.getFirst());
        }else{
            books.add(book);
        }

        long dbtime2=System.nanoTime();
        Integer authorId = bookAuthorRepository.getAuthorIDByBook(books.getFirst().getid());
        dbTime += duration(dbtime2);

        Author author = AuthorCache.getIfPresent(authorId);
        if (author == null){
            dbtime2=System.nanoTime();
            List<Integer> authorIds = Collections.singletonList(authorId);
            authors = authorRepository.getAuthorsByIDs(authorIds);
            if (!authors.isEmpty()) {
                AuthorCache.put(authorId, authors.getFirst());
            }
            dbTime += duration(dbtime2);
        }else{
            authors.add(author);
        }

        long parseTime=System.nanoTime();
        String res = parser.parseReservationsV2(format,reservation,libraries,clients,books,authors);
        parseTime=duration(parseTime);

        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,res.length());
        return returnResponse(format,false,res.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Reservations/{format}/client/{ClientName}/Info")
    public ResponseEntity<?> getReservationInfoByClientName(@PathVariable String format, @PathVariable String ClientName) throws JsonProcessingException {
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime =0;
        String endpoint = "/Reservations/ClientName/";
        String params = String.format("ClientName=%s",ClientName);

        if (allNull(ClientName)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        String Key = "Name=" + ClientName;
        List<Client> clients= searchClientCache.getIfPresent(Key);
        if (clients==null) {
            long dbTime2 = System.nanoTime();
            clients = clientRepository.getClientsParamether(null,ClientName, null, null, null);
            dbTime += duration(dbTime2);
            searchClientCache.put(Key,clients);
        }

        if (clients.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format,false);
        }


        List<Reservation> reservations= new ArrayList<>();
        Set<Reservation> reservationSet = new HashSet<>(reservations);
        long dbtime2=System.nanoTime();
        for (Client client : clients) {
            ClientCache.asMap().putIfAbsent(client.getId(),client);
            List<Reservation> r = reservationRepository.getReservationParamether(null, null, null, client.getId());
            for (Reservation rs : r) {
                if (reservationSet.add(rs)) {
                    reservations.add(rs);
                }
            }
        }
        dbTime += duration(dbtime2);

        if (reservations.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return  emptyResponse(format,false);
        }

        StringBuilder response= new StringBuilder();
        long parseTime = 0;
        if (format.equals("json")) {
            response.append("{");
        }
        for (Reservation reservation : reservations) {
            long dbtime3=System.nanoTime();
            List<Library> libraries = libraryRepository.getLibrariesParamether(reservation.getIdLibrary(), null, null, null);
            List<Book> books = new ArrayList<>();
            dbTime += duration(dbtime3);
            List<Client> client = new ArrayList<>();
            Client client1 = ClientCache.getIfPresent(reservation.getIdClients());
            Book book = BookCache.getIfPresent(reservation.getIdBook());
            if (book == null){
                dbtime3=System.nanoTime();
                books = bookRepository.getBooksbyParamether(reservation.getIdBook(), null, null, null);
                dbTime += duration(dbtime3);
            }else{
                books.add(book);
            }

            if (client1 == null){
                dbtime3=System.nanoTime();
                clients= clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
                ClientCache.put(reservation.getIdClients(),clients.getFirst());
                dbTime += duration(dbtime3);
            }


            Integer authorId = null;
            if (!books.isEmpty()) {
                authorId = bookAuthorRepository.getAuthorIDByBook(books.getFirst().getid());
            }

            List<Author> authors = (authorId != null)
                    ? authorRepository.getAuthorsByIDs(Collections.singletonList(authorId))
                    : Collections.emptyList();

            dbTime += duration(dbtime3);

            long parseTime2=System.nanoTime();
            response.append(parser.parseReservationsV2(format,reservation,libraries,client,books,authors)+"\n");
            parseTime+=duration(parseTime2);
        }
        if (format.equals("json")) {
            response.append("}");
        }

        String result=response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,response.toString().length());
        return returnResponse(format,false,response.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Reservations/{format}/Library/{LibraryName}/Info")
    public ResponseEntity<?> getReservationInfoByLibrary(@PathVariable String format, @PathVariable String LibraryName) throws JsonProcessingException {
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        String endpoint = "/Reservations/LibraryName/";
        String params = String.format("LlibraryName=%s",LibraryName);

        if (allNull(LibraryName)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        long dbTime = System.nanoTime();
        List<Library> libraries = libraryRepository.getLibrariesParamether(null, LibraryName, null, null);
        dbTime= duration(dbTime);
        if (libraries.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return  emptyResponse(format,false);
        }

        List<Reservation> reservations= new ArrayList<>();
        Set<Reservation> reservationSet = new HashSet<>(reservations);
        long dbtime2=System.nanoTime();
        for (Library library: libraries){
            List<Reservation> r = reservationRepository.getReservationParamether(null,library.getId(), null, null);
            for (Reservation rs : r) {
                if (reservationSet.add(rs)) {
                    reservations.add(rs);
                }
            }
        }

        dbTime += duration(dbtime2);
        if (reservations.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return  emptyResponse(format,false);
        }


        StringBuilder response= new StringBuilder();
        long parseTime = 0;
        for (Reservation reservation : reservations) {

            List<Book> books = new ArrayList<>();
            Book book = BookCache.getIfPresent(reservation.getIdBook());
            if (book == null){
                long dbtime3=System.nanoTime();
                books = bookRepository.getBooksbyParamether(reservation.getIdBook(), null, null, null);
                dbTime += duration(dbtime3);
            }else{
                books.add(book);
            }
            long dbtime3=System.nanoTime();
            Integer authorId = null;
            if (!books.isEmpty()) {
                authorId = bookAuthorRepository.getAuthorIDByBook(books.getFirst().getid());
            }

            List<Author> authors = (authorId != null)
                    ? authorRepository.getAuthorsByIDs(Collections.singletonList(authorId))
                    : Collections.emptyList();

            dbTime += duration(dbtime3);

            List<Client> client = new ArrayList<>();
            Client client1 = ClientCache.getIfPresent(reservation.getIdClients());
            if (client1 == null){
                dbtime3=System.nanoTime();
                client=clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
                dbTime += duration(dbtime3);
                ClientCache.put(client.getFirst().getId(), client.getFirst());
            }else{
                client.add(client1);
            }

            long parseTime2=System.nanoTime();
            response.append(parser.parseReservationsV2(format,reservation,libraries,client,books,authors)+"\n");
            parseTime+=duration(parseTime2);
        }


        String result=response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format,false,result.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Reservations/{format}/Book/{BookTitle}/Info")
    public ResponseEntity<?> getReservationInfoByBook(@PathVariable String format, @PathVariable String BookTitle) throws JsonProcessingException {
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        String endpoint = "/Reservations/BookTitle/";
        String params = String.format("BookTitle=%s",BookTitle);
        long dbTime= 0;
        if (allNull(BookTitle)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        String Key = "Title="+BookTitle;
        List<Book> books = searchBookCache.getIfPresent(Key);
        if (books == null){
            long dbTime2 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(null,null,BookTitle,null);
            dbTime+=duration(dbTime2);
            searchBookCache.put(Key,books);
        }

        if (books.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return  emptyResponse(format,false);
        }

        List<Reservation> reservations= new ArrayList<>();
        Set<Reservation> reservationSet = new HashSet<>(reservations);
        long dbTime2=System.nanoTime();
        for (Book book: books){
            List<Reservation> r = reservationRepository.getReservationParamether(null,null, book.getid(), null);
            for (Reservation rs : r) {
                if (reservationSet.add(rs)) {
                    reservations.add(rs);
                }
            }
        }
        dbTime+= duration(dbTime2);
        if (reservations.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return  emptyResponse(format,false);
        }

        StringBuilder response= new StringBuilder();
        long parseTime=0;
        for (Reservation reservation : reservations) {
            long dbtime3=System.nanoTime();
            List<Library> libraries = libraryRepository.getLibrariesParamether(reservation.getIdLibrary(),null,null,null);
            Integer authorId = null;
            if (!books.isEmpty()) {
                authorId = bookAuthorRepository.getAuthorIDByBook(books.getFirst().getid());
            }
            List<Author> authors = (authorId != null)
                    ? authorRepository.getAuthorsByIDs(Collections.singletonList(authorId))
                    : Collections.emptyList();

            dbTime += duration(dbtime3);

            List<Client> client= new ArrayList<>();
            Client client1 = ClientCache.getIfPresent(reservation.getIdClients());
            if (client1 == null){
                long dbtime4=System.nanoTime();
                client=clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
                dbTime+=duration(dbtime4);
            }else {
                client.add(client1);
            }

            long parseTime2=System.nanoTime();
            response.append(parser.parseReservationsV2(format,reservation,libraries,client,books,authors)+"\n");
            parseTime+=duration(parseTime2);
        }

        String result=response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format,false,result.getBytes(StandardCharsets.UTF_8));
    }


    // ======== Stocks ======== //
    @GetMapping("/Stocks/{format}/Book/{BookTitle}")
    public ResponseEntity<?> getStockInfoByBook(@PathVariable String format, @PathVariable String BookTitle) throws JsonProcessingException {
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        String endpoint = "/Stocks/BookTitle/";
        String params = String.format("BookTitle=%s",BookTitle);
        long dbTime=0;

        if (allNull(BookTitle)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        StringBuilder response= new StringBuilder();
        if (format.equalsIgnoreCase("csv")) {
            response.append("ID_Book,ISBN,bookTitle,Library_ID,Library_Name,Stock\n");
        }

        String key = "Title="+BookTitle;
        List<Book> books = searchBookCache.getIfPresent(key);
        if (books == null){
            long dbTime2 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(null,null,BookTitle,null);
            dbTime+=duration(dbTime2);
            searchBookCache.put(key,books);
        }

        if (books.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return  emptyResponse(format,false);
        }

        long parseTime = 0;
        for (Book book : books) {
            long dbTime2 = System.nanoTime();
            List<Stock> stocks = stockRepository.getStockByParamether(book.getid(), null);
            dbTime += duration(dbTime2);

            for (Stock stock : stocks) {
                dbTime2 = System.nanoTime();
                List<Library> libraries = libraryRepository.getLibrariesParamether(stock.getID_Library(), null, null, null);
                dbTime += duration(dbTime2);

                for (Library lib : libraries) {
                    long parseTime2 = System.nanoTime();
                    response.append(Parser.parseStockV2(format, book, stock, lib));
                    parseTime += duration(parseTime2);
                }
            }
        }

        String result=response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format,false,result.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Stocks/{format}/Library/{Library}")
    public ResponseEntity<?> getStockInfoByLibrary(@PathVariable String format, @PathVariable String Library) throws JsonProcessingException {
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        String endpoint = "/Stocks/Library";
        String params = String.format("LibraryName=%s",Library);
        String res="";
        if (allNull(Library, format)) {
            makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 400, 0);
            return badRequest(format, false, "Bad Request");
        }

        String cacheKey = Library + "_" + format;
        res = searchStockCache.getIfPresent(cacheKey);
        if (res != null) {
            if (res.isEmpty()) {
                makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 204, 0);
                return emptyResponse(format, false);
            }

            makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 200, res.length());
            return returnResponse(format, false, res.getBytes(StandardCharsets.UTF_8));
        }

        if (format.equalsIgnoreCase("csv")) {
            res="ID_Book,ISBN,bookTitle,Library_ID,Library_Name,Stock\n";
        }

        long dbTime = System.nanoTime();
        List<Library> libraries = libraryRepository.getLibrariesParamether(null,Library,null,null);
        dbTime=duration(dbTime);
        if (libraries.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return  emptyResponse(format,false);
        }

        long parseTime=0;

        for (Library library: libraries) {
            long dbTime2=System.nanoTime();
            List<Stock> Stocks= stockRepository.getStockByParamether(null,library.getId());
            dbTime += duration(dbTime2);
            for (Stock s: Stocks) {
                dbTime2=System.nanoTime();
                List<Book> books= bookRepository.getBooksbyParamether(s.getID_Book(),null,null,null);
                dbTime += duration(dbTime2);
                for (Book book: books) {
                    long parseTime2=System.nanoTime();
                    res += Parser.parseStockV2(format,book,s,library);
                    parseTime+=duration(parseTime2);
                }
            }
        }
        if (res ==null){
            res="";
        }
        searchStockCache.put(cacheKey,res);
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,res.length());
        return returnResponse(format,false,res.getBytes(StandardCharsets.UTF_8));
    }




    private long duration(long startNano) {
        return System.nanoTime() - startNano;
    }

    private long memoryUsed(long startMem) {
        long endMem = memoryBean.getHeapMemoryUsage().getUsed();
        long diff = endMem - startMem;
        return Math.max(0, diff);
    }


    private long cpuUsed(long startCpu) {
        long endCpu = threadBean.getCurrentThreadCpuTime();
        return Math.max(0, endCpu - startCpu);
    }

    public void makeLog(String Endpoint, String params,long startTime,long startMem,long startCpu,long dbTime,long parserTime,int statusCode, int resSize ){
        long memoryUsed = memoryUsed(startMem);
        long cpuUsed = cpuUsed(startCpu);
        Metrics.log(Endpoint, params, dbTime, parserTime, duration(startTime), resSize, statusCode, memoryUsed, cpuUsed);
    }

    private String cacheKeyAuhtors(Integer ID_Authors,String Name,Integer age, String Bibliography){
        StringBuilder sb = new StringBuilder();

        if (ID_Authors != null) {
            sb.append("ID_Authors=").append(ID_Authors);
        }
        if (Name != null) {
            sb.append("Name=").append(Name);
        }
        if (age != null) {
            sb.append("Age=").append(age);
        }
        if (Bibliography != null) {
            sb.append("Bibliography=").append(Bibliography);
        }
        return sb.toString().trim();
    }
    private String cacheKeyBook(Integer ID_Book,String ISBN,String Title,String Sinopse){
        StringBuilder sb = new StringBuilder();

        if (ID_Book != null) {
            sb.append("ID_Book=").append(ID_Book);
        }
        if (ISBN != null) {
            sb.append("ISBN=").append(ISBN);
        }
        if (Title != null) {
            sb.append("Title=").append(Title);
        }
        if (Sinopse != null) {
            sb.append("Sinopse=").append(Sinopse);
        }
        return sb.toString().trim();
    }
    private String cacheKeyClient(Integer ID_Client,String Name, String Phone, String Email, String Adress){
        StringBuilder sb = new StringBuilder();

        if (ID_Client != null) {
            sb.append("ID_Client=").append(ID_Client);
        }
        if (Name != null) {
            sb.append("Name=").append(Name);
        }
        if (Phone != null) {
            sb.append("Phone=").append(Phone);
        }
        if (Email != null) {
            sb.append("Email=").append(Email);
        }
        if (Adress != null) {
            sb.append("Adress=").append(Adress);
        }
        return sb.toString().trim();
    }
}
