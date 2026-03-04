package services;

import Model.ActionLog;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogService {
    private Connection connection;

    public LogService() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void create(ActionLog log) throws SQLException {
        String query = "INSERT INTO action_logs (action_type, target_table, target_id, description) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, log.getActionType());
            ps.setString(2, log.getTargetTable());
            ps.setInt(3, log.getTargetId());
            ps.setString(4, log.getDescription());
            ps.executeUpdate();
        }
    }

    public List<ActionLog> readAll() throws SQLException {
        List<ActionLog> list = new ArrayList<>();
        // Fetching latest actions first
        String query = "SELECT * FROM action_logs ORDER BY id DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                ActionLog log = new ActionLog(rs.getString("action_type"), rs.getString("target_table"),
                        rs.getInt("target_id"), rs.getString("description"));
                log.setId(rs.getInt("id"));
                log.setCreatedAt(rs.getString("created_at"));
                list.add(log);
            }
        }
        return list;
    }
}