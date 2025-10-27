package Repository;

import Tables.Reservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReservationRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<Reservation> getReservationParamether(Integer ID_Reservations, Integer ID_Library, Integer ID_Book, Integer ID_Client) {
        StringBuilder query = new StringBuilder("SELECT * FROM \"Reservations\" ");
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        if (ID_Reservations != null) {
            conditions.add("\"ID_Reservation\" = :idReservation");
            params.addValue("idReservation", ID_Reservations);
        }
        if (ID_Library != null) {
            conditions.add("\"ID_Library\" = :idLibrary");
            params.addValue("idLibrary", ID_Library);
        }
        if (ID_Book != null) {
            conditions.add("\"ID_Book\" = :idBook");
            params.addValue("idBook", ID_Book);
        }
        if (ID_Client != null) {
            conditions.add("\"ID_Clients\" = :idClient");
            params.addValue("idClient", ID_Client);
        }

        if (!conditions.isEmpty()) {
            query.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }

        query.append("ORDER BY \"ID_Clients\" ASC");

        return namedParameterJdbcTemplate.query(query.toString(), params, (rs, rowNum) -> mapRow(rs));
    }

    public Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getInt("ID_Reservation"));
        reservation.setIdLibrary(rs.getInt("ID_Library"));
        reservation.setIdBook(rs.getInt("ID_Book"));
        reservation.setIdClients(rs.getInt("ID_Clients"));
        return reservation;
    }
}
