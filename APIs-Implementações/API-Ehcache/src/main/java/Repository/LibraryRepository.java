package Repository;

import Tables.Library;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Repository
public class LibraryRepository {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public LibraryRepository(DataSource dataSource, JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.dataSource = dataSource;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.jdbcTemplate = getStreamingJdbcTemplate();
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

    public List<Library> getLibrariesParamether(Integer ID_Library, String Name, String Description, String Location) {
        StringBuilder query = new StringBuilder("SELECT * FROM \"Library\" ");
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        if (ID_Library != null) {
            conditions.add("\"ID_Library\" = :id");
            params.addValue("id", ID_Library);
        }
        if (Name != null && !Name.isBlank()) {
            conditions.add("\"Name\" ILIKE :name");
            params.addValue("name", "%" + Name + "%");
        }
        if (Description != null && !Description.isBlank()) {
            conditions.add("\"Description\" ILIKE :description");
            params.addValue("description", "%" + Description + "%");
        }
        if (Location != null && !Location.isBlank()) {
            conditions.add("\"Location\" ILIKE :location");
            params.addValue("location", "%" + Location + "%");
        }

        if (!conditions.isEmpty()) {
            query.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }

        query.append("ORDER BY \"ID_Library\" ASC");

        return namedParameterJdbcTemplate.query(query.toString(), params, (rs, rowNum) -> mapRow(rs));
    }



    private Library mapRow(ResultSet rs) throws SQLException {
        Library library = new Library();
        library.setId(rs.getInt("ID_Library"));
        library.setName(rs.getString("Name"));
        library.setDescription(rs.getString("Description"));
        library.setLocation(rs.getString("Location"));
        return library;
    }
}
