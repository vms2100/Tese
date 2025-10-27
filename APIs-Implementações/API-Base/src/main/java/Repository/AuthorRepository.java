package Repository;

import Tables.Author;
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
public class AuthorRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public AuthorRepository(     NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<Author> getAuthorsParamether(Integer ID_Author, String Name, Integer Age, String Biography) {
        StringBuilder query = new StringBuilder("SELECT * FROM \"Author\" ");
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();

        if (ID_Author != null) {
            conditions.add("\"ID_Author\" = :ID_Author");
            params.put("ID_Author", ID_Author);
        }
        if (Name != null && !Name.isBlank()) {
            conditions.add("\"Name\" ILIKE :Name");
            params.put("Name", "%" + Name + "%");
        }
        if (Age != null) {
            conditions.add("\"Age\" = :Age");
            params.put("Age", Age);
        }
        if (Biography != null && !Biography.isBlank()) {
            conditions.add("\"Biography\" ILIKE :Biography");
            params.put("Biography", "%" + Biography + "%");
        }

        if (!conditions.isEmpty()) {
            query.append("WHERE ").append(String.join(" AND ", conditions));
        }

        query.append(" ORDER BY \"ID_Author\" ASC;");

        return namedParameterJdbcTemplate.query(query.toString(), params, (rs, rowNum) -> mapRow(rs));
    }


    public List<Author> getAuthorsByIDs(List<Integer> ID_Authors) {
        if (ID_Authors == null || ID_Authors.isEmpty()) {
            return Collections.emptyList();
        }
        String query = "SELECT * FROM \"Author\" WHERE \"ID_Author\" IN (:ID_Authors) ORDER BY \"ID_Author\" ASC";
        Map<String, Object> params = new HashMap<>();
        params.put("ID_Authors", ID_Authors);

        return namedParameterJdbcTemplate.query(query, params, (rs, rowNum) -> mapRow(rs));
    }



    public Author mapRow(ResultSet rs) throws SQLException {
        Author author = new Author();
        author.setId(rs.getInt("ID_Author"));
        author.setName(rs.getString("Name"));
        author.setAge(rs.getInt("Age"));
        author.setBiography(rs.getString("Biography"));
        return author;
    }
}