package API;

import Repository.*;
import Tables.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.ehcache.CacheManager;
import org.ehcache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
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


    // =============== Simple Endpoints ===============
    @GetMapping("/Authors/{format}/")
    public ResponseEntity<byte[]> getAuthors(@PathVariable String format,
                                             @RequestParam(required = false) Integer ID_Authors,
                                             @RequestParam(required = false) String Name,
                                             @RequestParam(required = false) Integer age,
                                             @RequestParam(required = false) String Bibliography,
                                             @RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Authors/{format}/";
        String params = String.format("ID_Authors=%s,Name=%s,Age=%d,Bibliograhy=%s", ID_Authors,Name,age,Bibliography);
        String Key= cacheKeyAuhtors(ID_Authors,Name,age,Bibliography);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime= 0;

        if (allNull(ID_Authors, Name, age, Bibliography)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        List<Author> authors = new ArrayList<>();
        Cache<String,ArrayList<Author>> searchAuthor= getsearchAuthorCache();
        Cache<Integer,Author> authorCache= getAuthorCache();

        if (searchAuthor.get(Key)!= null){
            if (searchAuthor.get(Key).isEmpty()){
                makeLog(endpoint,params,startTime,startMem,startCpu,0,0,204,0);
                return emptyResponse(format, download);
            }else{
                authors.addAll(searchAuthor.get(Key));
            }
        }

        if (authors.isEmpty()) {
            long startDb1 = System.nanoTime();
            authors = authorRepository.getAuthorsParamether(ID_Authors, Name, age, Bibliography);
            dbTime= duration(startDb1);
            if (authors.isEmpty()){
                searchAuthor.put(Key,new ArrayList<>());
                makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
                return emptyResponse(format, download);
            }
            searchAuthor.put(Key,new ArrayList<>(authors));
        }

        StringBuilder result = new StringBuilder();
        if (format.equalsIgnoreCase("csv")){
            result.append("ID Author,Name,Age,Bibliography").append(System.lineSeparator());
        }
        long parseTime=0;
        for (Author author : authors) {
            authorCache.putIfAbsent(author.getId(), author);
            long parseTime2=System.nanoTime();
            result.append(parser.parseAuthor(format,author));
            parseTime+=duration(parseTime2);
        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.toString().length());
        return returnResponse(format, download, result.toString().getBytes(StandardCharsets.UTF_8));
    }



    @GetMapping("/Library/{format}/")
    public ResponseEntity<byte[]> getLibraries(@PathVariable String format,
                                               @RequestParam(required = false) Integer ID_Library,
                                               @RequestParam(required = false) String Name,
                                               @RequestParam(required = false) String Description,
                                               @RequestParam(required = false) String Location,
                                               @RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {

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
    public ResponseEntity<byte[]> getClients(@PathVariable String format,
                                             @RequestParam(required = false) Integer ID_Client,
                                             @RequestParam(required = false) String Name,
                                             @RequestParam(required = false) String Phone,
                                             @RequestParam(required = false) String Email,
                                             @RequestParam(required = false) String Adress,
                                             @RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Clients/{format}/";
        String params = String.format("ID_Client=%s,Name=%s,Phone=%s,Email=%s, Adress=%s", ID_Client, Name,Phone,Email,Adress);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        String key = cacheKeyClient(ID_Client,Name,Phone,Email,Adress);
        if (allNull(ID_Client, Name, Phone, Email, Adress)) {
            makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 400, 0);
            return badRequest(format, download, "Provide at least one parameter");
        }
        List<Client> clients = new ArrayList<>();
        Cache<String,ArrayList<Client>> searchClientCache= getsearchClientCache();
        Cache<Integer,Client> clientCache= getClientCache();

        if (searchClientCache.get(key)!= null){
            if (searchClientCache.get(key).isEmpty()){
                makeLog(endpoint,params,startTime,startMem,startCpu,0,0,204,0);
                return emptyResponse(format, download);
            }else{
                clients.addAll(searchClientCache.get(key));
            }
        }

        long dbTime = 0;
        if (clients.isEmpty()){
            long dbTime2 = System.nanoTime();
            clients = clientRepository.getClientsParamether(ID_Client, Name, Phone, Email, Adress);
            dbTime += duration(dbTime2);
            if(clients.isEmpty()){
                searchClientCache.put(key,new ArrayList<>());
                makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
                return emptyResponse(format, download);
            }
            searchClientCache.put(key,new ArrayList<>(clients));
        }

        long parseTime=0;
        StringBuilder result = new StringBuilder();
        if (format.equalsIgnoreCase("csv")){
            result.append("ID_Client,Name,Phone,Email,Adress").append(System.lineSeparator());
        }
        for(Client client : clients){
            clientCache.put(client.getId(),client);
            long parseTime2=System.nanoTime();
            result.append(parser.parseClient(format, client));
            parseTime+=duration(parseTime2);
        }

        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.toString().length());
        return returnResponse(format, download, result.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Reservations/{format}/")
    public ResponseEntity<byte[]> getReservations(@PathVariable String format,
                                                  @RequestParam(required = false) Integer ID_Reservations,
                                                  @RequestParam(required = false) Integer ID_Library,
                                                  @RequestParam(required = false) Integer ID_Book,
                                                  @RequestParam(required = false) Integer ID_Clients,
                                                  @RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
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
    public ResponseEntity<byte[]> getBooks(@PathVariable String format,@RequestParam(required = false) Integer ID_Book,@RequestParam(required = false) String ISBN,@RequestParam(required = false) String Title,@RequestParam(required = false) String Sinopse,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Books/{format}/";
        String params = String.format("ID_Book=%s,ISBN=%s,Title=%s,Sinopse=%s", ID_Book, ISBN, Title, Sinopse);
        String Key = cacheKeyBook(ID_Book,ISBN,Title,Sinopse);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime =0;

        if (allNull(ID_Book, ISBN, Title, Sinopse)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        Cache<String,ArrayList<Book>> searchBook = getsearchBookCache();
        Cache<Integer,Book> bookCache = getBookCache();
        List<Book> books= new ArrayList<>();
        Cache<String,String> cacheBookS = getBookString();

        if (searchBook.get(Key)!= null){
            if (searchBook.get(Key).isEmpty()){
                makeLog(endpoint,params,startTime,startMem,startCpu,0,0,204,0);
                return emptyResponse(format, download);
            }else{
                books.addAll(searchBook.get(Key));
            }
        }

        if (books.isEmpty()){
            long startDb1 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(ID_Book, ISBN, Title, Sinopse);
            dbTime += duration(startDb1);
            if (books.isEmpty()) {
                searchBook.put(Key, new ArrayList<>());
                makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
                return emptyResponse(format, download);
            }else{
                searchBook.put(Key,new ArrayList<>(books));
            }
        }

        StringBuilder result= new StringBuilder();
        long parseTime=0;
        if (format.equalsIgnoreCase("csv")){
            result.append("ID Book,ISBN,Author_Name,Author_ID,Title,Sinopse").append(System.lineSeparator());
        }

        for (Book book : books){
            bookCache.putIfAbsent(book.getid(),book);
            String key = format + book.getid();
            if (cacheBookS.get(key) != null){
                result.append(cacheBookS.get(key));
            }else {
                long startDb2 = System.nanoTime();
                List<Author> authors = authorRepository.getAuthorsParamether(bookAuthorRepository.getAuthorIDByBook(book.getid()), null, null, null);
                dbTime += duration(startDb2);
                long startParse = System.nanoTime();
                String bookS = "";
                List<Book> books2 = new ArrayList<>();
                books2.add(book);
                if (authors.isEmpty()) {
                    bookS = parser.parseBooks(format, books2, "", 0);
                } else {
                    bookS = parser.parseBooks(format, books2, authors.getFirst().getName(), authors.getFirst().getId());
                }
                result.append(bookS);
                parseTime += duration(startParse);
                cacheBookS.put(key, bookS);
            }
        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.toString().length());
        return returnResponse(format, download, result.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Books/{format}/AuthorName/{Author_Name}/")
    public ResponseEntity<byte[]> getBooksByAuthorName(@PathVariable String format,@PathVariable("Author_Name") String Author_Name,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Books/AuthorName/";
        String params = String.format("Author_Name=%s",Author_Name);
        String Key = "Name="+Author_Name;
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime = 0;

        if (allNull(Author_Name)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        List <Author> authors= new ArrayList<>();
        Cache<Integer,Author> authorCache = getAuthorCache();
        Cache<String,ArrayList<Author>> AuthorSearchCache = getsearchAuthorCache();
        Cache<Integer,Book> bookCache = getBookCache();
        Cache<String,String> cacheBookS = getBookString();

        if (AuthorSearchCache.get(Key)!= null){
            if (AuthorSearchCache.get(Key).isEmpty()){
                makeLog(endpoint,params,startTime,startMem,startCpu,0,0,204,0);
                return emptyResponse(format, download);
            }else{
                authors.addAll(AuthorSearchCache.get(Key));
            }
        }

        if (authors.isEmpty()){
            long dbTime2= System.nanoTime();
            authors=authorRepository.getAuthorsParamether(null,Author_Name,null,null);
            dbTime += duration(dbTime2);
            if (authors.isEmpty()) {
                AuthorSearchCache.put(Key,new ArrayList<>());
                makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
                return emptyResponse(format, download);
            }else{
                AuthorSearchCache.put(Key,new ArrayList<>(authors));
            }
        }


        StringBuilder result= new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            result.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }


        long parseTime=0;
        for (Author author : authors){
            authorCache.putIfAbsent(author.getId(),author);
            List<Book> books= new ArrayList<>();
            long dbTime2=System.nanoTime();
            List<Integer> bookIDs = bookAuthorRepository.getBooksIDbyAuthor(author.getId());
            dbTime +=duration(dbTime2);

            for (Integer bookID : bookIDs){
                if (bookCache.get(bookID)!=null){
                    books.add(bookCache.get(bookID));
                    bookIDs.remove(bookID);
                }
            }

            dbTime2=System.nanoTime();
            books.addAll(bookRepository.getBooksByIDS(bookIDs));
            dbTime+=duration(dbTime2);

            long parseTime2=System.nanoTime();
            for (Book book : books){
                String key = format + book.getid();
                if (cacheBookS.get(key)!=null){
                    result.append(cacheBookS.get(key));
                }else {
                    String s=parser.parseBooksV2(format, book, author);
                    result.append(s);
                    cacheBookS.put(key,s);
                }
            }
            parseTime+=duration(parseTime2);
        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.toString().length());
        return returnResponse(format, download, result.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Books/{format}/AuthorID/{Author_ID}/")
    public ResponseEntity<byte[]> getBooksByAuthorID(@PathVariable String format,@PathVariable("Author_ID") Integer Author_ID,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Books/AuthorID/";
        String params = String.format("AuthorID=%s",Author_ID);
        String Key = "ID_Authors"+Author_ID;
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(Author_ID)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        List <Author> authors= new ArrayList<>();
        Cache<Integer,Author> authorCache = getAuthorCache();
        Cache<String,ArrayList<Author>> AuthorSearchCache = getsearchAuthorCache();
        Cache<Integer,Book> bookCache = getBookCache();
        Cache<String,String> cacheBookS = getBookString();

        if (AuthorSearchCache.get(Key)!= null){
            if (AuthorSearchCache.get(Key).isEmpty()){
                makeLog(endpoint,params,startTime,startMem,startCpu,0,0,204,0);
                return emptyResponse(format, download);
            }else{
                authors.addAll(AuthorSearchCache.get(Key));
            }
        }
        long dbTime = 0;
        if (authors.isEmpty()){
            long dbTime2= System.nanoTime();
            authors = authorRepository.getAuthorsParamether(Author_ID,null,null,null);
            dbTime = duration(dbTime2);
            if (authors.isEmpty()){
                AuthorSearchCache.put(Key,new ArrayList<>());
                makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
                return emptyResponse(format, download);
            }else{
                AuthorSearchCache.put(Key,new ArrayList<>(authors));
            }

        }


        StringBuilder result= new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            result.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }
        long parseTime=0;
        for (Author author : authors){
            authorCache.putIfAbsent(author.getId(),author);
            List<Book> books= new ArrayList<>();
            long dbTime2=System.nanoTime();
            List<Integer> bookIDs = bookAuthorRepository.getBooksIDbyAuthor(author.getId());
            dbTime +=duration(dbTime2);

            for (Integer bookID : bookIDs){
                if (bookCache.get(bookID)!=null){
                    books.add(bookCache.get(bookID));
                    bookIDs.remove(bookID);
                }
            }

            dbTime2=System.nanoTime();
            books.addAll(bookRepository.getBooksByIDS(bookIDs));
            dbTime+=duration(dbTime2);

            long parseTime2=System.nanoTime();
            for (Book book : books){
                String key = format + book.getid();
                if (cacheBookS.get(key)!=null){
                    result.append(cacheBookS.get(key));
                }else {
                    String s=parser.parseBooksV2(format, book, author);
                    result.append(s);
                    cacheBookS.put(key,s);
                }
            }
            parseTime+=duration(parseTime2);
        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.toString().length());
        return returnResponse(format, download, result.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Books/{format}/BookName/{Book_Name}/")
    public ResponseEntity<byte[]> getAuthorsByBookName(@PathVariable String format,@PathVariable("Book_Name") String Book_Name,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Authors/Book_Name/";
        String params = String.format("Book_Name=%s",Book_Name);
        String Key = "Title"+Book_Name;
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(Book_Name)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        List <Book> books= new ArrayList<>();
        Cache<String,ArrayList<Book>> BookSearchCache = getsearchBookCache();
        Cache<Integer,Book> bookCache = getBookCache();
        Cache<String,String> cacheBookS = getBookString();

        if (BookSearchCache.get(Key)!= null){
            if (BookSearchCache.get(Key).isEmpty()){
                makeLog(endpoint,params,startTime,startMem,startCpu,0,0,204,0);
                return emptyResponse(format, download);
            }else{
                books.addAll(BookSearchCache.get(Key));
            }
        }
        long dbTime = 0;
        if (books.isEmpty()){
            long dbTime2 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(null,Book_Name,null,null);
            dbTime = duration(dbTime2);
            if (books.isEmpty()){
                BookSearchCache.put(Key,new ArrayList<>());
                makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
                return emptyResponse(format, download);
            }else{
                BookSearchCache.put(Key,new ArrayList<>(books));
            }
        }


        StringBuilder result= new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            result.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }

        long parseTime=0;
        for (Book book : books){
            bookCache.putIfAbsent(book.getid(),book);
            String key = format + book.getid();
            if (cacheBookS.get(key)!= null){
                result.append(cacheBookS.get(key));
            }else {
                long dbTime2 = System.nanoTime();
                Integer authorID = bookAuthorRepository.getAuthorIDByBook(book.getid());
                List<Author> authors = authorRepository.getAuthorsParamether(authorID, null, null, null);
                dbTime += duration(dbTime2);
                Author author = null;
                if (!authors.isEmpty()) {
                    author = authors.getFirst();
                }
                dbTime += duration(dbTime2);
                long parseTime2 = System.nanoTime();
                String bookS= parser.parseBooksV2(format, book, author);
                parseTime += duration(parseTime2);
                cacheBookS.putIfAbsent(key,bookS);
                result.append(bookS);
            }
        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.toString().length());
        return returnResponse(format, download, result.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Books/{format}/BookID/{Book_ID}/")
    public ResponseEntity<byte[]> getAuthorsByBookID(@PathVariable String format,@PathVariable("Book_ID") Integer Book_ID,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Authors/Book_ID/";
        String params = String.format("Book_ID=%s",Book_ID);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(Book_ID)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        Cache<Integer,Book> bookCache = getBookCache();
        Cache<String,String> cacheBookS = getBookString();
        long dbTime=0;
        Book book = bookCache.get(Book_ID);
        if (book==null){
            long dbTime2 = System.nanoTime();
            List<Book> books = bookRepository.getBooksbyParamether(Book_ID,null,null,null);
            dbTime += duration(dbTime2);
            if (books.isEmpty()){
                makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
                return emptyResponse(format, download);
            }else{
                book = books.getFirst();
                bookCache.put(book.getid(),book);
            }
        }





        StringBuilder response=new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            response.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }

        long parseTime=0;

        String Key = format + book.getid();
        if (cacheBookS.get(Key)!= null){
            response.append(cacheBookS.get(Key));
        }else{
            long startDb2 = System.nanoTime();
            List<Author> authors = authorRepository.getAuthorsParamether(bookAuthorRepository.getAuthorIDByBook(book.getid()), null, null, null);
            dbTime += duration(startDb2);
            long startParse = System.nanoTime();
            String bookS = "";
            if (authors.isEmpty()) {
                bookS = parser.parseBooksV2(format,book,null);

            } else {
                bookS = parser.parseBooksV2(format,book,authors.getFirst());
            }
            response.append(bookS);
            parseTime += duration(startParse);
            cacheBookS.put(Key, bookS);
        }

        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,response.toString().length());
        return returnResponse(format, download, response.toString().getBytes(StandardCharsets.UTF_8));
    }


    // ============= Reservations ======== //
    @GetMapping("/Reservations/{format}/ID_Reservation/{ID_Reservation}/Info")
    public ResponseEntity<?> getReservationInfoByReservationID(@PathVariable String format, @PathVariable Integer ID_Reservation) throws JsonProcessingException {
        String endpoint = "/Reservations/ID_Reservation/";
        String params = String.format("ID_Reservation=%s",ID_Reservation);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(ID_Reservation)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        Cache<Integer,Reservation> reservationCache = getReservationCache();
        Reservation reservation = reservationCache.get(ID_Reservation);


        long dbTime = 0;
        if(reservation == null){
            long dbTime2 = System.nanoTime();
            List<Reservation> reservations = reservationRepository.getReservationParamether(ID_Reservation, null, null, null);
            dbTime += duration(dbTime2);
            if (reservations.isEmpty()) {
                reservationCache.put(ID_Reservation,new Reservation());
                makeLog(endpoint, params, startTime, startMem, startCpu, dbTime, 0, 204, 0);
                return emptyResponse(format, false);
            }else {
                reservation = reservations.getFirst();
                reservationCache.put(ID_Reservation,reservation);
            }
        }
        if (reservation.equals(new Reservation())){
            makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 204, 0);
            return emptyResponse(format, false);
        }

        Cache <Integer,Book> bookCache = getBookCache();
        Cache<Integer,Client> clientCache = getClientCache();
        Cache <Integer,Author> authorCache= getAuthorCache();
        Client client = clientCache.get(reservation.getIdClients());
        Book book = bookCache.get(reservation.getIdBook());

        List<Book> books = new ArrayList<>();
        List<Client> clients = new ArrayList<>();
        List<Author> authors = new ArrayList<>();

        if (book != null) {
            books.add(book);
        }
        if (client!=null) {
            clients.add(client);
        }

        long dbtime2=System.nanoTime();
        if (books.isEmpty()){
             books=bookRepository.getBooksbyParamether(reservation.getIdBook(), null, null, null);
        }
        if (clients.isEmpty()){
            clients= clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
        }

        List<Library> libraries = libraryRepository.getLibrariesParamether(reservation.getIdLibrary(), null, null, null);
        Integer authorId = bookAuthorRepository.getAuthorIDByBook(books.getFirst().getid());
        dbTime += duration(dbtime2);
        Author author = authorCache.get(authorId);
        if (author != null){
        authors.add(author);
        }
        if (authors.isEmpty()){
            dbtime2=System.nanoTime();
            authors = authorRepository.getAuthorsByIDs(Collections.singletonList(authorId));
        }

        dbTime += duration(dbtime2);

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
        String endpoint = "/Reservations/ClientName/";
        String params = String.format("ClientName=%s",ClientName);

        if (allNull(ClientName)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        String key= "Name="+ClientName;
        List<Client> clients = new ArrayList<>();
        Cache<String,ArrayList<Client>> searchClientCache= getsearchClientCache();
        clients= searchClientCache.get(key);


        if (clients != null){
            if (clients.isEmpty()){
                makeLog(endpoint,params,startTime,startMem,startCpu,0,0,204,0);
                return emptyResponse(format,false);
            }
        }

        long dbTime = 0;
        if (clients == null){
            long dbTime2 = System.nanoTime();
            clients = clientRepository.getClientsParamether(null,ClientName, null, null, null);
            dbTime+= duration(dbTime2);
            if (clients.isEmpty()) {
                searchClientCache.put(key,new ArrayList<>());
                makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
                return emptyResponse(format,false);
            }
        }



        StringBuilder response= new StringBuilder();

        List<Reservation> reservations= new ArrayList<>();
        Set<Reservation> reservationSet = new HashSet<>(reservations);
        long dbtime2=System.nanoTime();
        for (Client client : clients) {
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


        Cache <Integer,Book> bookCache = getBookCache();


        long parseTime = 0;
        if (format.equals("json")) {
            response.append("{");
        }
        for (Reservation reservation : reservations) {


            Integer authorId = null;
            Book book = bookCache.get(reservation.getIdBook());
            List<Book> books= new ArrayList<>();

            if (book == null){
                long dbtime3=System.nanoTime();
                books= bookRepository.getBooksbyParamether(reservation.getIdBook(), null, null, null);
                dbTime+= duration(dbtime3);
            }else{
                books.add(book);
            }
            long dbtime4=System.nanoTime();
            if (!books.isEmpty()) {
                authorId = bookAuthorRepository.getAuthorIDByBook(books.getFirst().getid());
            }

            List<Author> authors = (authorId != null)
                    ? authorRepository.getAuthorsByIDs(Collections.singletonList(authorId))
                    : Collections.emptyList();

            List<Library> libraries = libraryRepository.getLibrariesParamether(reservation.getIdLibrary(), null, null, null);
            dbTime += duration(dbtime4);

            long parseTime2=System.nanoTime();
            response.append(parser.parseReservationsV2(format,reservation,libraries,clients,books,authors)+"\n");
            parseTime+=duration(parseTime2);
        }
        if (format.equals("json")) {
            response.append("}");
        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,response.toString().length());
        return returnResponse(format,false,response.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Reservations/{format}/Library/{LibraryName}/Info")
    public ResponseEntity<?> getReservationInfoByLibrary(@PathVariable String format, @PathVariable String LibraryName) throws JsonProcessingException {
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        String res="";
        String endpoint = "/Reservations/LibraryName/";
        String params = String.format("LlibraryName=%s",LibraryName);

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

        long parseTime = 0;
        for (Reservation reservation : reservations) {
            long dbtime3=System.nanoTime();
            List<Book> books = bookRepository.getBooksbyParamether(reservation.getIdBook(), null, null, null);
            Integer authorId = null;
            if (!books.isEmpty()) {
                authorId = bookAuthorRepository.getAuthorIDByBook(books.getFirst().getid());
            }

            List<Author> authors = (authorId != null)
                    ? authorRepository.getAuthorsByIDs(Collections.singletonList(authorId))
                    : Collections.emptyList();
            List<Client> client = clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
            dbTime += duration(dbtime3);

            long parseTime2=System.nanoTime();
            res += parser.parseReservationsV2(format,reservation,libraries,client,books,authors)+"\n";
            parseTime+=duration(parseTime2);
        }

        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,res.length());
        return returnResponse(format,false,res.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Reservations/{format}/Book/{BookTitle}/Info")
    public ResponseEntity<?> getReservationInfoByBook(@PathVariable String format, @PathVariable String BookTitle) throws JsonProcessingException {
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        String endpoint = "/Reservations/BookTitle/";
        String params = String.format("BookTitle=%s",BookTitle);
        String key = "Title=" + BookTitle;

        Cache <String,ArrayList<Book>> cacheBooks= getsearchBookCache();
        List<Book> books = cacheBooks.get(key);
        long dbTime= 0;
        if (books == null){
            long dbTime2 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(null,null,BookTitle,null);
            dbTime+=duration(dbTime2);
        }
        if (books.isEmpty()) {
            cacheBooks.put(key,new ArrayList<>());
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,204,0);
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
        StringBuilder response = new StringBuilder();
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
            List<Client> client = clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
            dbTime += duration(dbtime3);

            long parseTime2=System.nanoTime();
            response.append(parser.parseReservationsV2(format,reservation,libraries,client,books,authors)+"\n");
            parseTime+=duration(parseTime2);
        }

        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,response.toString().length());
        return returnResponse(format,false,response.toString().getBytes(StandardCharsets.UTF_8));
    }


    @GetMapping("/Stocks/{format}/Book/{bookTitle}")
    public ResponseEntity<?> getStockInfoByBook(@PathVariable String format,@PathVariable String bookTitle) throws JsonProcessingException {
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        String key = "Title="+bookTitle;
        String endpoint = "/Stocks/BookTitle/";
        String params = String.format("BookTitle=%s", bookTitle);

        if (allNull(bookTitle, format)) {
            makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 400, 0);
            return badRequest(format, false, "Bad Request");
        }


        Cache <String,ArrayList<Book>> cacheBooks= getsearchBookCache();
        List<Book> books = cacheBooks.get(key);


        long dbTime = 0;
        if (books == null) {
            long dbTime2 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(null, null, bookTitle, null);
            dbTime += duration(dbTime2);
        }

        if (books.isEmpty()){
            cacheBooks.put(key,new ArrayList<>());
            makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 204, 0);
            return emptyResponse(format, false);
        }

        StringBuilder response = new StringBuilder();
        if ("csv".equalsIgnoreCase(format)) {
            response.append("ID_Book,ISBN,bookTitle,Library_ID,Library_Name,Stock\n");
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


        makeLog(endpoint, params, startTime, startMem, startCpu, dbTime, parseTime, 200, response.toString().length());
        return returnResponse(format, false, response.toString().getBytes(StandardCharsets.UTF_8));
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

        if (format.equalsIgnoreCase("csv")) {
            res="ID_Book,ISBN,bookTitle,Library_ID,Library_Name,Stock\n";
        }


        String cacheKey = Library + "_" + format;

        Cache<String, String> cache = getStockCache();
        if (cache != null) {
            String cachedRes = cache.get(cacheKey);
            if (cachedRes != null) {
                if ("__NULL__".equals(cachedRes)) {
                    makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 204, 0);
                    return emptyResponse(format, false);
                }
                makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 200, cachedRes.length());
                return returnResponse(format, false, cachedRes.getBytes(StandardCharsets.UTF_8));
            }
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
        if (cache != null) cache.put(cacheKey, res);
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

    @Autowired
    private CacheManager cacheManager;

    private Cache<String, String> getStockCache() {
        return cacheManager.getCache("stockBookCache", String.class, String.class);
    }
    private Cache<Integer, Client> getClientCache() {
        return cacheManager.getCache("ClientCache", Integer.class, Client.class);
    }
    private Cache<Integer, Book> getBookCache() {
        return cacheManager.getCache("BookCache", Integer.class, Book.class);
    }
    private Cache<Integer, Author> getAuthorCache() {
        return cacheManager.getCache("AuthorCache", Integer.class, Author.class);
    }
    private Cache<String, ArrayList<Author>> getsearchAuthorCache() {
        return (Cache<String, ArrayList<Author>>) (Cache<?, ?>) cacheManager.getCache("AuthorSearchCache", String.class, ArrayList.class);
    }
    private Cache<String, ArrayList<Client>> getsearchClientCache() {
        return (Cache<String, ArrayList<Client>>) (Cache<?, ?>) cacheManager.getCache("ClientSearchCache", String.class, ArrayList.class);
    }
    private Cache<String, ArrayList<Book>> getsearchBookCache() {
        return (Cache<String, ArrayList<Book>>) (Cache<?, ?>) cacheManager.getCache("BookSearchCache", String.class, ArrayList.class);
    }
    private Cache<String, String> getBookString() {
        return cacheManager.getCache("BookSCache", String.class, String.class);
    }
    private Cache<Integer,Reservation> getReservationCache() {
        return cacheManager.getCache("ReservationCache", Integer.class, Reservation.class);
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
