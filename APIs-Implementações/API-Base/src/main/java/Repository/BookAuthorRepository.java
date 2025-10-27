package Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class BookAuthorRepository {
    private final JdbcTemplate jdbcTemplate;

    public BookAuthorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    public List<Integer> getBooksIDbyAuthor(Integer AuthorID) {

        String query = "SELECT \"ID_Book\" FROM \"Book_Author\" WHERE \"ID_Author\" =:AuthorID";

        Map<String, Object> params = new HashMap<>();
        params.put("AuthorID", AuthorID);

        return namedParameterJdbcTemplate.query(
                query,
                params,
                (rs, rowNum) -> rs.getInt("ID_Book")
        );
    }



    public Integer getAuthorIDByBook(Integer BookID) {
        String query = "SELECT \"ID_Author\" FROM \"Book_Author\" WHERE \"ID_Book\" =:BookID";

        Map<String, Object> params = new HashMap<>();
        params.put("BookID", BookID);

        List<Integer> authors = namedParameterJdbcTemplate.query(
                query,
                params,
                (rs, rowNum) -> rs.getInt("ID_Author")
        );

        return authors.isEmpty() ? 0 : authors.getFirst();
    }
}