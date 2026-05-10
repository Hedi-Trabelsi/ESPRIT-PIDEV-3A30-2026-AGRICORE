package services;

import utils.MyDatabase;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private Connection connection;
    public static final int ADMIN_SENDER_ID = 0;

    public MessageService() {
        try {
            connection = MyDatabase.getInstance().getConnection();
        } catch (SQLException e) {
            System.err.println("Erreur d'initialisation de la connexion : " + e.getMessage());
        }
    }

    /**
     * Envoie un message en base de données.
     * Retourne l'ID auto-généré du nouveau message (utilisé pour le polling temps réel).
     */
    public int sendMessage(int senderId, String senderName, int eventId, String content) throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = MyDatabase.getInstance().getConnection();
        }

        String query = "INSERT INTO messages (sender_id, sender_name, receiver_id, content, event_id, timestamp) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderId);
            ps.setString(2, senderName);
            ps.setInt(3, 0);
            ps.setString(4, content);
            ps.setInt(5, eventId);

            Timestamp now = Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Africa/Tunis")));
            ps.setTimestamp(6, now);

            ps.executeUpdate();

            // Récupérer l'ID généré automatiquement
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        return -1; // Ne devrait jamais arriver
    }

    /**
     * Récupère l'historique complet des messages d'un événement.
     * Utilise un LEFT JOIN pour obtenir le nom/prénom depuis la table user.
     */
    public List<ChatMessage> getGroupMessages(int eventId) throws SQLException {
        List<ChatMessage> list = new ArrayList<>();

        if (connection == null || connection.isClosed()) {
            connection = MyDatabase.getInstance().getConnection();
        }

        String query = "SELECT m.*, u.nom, u.prenom FROM messages m " +
                "LEFT JOIN user u ON m.sender_id = u.id " +
                "WHERE m.event_id = ? ORDER BY m.timestamp ASC";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(buildChatMessage(rs));
                }
            }
        }
        return list;
    }

    /**
     * Récupère uniquement les messages dont l'ID est supérieur à afterId.
     * Utilisé par le poller temps réel pour ne charger que les nouveaux messages.
     */
    public List<ChatMessage> getNewMessages(int eventId, int afterId) throws SQLException {
        List<ChatMessage> list = new ArrayList<>();

        if (connection == null || connection.isClosed()) {
            connection = MyDatabase.getInstance().getConnection();
        }

        String query = "SELECT m.*, u.nom, u.prenom FROM messages m " +
                "LEFT JOIN user u ON m.sender_id = u.id " +
                "WHERE m.event_id = ? AND m.id > ? ORDER BY m.id ASC";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, eventId);
            ps.setInt(2, afterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(buildChatMessage(rs));
                }
            }
        }
        return list;
    }

    /**
     * Méthode utilitaire partagée entre getGroupMessages et getNewMessages
     * pour construire un ChatMessage depuis un ResultSet.
     */
    private ChatMessage buildChatMessage(ResultSet rs) throws SQLException {
        int msgId = rs.getInt("id");
        int sId = rs.getInt("sender_id");
        String name;

        if (sId == ADMIN_SENDER_ID) {
            name = "👑 Admin";
        } else {
            String prenom = rs.getString("prenom");
            String nom = rs.getString("nom");

            if (prenom != null && nom != null) {
                name = prenom + " " + nom;
            } else {
                name = rs.getString("sender_name");
                if (name == null) name = "Utilisateur inconnu";
            }
        }

        return new ChatMessage(msgId, sId, name, rs.getString("content"));
    }

    // Classe interne pour structurer les données du chat
    public static class ChatMessage {
        public int id;         // ID de la ligne en base — requis pour le polling temps réel
        public int senderId;
        public String senderName;
        public String content;

        public ChatMessage(int id, int senderId, String senderName, String content) {
            this.id = id;
            this.senderId = senderId;
            this.senderName = senderName;
            this.content = content;
        }
    }
}