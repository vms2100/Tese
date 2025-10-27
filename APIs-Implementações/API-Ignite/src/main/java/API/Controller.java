package API;

import Repository.*;
import Tables.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
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
    private final CacheManager cacheManager;

    public Controller(BookRepository bookRepository, AuthorRepository authorRepository,
                      BookAuthorRepository bookAuthorRepository, LibraryRepository libraryRepository,
                      ReservationRepository reservationRepository, ClientRepository clientRepository, StockRepository stockRepository, CacheManager cacheManager) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.bookAuthorRepository = bookAuthorRepository;
        this.libraryRepository = libraryRepository;
        this.reservationRepository = reservationRepository;
        this.clientRepository = clientRepository;
        this.stockRepository = stockRepository;
        this.parser = new Parser();
        this.cacheManager = cacheManager;
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
                                             @RequestParam(defaultValue = "false") boolean download) throws IOException, ClassNotFoundException {
        String endpoint = "/Authors/{format}/";
        String params = String.format("ID_Authors=%s,Name=%s,Age=%d,Bibliograhy=%s", ID_Authors,Name,age,Bibliography);
        String key = cacheKeyAuhtors(ID_Authors, Name, age, Bibliography);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime =0;

        if (allNull(ID_Authors, Name, age, Bibliography)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }
        List<Author> authors= cacheManager.get("AuthorSearchCache",key,ArrayList.class);

        if (authors== null){
            long startDb1 = System.nanoTime();
            authors = authorRepository.getAuthorsParamether(ID_Authors, Name, age, Bibliography);
            dbTime += duration(startDb1);
            cacheManager.put("AuthorSearchCache", key, (Serializable) authors);
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
                                             @RequestParam(defaultValue = "false") boolean download) throws IOException, ClassNotFoundException {


        String endpoint = "/Clients/{format}/";
        String params = String.format("ID_Client=%s,Name=%s,Phone=%s,Email=%s, Adress=%s", ID_Client, Name,Phone,Email,Adress);
        String key = cacheKeyClient(ID_Client,Name,Phone,Email,Adress);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime=0;
        if (allNull(ID_Client, Name, Phone, Email, Adress)) {
            makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 400, 0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        List<Client> clients= cacheManager.get("ClientSearchCache",key,ArrayList.class);

        if (clients==null) {
            long dbTime2 = System.nanoTime();
            clients = clientRepository.getClientsParamether(ID_Client, Name, Phone, Email, Adress);
            dbTime += duration(dbTime2);
            cacheManager.put("ClientSearchCache", key, (Serializable) clients);
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
    public ResponseEntity<byte[]> getBooks(@PathVariable String format,
                                           @RequestParam(required = false) Integer ID_Book,
                                           @RequestParam(required = false) String ISBN,
                                           @RequestParam(required = false) String Title,
                                           @RequestParam(required = false) String Sinopse,
                                           @RequestParam(defaultValue = "false") boolean download) throws IOException, ClassNotFoundException {

        String endpoint = "/Books/{format}/";
        String params = String.format("ID_Book=%s,ISBN=%s,Title=%s,Sinopse=%s", ID_Book, ISBN, Title, Sinopse);
        String key = cacheKeyBook(ID_Book,ISBN,Title,Sinopse);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime = 0;

        if (allNull(ID_Book, ISBN, Title, Sinopse)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        List<Book> books = cacheManager.get("BookSearchCache",key,ArrayList.class);
        if (books== null) {
            long startDb1 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(ID_Book, ISBN, Title, Sinopse);
            dbTime += duration(startDb1);
            cacheManager.put("BookSearchCache", key, (Serializable) books);
        }

        if (books.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder response = new StringBuilder();
        if (format.equalsIgnoreCase("csv")){
            response.append("ID Book,ISBN,Author_Name,Author_ID,Title,Sinopse").append(System.lineSeparator());
        }
        long parseTime=0;
        for (Book book : books){
            cacheManager.putIfAbsent("BookCache",Integer.toString(book.getid()),Book.class);
            String Key = format + book.getid();

            String res = cacheManager.get("BookSearchCache",Key,String.class);
            if (res == null){
                long startDb2 = System.nanoTime();
                List<Author> authors = authorRepository.getAuthorsParamether(bookAuthorRepository.getAuthorIDByBook(book.getid()), null, null, null);
                dbTime += duration(startDb2);

                String resultQ ="";
                List<Book> books2=new ArrayList<>();
                books2.add(book);
                long startParse = System.nanoTime();
                if (authors.isEmpty()) {
                    resultQ += parser.parseBooks(format, books2, "", 0);
                } else {
                    resultQ += parser.parseBooks(format, books2, authors.getFirst().getName(), authors.getFirst().getId());
                }
                response.append(resultQ);
                parseTime += duration(startParse);
                cacheManager.get("BookSCache",Key,String.class);
            }else{
                response.append(res);
            }

        }
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,response.length());
        return returnResponse(format, download, response.toString().getBytes(StandardCharsets.UTF_8));
    }






    @GetMapping("/Books/{format}/AuthorName/{Author_Name}/")
    public ResponseEntity<byte[]> getBooksByAuthorName(@PathVariable String format,@PathVariable("Author_Name") String Author_Name,@RequestParam(defaultValue = "false") boolean download) throws IOException, ClassNotFoundException {
        String endpoint = "/Books/AuthorName/";
        String params = String.format("Author_Name=%s",Author_Name);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime = 0;
        String Key = "Name="+Author_Name;

        if (allNull(Author_Name)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        List<Author> authors = cacheManager.get("AuthorSearchCache",Key, ArrayList.class);

        if (authors == null) {
            long startDb2 = System.nanoTime();
            authors = authorRepository.getAuthorsParamether(null,Author_Name,null,null);
            dbTime = duration(startDb2);
            cacheManager.put("AuthorSearchCache", Key, (Serializable) authors);
        }

        if (authors.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder res=new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            res.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }

        long parseTime=0;
        for (Author author : authors){
            cacheManager.putIfAbsent("AuthorCache",Key,Author.class);

            long dbTime2=System.nanoTime();
            List<Integer> bookIDs = bookAuthorRepository.getBooksIDbyAuthor(author.getId());
            dbTime+=duration(dbTime2);

            List<Book> books= new ArrayList<>();
            for (Integer bookID : bookIDs){
                books.add(cacheManager.get("BookCache",Integer.toString(bookID),Book.class));
                bookIDs.remove(bookID);
            }

            dbTime2=System.nanoTime();
            books.addAll(bookRepository.getBooksByIDS(bookIDs));
            dbTime+=duration(dbTime2);

            long parseTime2=System.nanoTime();
            for (Book book : books){
                String key = format + book.getid();
                String s = (String) cacheManager.getIfPresent("BookSCache",Key);
                if (s == null){
                    s =parser.parseBooksV2(format,book,author);
                    cacheManager.putIfAbsent("BookSCache",Key,s);
                }
                res.append(s);
            }
            parseTime+=duration(parseTime2);
        }

        String result = res.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }



    @GetMapping("/Books/{format}/AuthorID/{Author_ID}/")
    public ResponseEntity<byte[]> getBooksByAuthorID(@PathVariable String format,@PathVariable("Author_ID") Integer Author_ID,@RequestParam(defaultValue = "false") boolean download) throws IOException, ClassNotFoundException {
        String endpoint = "/Books/AuthorID/";
        String params = String.format("AuthorID=%s",Author_ID);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime = 0;
        String Key = "ID_Authors="+Author_ID;


        if (allNull(Author_ID)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }


        List<Author> authors = cacheManager.get("AuthorSearchCache",Key, ArrayList.class);
        if (authors == null) {
            long dbTime1=System.nanoTime();
            authors = authorRepository.getAuthorsParamether(Author_ID,null,null,null);
            dbTime = duration(dbTime1);
            cacheManager.put("AuthorSearchCache", Key, (Serializable) authors);
        }

        if (authors.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder res=new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            res.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }


        long parseTime=0;
        for (Author author : authors){
            List<Book> books= new ArrayList<>();
            cacheManager.putIfAbsent("AuthorCache",Key,Author.class);
            long dbTime2=System.nanoTime();
            List<Integer> booksIds=bookAuthorRepository.getBooksIDbyAuthor(author.getId());

            for (Integer bookID : booksIds){
                Book book = (Book) cacheManager.getIfPresent("BookCache",Integer.toString(bookID));
                if (book!=null){
                    books.add(book);
                    booksIds.remove(bookID);
                }
            }
            books.addAll(bookRepository.getBooksByIDS(booksIds));

            dbTime+=duration(dbTime2);

            long parseTime2=System.nanoTime();
            for (Book book : books){
                String key = format + book.getid();
                String s = (String) cacheManager.getIfPresent("BookSCache",key);
                if (s == null){
                    s =parser.parseBooksV2(format,book,author);
                    cacheManager.put("BookSCache",key,s);
                }
                res.append(s);
            }
            parseTime+=duration(parseTime2);
        }

        String result = res.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,res.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }


    @GetMapping("/Books/{format}/BookName/{Book_Name}/")
    public ResponseEntity<byte[]> getAuthorsByBookName(@PathVariable String format,@PathVariable("Book_Name") String Book_Name,@RequestParam(defaultValue = "false") boolean download) throws IOException, ClassNotFoundException {
        String endpoint = "/Authors/Book_Name/";
        String params = String.format("Book_Name=%s",Book_Name);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime = 0;

        String Key = "Title="+Book_Name;

        if (allNull(Book_Name)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        List<Book> books = cacheManager.get("BookSearchCache",Key,ArrayList.class);

        if (books == null) {
            long dbTime1=System.nanoTime();
            books=bookRepository.getBooksbyParamether(null,Book_Name,null,null);
            dbTime += duration(dbTime1);
            cacheManager.put("BookSearchCache",Key, (Serializable) books);
        }

        if (books.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder response = new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            response.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }

        long parseTime=0;
        for (Book book : books){
            cacheManager.putIfAbsent("BookCache",Key,Book.class);
            String key = format + book.getid();
            String s =(String) cacheManager.getIfPresent("BookSCache",key);
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
                response.append(s2);
                cacheManager.put("BookSCache",key,s2);
                parseTime+=duration(parseTime2);
            }else{
                response.append(s);
            }

        }
        String result = response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }


    @GetMapping("/Books/{format}/BookID/{Book_ID}/")
    public ResponseEntity<byte[]> getAuthorsByBookID(@PathVariable String format,@PathVariable("Book_ID") Integer Book_ID,@RequestParam(defaultValue = "false") boolean download) throws IOException, ClassNotFoundException {
        String endpoint = "/Authors/Book_ID/";
        String params = String.format("Book_ID=%s",Book_ID);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime = 0;
        String key = "ID_Book="+Book_ID;
        if (allNull(Book_ID)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        List<Book> books = cacheManager.get("BookSearchCache",key,ArrayList.class);

        if (books == null) {
            long dbTime2=System.nanoTime();
            books=bookRepository.getBooksbyParamether(Book_ID,null,null,null);
            dbTime += duration(dbTime2);
            cacheManager.put("BookSearchCache",key,(Serializable) books);
        }

        if (books.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder response = new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            response.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }

        long parseTime=0;
        for (Book book : books){
            String Key = format + book.getid();
            cacheManager.putIfAbsent("BookCache",Key,Book.class);
            String s = (String) cacheManager.getIfPresent("BookSCache",Key);
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
                response.append(s2);
                parseTime+=duration(parseTime2);
                cacheManager.put("BookSCache",Key,s2);
            }else{
                response.append(s);
            }

        }

        String result = response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }


    // ============= Reservations ======== //
    @GetMapping("/Reservations/{format}/ID_Reservation/{ID_Reservation}/Info")
    public ResponseEntity<?> getReservationInfoByReservationID(@PathVariable String format, @PathVariable Integer ID_Reservation) throws IOException, ClassNotFoundException {
        String endpoint = "/Reservations/ID_Reservation/";
        String params = String.format("ID_Reservation=%s",ID_Reservation);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        long dbTime = 0;
        List<Reservation> reservations = new ArrayList<>();

        if (allNull(ID_Reservation)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        Reservation reservation = (Reservation) cacheManager.getIfPresent("ReservationCache",ID_Reservation.toString());

        if (reservation==null) {
            long dbTime2 = System.nanoTime();
            reservations = reservationRepository.getReservationParamether(ID_Reservation, null, null, null);
            dbTime+= duration(dbTime2);
            if (reservations.isEmpty()) {
                makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
                Reservation r1 = new Reservation();
                cacheManager.put("ReservationCache",ID_Reservation.toString(),r1);
                return  emptyResponse(format,false);
            }
            reservation= reservations.getFirst();
        }

        List<Library> libraries = libraryRepository.getLibrariesParamether(reservation.getIdLibrary(), null, null, null);
        List<Client> clients = new ArrayList<>();
        List<Book> books = new ArrayList<>();
        List<Author> authors = new ArrayList<>();
        Client client = (Client) cacheManager.getIfPresent("ClientCache",Integer.toString(reservation.getIdClients()));
        Book book = (Book) cacheManager.getIfPresent("BookCache",Integer.toString(reservation.getIdBook()));

        if (client == null){
            long dbtime2=System.nanoTime();
            clients = clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
            dbTime += duration(dbtime2);
            cacheManager.put("ClientCache",Integer.toString(reservation.getIdClients()),client);
        }else{
            clients.add(client);
        }

        if (book == null){
            long dbtime2=System.nanoTime();
            books = bookRepository.getBooksbyParamether(reservation.getIdBook(), null, null, null);
            dbTime += duration(dbtime2);
            cacheManager.put("BookCache",Integer.toString(reservation.getIdBook()),book);
        }else{
            books.add(book);
        }

        long dbtime2=System.nanoTime();
        Integer authorId = bookAuthorRepository.getAuthorIDByBook(books.getFirst().getid());
        dbTime += duration(dbtime2);

        Author author = (Author) cacheManager.getIfPresent("AuthorCache",Integer.toString(authorId));
        if (author == null){
            dbtime2=System.nanoTime();
            List<Integer> authorIds = Collections.singletonList(authorId);
            authors = authorRepository.getAuthorsByIDs(authorIds);
            if (!authors.isEmpty()) {
                cacheManager.put("AuthorCache",Integer.toString(authorId),author);
            }
            dbTime += duration(dbtime2);
        }else{
            authors.add(author);
        }


        long parseTime=System.nanoTime();
        StringBuilder response= new StringBuilder();
        response.append(parser.parseReservationsV2(format,reservation,libraries,clients,books,authors));
        parseTime=duration(parseTime);

        String result = response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format,false,result.getBytes(StandardCharsets.UTF_8));
    }




    @GetMapping("/Reservations/{format}/client/{ClientName}/Info")
    public ResponseEntity<?> getReservationInfoByClientName(@PathVariable String format, @PathVariable String ClientName) throws IOException, ClassNotFoundException {
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();
        String endpoint = "/Reservations/ClientName/";
        String params = String.format("ClientName=%s",ClientName);
        long dbTime =0;

        if (allNull(ClientName)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        String Key = "Name=" + ClientName;
        List<Client> clients= (List<Client>) cacheManager.getIfPresent("ClientSearchCache",Key);

        if (clients == null){
            long dbTime2 = System.nanoTime();
            clients = clientRepository.getClientsParamether(null,ClientName, null, null, null);
            dbTime+= duration(dbTime2);
            cacheManager.put("ClientSearchCache",Key,ArrayList.class);
        }

        if (clients.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format,false);
        }

        List<Reservation> reservations= new ArrayList<>();
        Set<Reservation> reservationSet = new HashSet<>(reservations);
        long dbtime2=System.nanoTime();
        for (Client client : clients) {
            cacheManager.putIfAbsent("ClientCache",Integer.toString(client.getId()),client);
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
            Client client1 = (Client) cacheManager.getIfPresent("ClientCache",Integer.toString(reservation.getIdClients()));
            Book book = (Book) cacheManager.getIfPresent("BookCache",Integer.toString(reservation.getIdBook()));
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
                cacheManager.put("ClientCache",Integer.toString(reservation.getIdClients()),client1);
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
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format,false,result.getBytes(StandardCharsets.UTF_8));
    }



    @GetMapping("/Reservations/{format}/Library/{LibraryName}/Info")
    public ResponseEntity<?> getReservationInfoByLibrary(@PathVariable String format, @PathVariable String LibraryName) throws IOException, ClassNotFoundException {
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
            Book book = (Book) cacheManager.getIfPresent("BookCache",Integer.toString(reservation.getIdBook()));
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
            Client client1 =(Client) cacheManager.getIfPresent("ClientCache",Integer.toString(reservation.getIdClients()));
            if (client1 == null){
                dbtime3=System.nanoTime();
                client=clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
                dbTime += duration(dbtime3);
                cacheManager.put("ClientCache",Integer.toString(reservation.getIdClients()),client.getFirst());
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
    public ResponseEntity<?> getReservationInfoByBook(@PathVariable String format, @PathVariable String BookTitle) throws IOException, ClassNotFoundException {
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
        List<Book> books = (List<Book>) cacheManager.getIfPresent("BookSearchCache",Key);
        if (books == null){
            long dbTime2 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(null,null,BookTitle,null);
            dbTime+=duration(dbTime2);
            cacheManager.put("BookSearchCache",Key, (Serializable) books);
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
            Client client1 = (Client) cacheManager.getIfPresent("ClientCache",Integer.toString(reservation.getIdClients()));
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
    public ResponseEntity<?> getStockInfoByBook(@PathVariable String format, @PathVariable String BookTitle) throws IOException, ClassNotFoundException {
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
        List<Book> books = (List<Book>) cacheManager.getIfPresent("BookSearchCache",key);

        if (books == null){
            long dbTime2 = System.nanoTime();
            books = bookRepository.getBooksbyParamether(null,null,BookTitle,null);
            dbTime+=duration(dbTime2);
            cacheManager.put("BookSearchCache",key, (Serializable) books);
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
    public ResponseEntity<?> getStockInfoByLibrary(@PathVariable String format, @PathVariable String Library) throws IOException, ClassNotFoundException {
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

        res = (String) cacheManager.getIfPresent("stockBookCache",cacheKey);
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
        cacheManager.put("stockBookCache",cacheKey,res);
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,res.length());
        return returnResponse(format,false,res.getBytes(StandardCharsets.UTF_8));
    }



    // Export helpers
    private void streamAll(String format, HttpServletResponse response, String fileName,
                           String[] csvHeaders, RunnableJsonWriter writerLogic) throws IOException {
        String contentType = format.equalsIgnoreCase("json") ? "application/json" : "text/csv";
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        try (PrintWriter writer = response.getWriter()) {
            if (format.equalsIgnoreCase("csv") && csvHeaders != null)
                writer.println(String.join(",", csvHeaders));
            else if (format.equalsIgnoreCase("json"))
                writer.println("[");

            writerLogic.write(writer, format);

            if (format.equalsIgnoreCase("json"))
                writer.println("]");

            writer.flush();
        }
    }

    @FunctionalInterface
    private interface RunnableJsonWriter {
        void write(PrintWriter writer, String format);
    }

    @GetMapping("/Export/Books/{format}/")
    public void exportBooks(@PathVariable String format, HttpServletResponse response) throws IOException {
        streamAll(format, response, "books." + format,
                new String[]{"ID_Book", "ISBN", "Title", "Sinopse"},
                (writer, fmt) -> bookRepository.getAllBooks(rs -> {
                    try {
                        parser.parserAllBooks(fmt, rs, writer);
                    } catch (SQLException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }

    @GetMapping("/Export/Library/{format}/")
    public void exportLibraries(@PathVariable String format, HttpServletResponse response) throws IOException {
        streamAll(format, response, "libraries." + format,
                new String[]{"ID_Library", "Name", "Description", "Location"},
                (writer, fmt) -> libraryRepository.getAllLibraries(rs -> {
                    try {
                        parser.parserAllLibraries(fmt, rs, writer);
                    } catch (SQLException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
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
