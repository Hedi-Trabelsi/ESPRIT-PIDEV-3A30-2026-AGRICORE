package services;

import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private Connection connection;

    public MessageService() {
        try {
            connection = MyDatabase.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Sauvegarder un message (Ici receiver_id = eventId pour le groupe)
    public void sendMessage(int senderId, int eventId, String content) throws SQLException {
        String query = "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, senderId);
        ps.setInt(2, eventId);
        ps.setString(3, content);
        ps.executeUpdate();
    }

    // Charger l'historique d'un événement
    public List<ChatMessage> getGroupMessages(int eventId) throws SQLException {
        List<ChatMessage> list = new ArrayList<>();
        String query = "SELECT m.*, u.nom, u.prenom FROM messages m " +
                "JOIN user u ON m.sender_id = u.id " +
                "WHERE m.receiver_id = ? ORDER BY m.timestamp ASC";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, eventId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String fullName = rs.getString("prenom") + " " + rs.getString("nom");
            list.add(new ChatMessage(rs.getInt("sender_id"), fullName, rs.getString("content")));
        }
        return list;
    }

    public static class ChatMessage {
        public int senderId;
        public String senderName;
        public String content;
        public ChatMessage(int id, String name, String msg) {
            this.senderId = id; this.senderName = name; this.content = msg;
        }
    }
}