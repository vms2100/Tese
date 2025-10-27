package Repository;
import Tables.Stock;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class StockRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public StockRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }
    public List<Stock> getStockByParamether(Integer ID_Book, Integer ID_Library) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder query = new StringBuilder("SELECT \"ID_Library\", \"ID_Book\", \"Qty\" FROM \"Stock\"");

        List<String> conditions = new ArrayList<>();

        if (ID_Book != null) {
            conditions.add("\"ID_Book\" = :IDBook");
            params.addValue("IDBook", ID_Book);
        }
        if (ID_Library != null) {
            conditions.add("\"ID_Library\" = :IDLibrary");
            params.addValue("IDLibrary", ID_Library);
        }

        if (!conditions.isEmpty()) {
            query.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        return namedParameterJdbcTemplate.query(query.toString(), params, (rs, rowNum) -> mapRow(rs));
    }


    private Stock mapRow(ResultSet rs) throws SQLException {
        Stock stock = new Stock();
        stock.setIdLibrary(rs.getInt("ID_Library"));
        stock.setIdBook(rs.getInt("ID_Book"));
        stock.setQty(rs.getInt("Qty"));
        return stock;
    }
}
