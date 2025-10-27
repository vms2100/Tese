package API;

import Tables.*;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Parser{
    static String res;

    public String parseBooks(String format, List<Book> books, String AuthorName,Integer Author_id) throws JsonProcessingException {
        switch(format.toLowerCase()){
            case "json":
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> wrappedBooks = new ArrayList<>();
                for (Book book : books) {
                    Map<String, Object> bookContent = new HashMap<>();
                    bookContent.put("ID_Book", book.getid());
                    bookContent.put("ISBN", book.getIsbn());
                    bookContent.put("Sinopse", book.getSinopse());
                    bookContent.put("Author_Name",AuthorName);
                    bookContent.put("Author_ID",Author_id);
                    Map<String, Object> bookMap = new HashMap<>();
                    bookMap.put("Title:" + book.getTitle(), bookContent);
                    wrappedBooks.add(bookMap);
                }

                res= mapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrappedBooks);
                break;
            case "csv":
                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append("ID Book,ISBN,Author_Name,Author_ID,Title,Sinopse").append(System.lineSeparator());
                for (Book book : books) {
                    csvBuilder.append(book.getID_Book()).append(",");
                    csvBuilder.append("\"").append(book.getIsbn().replace("\"", "\"\"")).append("\",");
                    csvBuilder.append("\"").append(AuthorName.replace("\"", "\"\"")).append("\",");
                    csvBuilder.append("\"").append(Author_id).append("\",");
                    csvBuilder.append("\"").append(book.getTitle().replace("\"", "\"\"")).append("\",");
                    csvBuilder.append("\"").append(book.getSinopse().replace("\"", "\"\"")).append("\"\n");
                }
                res = csvBuilder.toString();
                break;
            default:
                res="Wrong format";
                break;
        }
        return res;
    }

    public void parserAllBooks(String format, ResultSet rs, Writer writer) throws SQLException, IOException {
        switch (format.toLowerCase()) {
            case "json":
                ObjectMapper mapper = new ObjectMapper();
                Book book = new Book();
                 book.setID_Book(rs.getInt("ID_Book"));
                 book.setIsbn(rs.getString("ISBN"));
                 book.setTitle(rs.getString("Title"));
                 book.setSinopse(rs.getString("Sinopse"));
                String json = mapper.writeValueAsString(book);
                writer.write(json + "\n");
                break;

            case "csv":
                int id = rs.getInt("ID_Book");
                String isbn = rs.getString("ISBN");
                String title = rs.getString("Title");
                String sinopse = rs.getString("Sinopse");
                writer.write(String.format("%d,%s,%s,%s%n", id, isbn, title, sinopse));
                break;

            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }


    public String parseAuthors(String format, List<Author> Author) throws JsonProcessingException {
        switch(format.toLowerCase()){
            case "json":
                ObjectMapper mapper = new ObjectMapper();

                List<Map<String, Object>> wrappedAuthors = new ArrayList<>();
                for (Author author : Author) {
                    Map<String, Object> AuthorContent = new HashMap<>();
                    AuthorContent.put("ID Author", author.getId());
                    AuthorContent.put("Name", author.getName());
                    AuthorContent.put("Age", author.getAge().toString());
                    AuthorContent.put("Bibliography", author.getBiography());
                    Map<String, Object> AuthorMap = new HashMap<>();
                    AuthorMap.put("Name:" + author.getName(), AuthorContent);
                    wrappedAuthors.add(AuthorMap);
                }

                res= mapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrappedAuthors);
                break;
            case "csv":
                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append("ID Author,Name,Age,Bibliography").append(System.lineSeparator());
                for (Author author : Author) {
                    csvBuilder.append(author.getId()).append(",");
                    csvBuilder.append("\"").append(author.getName().replace("\"", "\"\"")).append("\",");
                    csvBuilder.append("\"").append(author.getAge()).append("\",");
                    csvBuilder.append("\"").append(author.getBiography().replace("\"", "\"\"")).append("\"\n");
                }
                res = csvBuilder.toString();
                break;
            default:
                res="Wrong format";
                break;
        }
        return res;
    }


    public String parseLibrarys(String format, List<Library> Libraries) throws JsonProcessingException {
        switch(format.toLowerCase()){
            case "json":
                res=parseLibrarysjson(Libraries);
                break;
            case "csv":
                res=parseLibraryscsv(Libraries);
                break;
            default:
                res="Wrong format";
                break;
        }
        return res;

    }


    public String parseReservations(String format, List<Reservation> reservations) throws JsonProcessingException {
        switch(format.toLowerCase()){
            case "json":
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Map<String, Object>> result = new LinkedHashMap<>();
                for (Reservation reservation: reservations) {
                    Map<String, Object> reservationContent = new LinkedHashMap<>();
                    reservationContent.put("ID_Library", reservation.getIdLibrary());
                    reservationContent.put("ID_Book", reservation.getIdBook());
                    reservationContent.put("ID_Clients", reservation.getIdClients());
                    result.put("ID_Reservation:" + reservation.getId(), reservationContent);
                }
                res=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                break;
            case "csv":
                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append("ID_Reservation,ID_Library,ID_Book,ID_Clients").append(System.lineSeparator());
                for (Reservation reservation : reservations) {
                    csvBuilder.append(reservation.getId()).append(",")
                            .append(reservation.getIdLibrary()).append(",")
                            .append(reservation.getIdBook()).append(",")
                            .append(reservation.getIdClients()).append(",").append("\n");
                }
                res = csvBuilder.toString();
                break;
            default:
                res="Wrong format";
                break;
        }
        return res;
    }



    public void parserAllLibraries(String format, ResultSet rs, Writer writer) throws SQLException, IOException {
        switch (format.toLowerCase()) {
            case "json":
                // Para JSON, escrevemos o objeto da biblioteca como um JSON simples
                writer.write("{");
                writer.write("\"ID_Library\": " + rs.getInt("ID_Library") + ",");
                writer.write("\"Name\": \"" + rs.getString("Name").replace("\"", "\\\"") + "\",");
                writer.write("\"Description\": \"" + rs.getString("Description").replace("\"", "\\\"") + "\",");
                writer.write("\"Location\": \"" + rs.getString("Location").replace("\"", "\\\"") + "\"");
                writer.write("}");
                break;

            case "csv":
                // Para CSV, formatamos a linha como CSV (sem necessidade de criar objetos)
                int id = rs.getInt("ID_Library");
                String name = rs.getString("Name");
                String description = rs.getString("Description");
                String location = rs.getString("Location");
                writer.write(String.format("%d,%s,%s,%s%n", id,
                        name.replace("\"", "\"\""),
                        description.replace("\"", "\"\""),
                        location.replace("\"", "\"\"")));
                break;

            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }


    public String parseClients(String format, List<Client> Clients) throws JsonProcessingException {
        switch(format.toLowerCase()){
            case "json":
                res=parseClientsJson(Clients);
                break;
            case "csv":
                res=parseClientscsv(Clients);
                break;
            default:
                res="Wrong format";
                break;
        }
        return res;
    }


    public String parseClientscsv(List<Client> clients){
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("ID_Client,Name,Phone,Email,Adress").append(System.lineSeparator());
        for (Client client : clients) {
            csvBuilder.append(client.getId()).append(",")
                    .append(client.getName()).append(",")
                    .append(client.getPhone())
                    .append(",")
                    .append(client.getEmail()).append("\n");
        }
        res = csvBuilder.toString();
        return res;
    }

    public String parseClientsJson(List<Client> clients) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        for (Client client : clients) {
            Map<String, Object> clientContent = new LinkedHashMap<>();
            clientContent.put("Name", client.getName());
            clientContent.put("Phone", client.getPhone());
            clientContent.put("Email", client.getEmail());
            clientContent.put("Adress", client.getAdress());

            result.put("ID_Client:" + client.getId(), clientContent);
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }


    public String parseLibrarysjson(List<Library> Libraries) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        List<Map<String, Object>> wrappedLibrary = new ArrayList<>();
        for (Library library : Libraries) {
            Map<String, Object> LibraryContent = new HashMap<>();
            LibraryContent.put("Name", library.getName());
            LibraryContent.put("Description", library.getDescription());
            LibraryContent.put("Location", library.getLocation());
            Map<String, Object> LibraryMap = new HashMap<>();
            LibraryMap.put("ID_Library:" + library.getId(), LibraryContent);
            wrappedLibrary.add(LibraryMap);
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrappedLibrary);
    }

    public String parseLibraryscsv(List<Library> Libraries){
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("ID Library,Name,Description,Location").append(System.lineSeparator());
        for (Library library : Libraries) {
            csvBuilder.append(library.getId()).append(",");
            csvBuilder.append("\"").append(library.getName().replace("\"", "\"\"")).append("\",");
            csvBuilder.append("\"").append(library.getDescription().replace("\"", "\"\"")).append("\",");
            csvBuilder.append("\"").append(library.getLocation().replace("\"", "\"\"")).append("\"\n");
        }
        res = csvBuilder.toString();
        return res;
    }

    public String parseReservationsV2(String format,Reservation reservation, List<Library> libraries,List<Client> clients,List<Book> books,List<Author> authors) throws JsonProcessingException {
        switch(format.toLowerCase()) {
            case "json":
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> info = new HashMap<>();

                info.put("reservationId", reservation.getId());
                info.put("libraryName", libraries.isEmpty() ? "" : libraries.getFirst().getName());
                info.put("clientName", clients.isEmpty() ? "" : clients.getFirst().getName());
                info.put("clientPhone", clients.isEmpty() ? "" : clients.getFirst().getPhone());
                info.put("bookTitle", books.isEmpty() ? "" : books.getFirst().getTitle());
                info.put("authorName", authors.isEmpty() ? "" : authors.getFirst().getName());
                info.put("ID_Library",libraries.isEmpty()? "" : libraries.getFirst().getId());
                info.put("ID_Client",clients.isEmpty()? "" : clients.getFirst().getId());
                info.put("ID_Author",authors.isEmpty()? "" : authors.getFirst().getId());
                info.put("ID_Book",books.isEmpty()? "": books.getFirst().getid());
                info.put("Client_Email", clients.isEmpty()? "" : clients.getFirst().getEmail());

                res = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(info);
                break;

            case "csv":
                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append("reservationId,libraryName,clientName,clientPhone,bookTitle,authorName,ID_Library,ID_Client,ID_Author,ID_Book,Client_Email").append(System.lineSeparator());
                csvBuilder.append(reservation.getId()).append(",");
                csvBuilder.append(libraries.isEmpty() ? "" : libraries.getFirst().getName()).append(",");
                csvBuilder.append(clients.isEmpty() ? "" : clients.getFirst().getName()).append(",");
                csvBuilder.append(clients.isEmpty() ? "" : clients.getFirst().getPhone()).append(",");
                csvBuilder.append(books.isEmpty() ? "" : books.getFirst().getTitle()).append(",");
                csvBuilder.append(authors.isEmpty() ? "" : authors.getFirst().getName()).append(",");
                csvBuilder.append(libraries.isEmpty() ? "" : libraries.getFirst().getId()).append(",");
                csvBuilder.append(clients.isEmpty() ? "" : clients.getFirst().getId()).append(",");
                csvBuilder.append(authors.isEmpty() ? "" : authors.getFirst().getId()).append(",");
                csvBuilder.append(books.isEmpty() ? "" : books.getFirst().getid()).append(",");
                csvBuilder.append(clients.isEmpty() ? "" : clients.getFirst().getEmail());
                res= csvBuilder.toString();
                break;

        }
        return res;
    }

    public String parseBooksV2(String format,Book book,Author author) throws JsonProcessingException {
        //case where we dont have author
        if (author==null){
            author = new Author();
            author.setName("");
            author.setId(0);
        }
        switch(format.toLowerCase()) {
            case "json":
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> info = new HashMap<>();
                info.put("ID_Book", book.getid());
                info.put("ISBN", book.getIsbn());
                info.put("Sinopse", book.getSinopse());
                info.put("Author_Name",author.getName());
                info.put("Author_ID",author.getId());

                res = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(info);
                break;

            case "csv":
                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append(book.getID_Book()).append(",");
                csvBuilder.append(book.getTitle()).append(",");
                csvBuilder.append(book.getIsbn()).append(",");
                csvBuilder.append(book.getSinopse()).append(",");
                csvBuilder.append(author.getId()).append(",");
                csvBuilder.append(author.getName()).append(",").append(System.lineSeparator());
                res= csvBuilder.toString();
                break;
        }
        return res;
    }


    public static String parseStockV2(String format, Book book, Stock stock, Library library) throws JsonProcessingException {
        switch(format.toLowerCase()) {
            case "json":
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> info = new HashMap<>();
                info.put("ID_Book", book.getid());
                info.put("ISBN", book.getIsbn());
                info.put("Title",book.getTitle());
                info.put("ID_Library",library.getId());
                info.put("Library_Name",library.getName());
                info.put("Stock",stock.getQty());
                res = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(info);
                break;

            case "csv":
                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append(book.getID_Book()).append(",");
                csvBuilder.append(book.getIsbn()).append(",");
                csvBuilder.append(book.getTitle()).append(",");
                csvBuilder.append(library.getId()).append(",");
                csvBuilder.append(library.getName()).append(",");
                csvBuilder.append(stock.getQty()).append(System.lineSeparator());
                res= csvBuilder.toString();
                break;
        }
        return res;
    }

}
