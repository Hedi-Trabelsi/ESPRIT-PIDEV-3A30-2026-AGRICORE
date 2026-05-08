package services;

import Model.Commande;
import Model.Equipement;
import Model.LigneCommande;
import Model.Panier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory cart, per agriculteur. Mirrors the design of Symfony's
 * {@code App\Service\CartService} which keeps the cart in the user
 * session — the cart is intentionally NOT shared across machines or
 * apps. The only data that crosses Java ↔ Symfony for the order flow
 * is the resulting {@code commande} + {@code ligne_commande} rows
 * created on checkout.
 */
public final class CartMemoire {

    private static final CartMemoire INSTANCE = new CartMemoire();

    public static CartMemoire getInstance() {
        return INSTANCE;
    }

    /** userId → ( equipementId → quantity ) — preserves insertion order. */
    private final Map<Integer, LinkedHashMap<Integer, Integer>> carts = new HashMap<>();

    private CartMemoire() {}

    private synchronized LinkedHashMap<Integer, Integer> cartFor(int userId) {
        return carts.computeIfAbsent(userId, k -> new LinkedHashMap<>());
    }

    public synchronized void add(int userId, int equipementId, int quantity) {
        if (quantity <= 0) return;
        LinkedHashMap<Integer, Integer> cart = cartFor(userId);
        cart.merge(equipementId, quantity, Integer::sum);
    }

    public synchronized void setQuantity(int userId, int equipementId, int quantity) {
        LinkedHashMap<Integer, Integer> cart = cartFor(userId);
        if (quantity <= 0) cart.remove(equipementId);
        else cart.put(equipementId, quantity);
    }

    public synchronized void remove(int userId, int equipementId) {
        LinkedHashMap<Integer, Integer> cart = cartFor(userId);
        cart.remove(equipementId);
    }

    public synchronized void clear(int userId) {
        carts.remove(userId);
    }

    public synchronized Map<Integer, Integer> getCart(int userId) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(cartFor(userId)));
    }

    public synchronized int totalItems(int userId) {
        return cartFor(userId).values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Convenience for UI code that expects a {@link Panier}-shaped DTO list.
     * The {@code id_panier} field is left at 0 (no DB row) and {@code total}
     * is computed against the live unit price of each equipement.
     */
    public List<Panier> snapshotForUi(int userId, EquipementService es) throws SQLException {
        Map<Integer, Integer> cart = getCart(userId);
        List<Panier> rows = new ArrayList<>(cart.size());
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            Equipement eq = es.findById(e.getKey());
            if (eq == null) continue;
            BigDecimal lineTotal = parsePrix(eq.getPrix())
                    .multiply(BigDecimal.valueOf(e.getValue()))
                    .setScale(2, RoundingMode.HALF_UP);
            Panier p = new Panier();
            p.setId_equipement(e.getKey());
            p.setQuantite(e.getValue());
            p.setTotal(lineTotal.toPlainString());
            p.setId_agriculteur(userId);
            rows.add(p);
        }
        return rows;
    }

    /**
     * Convert the in-memory cart into a {@code commande} + {@code ligne_commande}
     * rows, decrement equipement stock, and clear the cart. Returns the new
     * commande id, or 0 if the cart was empty.
     */
    public int checkout(int userId) throws SQLException {
        Map<Integer, Integer> cart;
        synchronized (this) {
            cart = new LinkedHashMap<>(cartFor(userId));
        }
        if (cart.isEmpty()) return 0;

        EquipementService es = new EquipementService();
        CommandeService cs = new CommandeService();
        LigneCommandeService ls = new LigneCommandeService();

        BigDecimal total = BigDecimal.ZERO;
        List<LigneCommande> lignes = new ArrayList<>();

        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            Equipement eq = es.findById(e.getKey());
            if (eq == null) continue;
            BigDecimal prixUnit = parsePrix(eq.getPrix());
            BigDecimal lineTotal = prixUnit.multiply(BigDecimal.valueOf(e.getValue()))
                    .setScale(2, RoundingMode.HALF_UP);
            lignes.add(new LigneCommande(0, eq.getId_equipement(), e.getValue(),
                    prixUnit, lineTotal));
            total = total.add(lineTotal);
        }

        if (lignes.isEmpty()) return 0;

        Commande commande = new Commande(userId, total.setScale(2, RoundingMode.HALF_UP));
        int commandeId = cs.ajouter(commande);
        for (LigneCommande l : lignes) {
            l.setCommandeId(commandeId);
            ls.ajouter(l);
            es.decrementerStock(l.getEquipementId(), l.getQuantite());
        }

        clear(userId);
        return commandeId;
    }

    private static BigDecimal parsePrix(String prix) {
        if (prix == null || prix.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(prix.replace(",", ".").trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
