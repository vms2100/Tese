package Repository;

import Tables.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

@Repository
public class BookRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final DataSource dataSource;

    @Autowired
    public BookRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public JdbcTemplate getStreamingJdbcTemplate() {
        try {
            Connection conn = DataSourceUtils.getConnection(dataSource);
            conn.setAutoCommit(false);
            JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(conn, true));
            jdbc.setFetchSize(1000);
            return jdbc;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao configurar JdbcTemplate para streaming", e);
        }
    }

    public List<Book> getBooksbyParamether(Integer ID_Book, String ISBN, String Title, String Sinopse) {
        StringBuilder query = new StringBuilder("SELECT * FROM \"Book\" ");
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();

        if (ID_Book != null) {
            conditions.add("\"ID_Book\" = :ID_Book");
            params.put("ID_Book", ID_Book);
        }
        if (ISBN != null && !ISBN.isBlank()) {
            conditions.add("\"ISBN\" = :ISBN");
            params.put("ISBN", ISBN);
        }
        if (Title != null && !Title.isBlank()) {
            conditions.add("\"Title\" ILIKE :Title");
            params.put("Title", "%" + Title + "%");
        }
        if (Sinopse != null && !Sinopse.isBlank()) {
            conditions.add("\"Sinopse\" ILIKE :Sinopse");
            params.put("Sinopse", "%" + Sinopse + "%");
        }

        if (!conditions.isEmpty()) {
            query.append("WHERE ").append(String.join(" AND ", conditions));
        }

        query.append(" ORDER BY \"ID_Book\" ASC;");

        return namedParameterJdbcTemplate.query(query.toString(), params, (rs, rowNum) -> mapRow(rs));
    }

    public void getAllBooks(Consumer<ResultSet> rowHandler) {
        StringBuilder query = new StringBuilder("SELECT * FROM \"Book\" ORDER BY \"ID_Book\" ASC");
        JdbcTemplate jdbc = getStreamingJdbcTemplate();

        jdbc.query(query.toString(), (ResultSet rs) -> {
            while (rs.next()) {
                rowHandler.accept(rs);
            }
        });
    }

    public List<Book> getBooksByIDS(List<Integer> ID_Books) {
        if (ID_Books == null || ID_Books.isEmpty()) {
            return Collections.emptyList();
        }
        String query = "SELECT * FROM \"Book\" WHERE \"ID_Book\" IN (:ID_Books) ORDER BY \"ID_Book\" ASC";
        Map<String, Object> params = new HashMap<>();
        params.put("ID_Books", ID_Books);
        return namedParameterJdbcTemplate.query(query, params, (rs, rowNum) -> mapRow(rs));
    }

    public Book mapRow(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setID_Book(rs.getInt("ID_Book"));
        book.setIsbn(rs.getString("ISBN"));
        book.setTitle(rs.getString("Title"));
        book.setSinopse(rs.getString("Sinopse"));
        return book;
    }
}
