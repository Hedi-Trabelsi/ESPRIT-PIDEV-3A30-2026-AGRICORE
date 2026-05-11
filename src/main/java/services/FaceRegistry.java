package services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Persistent map of {@code userId -> face_token} for users registered in the
 * Face++ FaceSet. Stored at {@code <user.home>/.agricore/face_registry.properties}
 * so we don't have to add a column to the existing user table.
 */
public final class FaceRegistry {

    private static final Path FILE = Paths.get(
            System.getProperty("user.home"),
            ".agricore",
            "face_registry.properties"
    );

    private static final Properties PROPS = new Properties();
    private static boolean loaded = false;

    private FaceRegistry() {}

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        try {
            if (Files.exists(FILE)) {
                try (InputStream in = Files.newInputStream(FILE)) {
                    PROPS.load(in);
                }
            }
        } catch (IOException e) {
            System.err.println("FaceRegistry: failed to load " + FILE + " - " + e.getMessage());
        }
        loaded = true;
    }

    public static synchronized String get(int userId) {
        ensureLoaded();
        return PROPS.getProperty(Integer.toString(userId));
    }

    public static synchronized boolean contains(int userId) {
        ensureLoaded();
        return PROPS.containsKey(Integer.toString(userId));
    }

    public static synchronized void put(int userId, String faceToken) {
        ensureLoaded();
        PROPS.setProperty(Integer.toString(userId), faceToken);
        save();
    }

    public static synchronized void remove(int userId) {
        ensureLoaded();
        if (PROPS.remove(Integer.toString(userId)) != null) {
            save();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                PROPS.store(out, "AGRICORE Face++ registry");
            }
        } catch (IOException e) {
            System.err.println("FaceRegistry: failed to save " + FILE + " - " + e.getMessage());
        }
    }
}
