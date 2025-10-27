package API;

import Repository.*;
import Tables.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
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
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(ID_Authors, Name, age, Bibliography)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }
        long startDb1 = System.nanoTime();
        List<Author> authors = authorRepository.getAuthorsParamether(ID_Authors, Name, age, Bibliography);
        long dbTime = duration(startDb1);

        if (authors.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder response = new StringBuilder();
        if (format.equalsIgnoreCase("csv")) {
            response.append("ID Author,Name,Age,Bibliography").append(System.lineSeparator());
        }

        long parseTime=System.nanoTime();
        response.append(parser.parseAuthors(format, authors));
        parseTime=duration(parseTime);

        String result = response.toString();
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
        StringBuilder response = new StringBuilder();
        response.append(parser.parseLibrarys(format, libraries));
        parseTime=duration(parseTime);
        String result = response.toString();

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

        if (allNull(ID_Client, Name, Phone, Email, Adress)) {
            makeLog(endpoint, params, startTime, startMem, startCpu, 0, 0, 400, 0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        long dbTime = System.nanoTime();
        List<Client> clients = clientRepository.getClientsParamether(ID_Client, Name, Phone, Email, Adress);
        dbTime = duration(dbTime);

        if (clients.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder response = new StringBuilder();
        if (format.equalsIgnoreCase("csv")){
            response.append("ID_Client,Name,Phone,Email,Adress").append(System.lineSeparator());
        }
        long parseTime=System.nanoTime();
        for (Client client : clients) {
            response.append(parser.parseClient(format, client));
            parseTime=duration(parseTime);
        }


        String result = response.toString();
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
        StringBuilder response = new StringBuilder();
        response.append(parser.parseReservations(format, reservations));
        parseTime=duration(parseTime);

        String result = response.toString();
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
                                           @RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {

        String endpoint = "/Books/{format}/";
        String params = String.format("ID_Book=%s,ISBN=%s,Title=%s,Sinopse=%s", ID_Book, ISBN, Title, Sinopse);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(ID_Book, ISBN, Title, Sinopse)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        long startDb1 = System.nanoTime();
        List<Book> books = bookRepository.getBooksbyParamether(ID_Book, ISBN, Title, Sinopse);
        long dbTime = duration(startDb1);

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
            long startDb2 = System.nanoTime();
            List<Author> authors = authorRepository.getAuthorsParamether(bookAuthorRepository.getAuthorIDByBook(book.getid()), null, null, null);
            dbTime += duration(startDb2);
            String resultQ ="";
            long startParse = System.nanoTime();
            if (authors.isEmpty()) {
                resultQ += parser.parseBooks(format, books, "", 0);
            } else {
                resultQ += parser.parseBooks(format, books, authors.getFirst().getName(), authors.getFirst().getId());
            }
            response.append(resultQ);
            parseTime += duration(startParse);
        }

        String result = response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }


    @GetMapping("/Books/{format}/AuthorName/{Author_Name}/")
    public ResponseEntity<byte[]> getBooksByAuthorName(@PathVariable String format,@PathVariable("Author_Name") String Author_Name,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Books/AuthorName/";
        String params = String.format("Author_Name=%s",Author_Name);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(Author_Name)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        long dbTime = System.nanoTime();
        List<Author> authors = authorRepository.getAuthorsParamether(null,Author_Name,null,null);
        dbTime = duration(dbTime);
        if (authors.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder response = new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            response.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }
        long parseTime=0;
        for (Author author : authors){
            long dbTime2=System.nanoTime();
            List<Integer> bookIDs = bookAuthorRepository.getBooksIDbyAuthor(author.getId());
            List<Book> books = bookRepository.getBooksByIDS(bookIDs);
            dbTime=duration(dbTime2);
            long parseTime2=System.nanoTime();
            for (Book book : books){
                response.append(parser.parseBooksV2(format,book,author));
            }
            parseTime+=duration(parseTime2);
        }

        String result = response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Books/{format}/AuthorID/{Author_ID}/")
    public ResponseEntity<byte[]> getBooksByAuthorID(@PathVariable String format,@PathVariable("Author_ID") Integer Author_ID,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Books/AuthorID/";
        String params = String.format("AuthorID=%s",Author_ID);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(Author_ID)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        long dbTime = System.nanoTime();
        List<Author> authors = authorRepository.getAuthorsParamether(Author_ID,null,null,null);
        dbTime = duration(dbTime);
        if (authors.isEmpty()){
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format, download);
        }

        StringBuilder response = new StringBuilder();
        if (format.equalsIgnoreCase("CSV")){
            response.append("ID_Book,bookTitle,ISBN,Sinopse,ID_Author,authorName\n");
        }
        long parseTime=0;
        for (Author author : authors){
            long dbTime2=System.nanoTime();
            List<Integer> booksIds=bookAuthorRepository.getBooksIDbyAuthor(author.getId());
            List<Book> books=bookRepository.getBooksByIDS(booksIds);
            dbTime+=duration(dbTime2);
            long parseTime2=System.nanoTime();
            for (Book book : books){
                response.append(parser.parseBooksV2(format, book,author));
            }
            parseTime+=duration(parseTime2);
        }

        String result = response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/Books/{format}/BookName/{Book_Name}/")
    public ResponseEntity<byte[]> getAuthorsByBookName(@PathVariable String format,@PathVariable("Book_Name") String Book_Name,@RequestParam(defaultValue = "false") boolean download) throws JsonProcessingException {
        String endpoint = "/Authors/Book_Name/";
        String params = String.format("Book_Name=%s",Book_Name);
        long startTime = System.nanoTime();
        long startMem = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpu = threadBean.getCurrentThreadCpuTime();

        if (allNull(Book_Name)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, download, "Provide at least one parameter");
        }

        long dbTime = System.nanoTime();
        List<Book> books = bookRepository.getBooksbyParamether(null,Book_Name,null,null);
        dbTime = duration(dbTime);
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
           long dbTime2=System.nanoTime();
            Integer authorID= bookAuthorRepository.getAuthorIDByBook(book.getid());
            List<Author> authors = authorRepository.getAuthorsParamether(authorID,null,null,null);
            Author author=null;
            if (!authors.isEmpty()) {
                author=authors.getFirst();
            }
           dbTime+=duration(dbTime2);
           long parseTime2=System.nanoTime();
           response.append(parser.parseBooksV2(format, book,author));
           parseTime+=duration(parseTime2);
        }

        String result = response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
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

        long dbTime = System.nanoTime();
        List<Book> books = bookRepository.getBooksbyParamether(Book_ID,null,null,null);
        dbTime = duration(dbTime);
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
            long dbTime2=System.nanoTime();
            Integer authorID= bookAuthorRepository.getAuthorIDByBook(book.getid());
            List<Author> authors = authorRepository.getAuthorsParamether(authorID,null,null,null);
            Author author=null;
            if (!authors.isEmpty()) {
                author=authors.getFirst();
            }
            dbTime+=duration(dbTime2);
            long parseTime2=System.nanoTime();
            response.append(parser.parseBooksV2(format, book,author));
            parseTime+=duration(parseTime2);
        }

        String result = response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format, download, result.getBytes(StandardCharsets.UTF_8));
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

        long dbTime = System.nanoTime();
        List<Reservation> reservations = reservationRepository.getReservationParamether(ID_Reservation, null, null, null);
        dbTime= duration(dbTime);
        if (reservations.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return  emptyResponse(format,false);
        }

        Reservation reservation = reservations.getFirst();

        long dbtime2=System.nanoTime();
        List<Library> libraries = libraryRepository.getLibrariesParamether(reservation.getIdLibrary(), null, null, null);
        List<Client> clients = clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
        List<Book> books = bookRepository.getBooksbyParamether(reservation.getIdBook(), null, null, null);
        Integer authorId = bookAuthorRepository.getAuthorIDByBook(books.getFirst().getid());
        List<Integer> authorIds = Collections.singletonList(authorId);
        List<Author> authors = authorRepository.getAuthorsByIDs(authorIds);
        dbTime += duration(dbtime2);

        long parseTime=System.nanoTime();
        StringBuilder response= new StringBuilder();
        response.append(parser.parseReservationsV2(format,reservation,libraries,clients,books,authors));
        parseTime=duration(parseTime);

        String result = response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format,false,result.getBytes(StandardCharsets.UTF_8));
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

        long dbTime = System.nanoTime();
        List<Client> clients = clientRepository.getClientsParamether(null,ClientName, null, null, null);
        dbTime= duration(dbTime);

        if (clients.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return emptyResponse(format,false);
        }


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

        StringBuilder response= new StringBuilder();
        long parseTime = 0;
        if (format.equals("json")) {
            response.append("{");
        }
        for (Reservation reservation : reservations) {
            long dbtime3=System.nanoTime();
            List<Library> libraries = libraryRepository.getLibrariesParamether(reservation.getIdLibrary(), null, null, null);
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

        if (allNull(BookTitle)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        long dbTime = System.nanoTime();
        List<Book> books = bookRepository.getBooksbyParamether(null,null,BookTitle,null);
        dbTime=duration(dbTime);
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
            List<Client> client = clientRepository.getClientsParamether(reservation.getIdClients(), null, null, null, null);
            dbTime += duration(dbtime3);

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

        if (allNull(BookTitle)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        StringBuilder response= new StringBuilder();
        if (format.equalsIgnoreCase("csv")) {
            response.append("ID_Book,ISBN,bookTitle,Library_ID,Library_Name,Stock\n");
        }

        long dbTime = System.nanoTime();
        List<Book> books = bookRepository.getBooksbyParamether(null,null,BookTitle,null);
        dbTime=duration(dbTime);
        if (books.isEmpty()) {
            makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,0,204,0);
            return  emptyResponse(format,false);
        }

        long parseTime=0;

        for (Book book: books) {
            long dbTime2=System.nanoTime();
            List<Stock> Stocks= stockRepository.getStockByParamether(book.getid(),null);
            dbTime += duration(dbTime2);
            for (Stock s: Stocks) {
                dbTime2=System.nanoTime();
                List<Library> libraries= libraryRepository.getLibrariesParamether(s.getID_Library(),null,null,null);
                dbTime += duration(dbTime2);
                for (Library lib: libraries) {
                    long parseTime2=System.nanoTime();
                    response.append(Parser.parseStockV2(format,book,s,lib));
                    parseTime+=duration(parseTime2);
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
        String endpoint = "/Stocks/Library/";
        String params = String.format("BookTitle=%s",Library);
        if (allNull(Library)) {
            makeLog(endpoint,params,startTime,startMem,startCpu,0,0,400,0);
            return badRequest(format, false, "Provide at least one parameter");
        }

        StringBuilder response= new StringBuilder();
        if (format.equalsIgnoreCase("csv")) {
            response.append("ID_Book,ISBN,bookTitle,Library_ID,Library_Name,Stock\n");
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
                    response.append(Parser.parseStockV2(format,book,s,library));
                    parseTime+=duration(parseTime2);
                }
            }
        }

        String result=response.toString();
        makeLog(endpoint,params,startTime,startMem,startCpu,dbTime,parseTime,200,result.length());
        return returnResponse(format,false,result.getBytes(StandardCharsets.UTF_8));
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

}
