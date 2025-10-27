package Repository;

import Tables.Client;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ClientRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ClientRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<Client> getClientsParamether(Integer ID_Client, String Name, String Phone, String Email, String Adress) {
        StringBuilder query = new StringBuilder("SELECT * FROM \"Clients\" ");
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        if (ID_Client != null) {
            conditions.add("\"ID_Clients\" = :id");
            params.addValue("id", ID_Client);
        }
        if (Name != null && !Name.isBlank()) {
            conditions.add("\"Name\" ILIKE :name");
            params.addValue("name", "%" + Name + "%");
        }
        if (Phone != null && !Phone.isBlank()) {
            conditions.add("\"Phone\" = :phone");
            params.addValue("phone", Phone);
        }
        if (Email != null && !Email.isBlank()) {
            conditions.add("\"Email\" ILIKE :email");
            params.addValue("email", "%" + Email + "%");
        }
        if (Adress != null && !Adress.isBlank()) {
            conditions.add("\"Adress\" ILIKE :adress");
            params.addValue("adress", "%" + Adress + "%");
        }

        if (!conditions.isEmpty()) {
            query.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }

        query.append("ORDER BY \"ID_Clients\" ASC");

        return namedParameterJdbcTemplate.query(query.toString(), params, (rs, rowNum) -> mapRow(rs));
    }

    private Client mapRow(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setId(rs.getInt("ID_Clients"));
        client.setName(rs.getString("Name"));
        client.setPhone(rs.getString("Phone"));
        client.setEmail(rs.getString("Email"));
        client.setAdress(rs.getString("Adress"));
        return client;
    }
}
