package services;

import Model.Utilisateur;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-level face-authentication service. Wraps the Face++ FaceSet API and the
 * local {@link FaceRegistry} so the rest of the app only deals in users + bytes.
 *
 * Login is one Face++ /search call. Registration is detect + setuserid + addface.
 * Calls retry internally on Face++'s CONCURRENCY_LIMIT_EXCEEDED, so callers
 * see transient 403s as a slow response rather than a failure.
 */
public final class FaceAuthService {

    private static final String FACESET_OUTER_ID = "agricore_users";
    private static final double LOGIN_CONFIDENCE_THRESHOLD = 80.0;

    /**
     * Sentinel value stored in {@link FaceRegistry} for users whose stored
     * image is unusable (corrupt bytes or no detectable face). Marking them
     * stops the next sync run from re-spending API quota on them.
     */
    private static final String SKIP_SENTINEL = "__SKIP__";

    // Polite pacing between sync registrations. Free-tier Face++ allows ~1 QPS
    // total; postFormOnce already retries on rate-limit, but spacing reduces
    // how often we have to back off.
    private static final long SYNC_DELAY_MS = 1200;

    private static volatile boolean faceSetEnsured = false;
    private static final AtomicBoolean syncStarted = new AtomicBoolean(false);

    private FaceAuthService() {}

    public static synchronized void ensureFaceSet() throws IOException {
        if (faceSetEnsured) return;
        FacePPService.ensureFaceSet(FACESET_OUTER_ID);
        faceSetEnsured = true;
    }

    /**
     * Registers (or re-registers) a user's face. Safe to call repeatedly: any
     * previous token for this user is removed from the FaceSet first. Users
     * whose image can't be decoded or contains no face are marked as skipped
     * so future sync runs ignore them.
     */
    public static void registerUser(int userId, byte[] image) throws IOException {
        if (image == null || image.length == 0) {
            FaceRegistry.put(userId, SKIP_SENTINEL);
            throw new IOException("User has no image to register");
        }
        ensureFaceSet();

        String stale = FaceRegistry.get(userId);
        if (stale != null && !SKIP_SENTINEL.equals(stale)) {
            try {
                FacePPService.removeFaceFromSet(FACESET_OUTER_ID, stale);
            } catch (IOException ignored) {
                // Stale token may already be gone from Face++; the registry was wrong.
            }
        }

        String faceToken;
        try {
            faceToken = FacePPService.detectFace(image);
        } catch (IOException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("could not be decoded") || msg.contains("No face detected")
                    || msg.contains("IMAGE_ERROR_UNSUPPORTED_FORMAT")
                    || msg.contains("INVALID_IMAGE_FACE")) {
                FaceRegistry.put(userId, SKIP_SENTINEL);
            }
            throw e;
        }

        FacePPService.setUserId(faceToken, Integer.toString(userId));
        FacePPService.addFaceToSet(FACESET_OUTER_ID, faceToken);
        FaceRegistry.put(userId, faceToken);
    }

    /** Best-effort cleanup. Never throws. */
    public static void unregisterUser(int userId) {
        String token = FaceRegistry.get(userId);
        if (token == null || SKIP_SENTINEL.equals(token)) {
            FaceRegistry.remove(userId);
            return;
        }
        try {
            FacePPService.removeFaceFromSet(FACESET_OUTER_ID, token);
        } catch (IOException ignored) {
        }
        FaceRegistry.remove(userId);
    }

    /**
     * One Face++ /search call. Returns the matched user id, or -1 if no face
     * cleared the confidence threshold (also returned when the FaceSet is
     * still empty during initial backfill).
     */
    public static int searchUser(byte[] liveImage) throws IOException {
        ensureFaceSet();
        FacePPService.SearchResult r;
        try {
            r = FacePPService.searchInSet(liveImage, FACESET_OUTER_ID);
        } catch (IOException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("EMPTY_FACESET")) {
                return -1;
            }
            throw e;
        }
        if (r == null) return -1;
        if (r.confidence < LOGIN_CONFIDENCE_THRESHOLD) return -1;
        if (r.userId == null || r.userId.isEmpty()) return -1;
        try {
            return Integer.parseInt(r.userId);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Walks the user list and registers anyone with an image who isn't yet in
     * the local registry. Users already marked with {@link #SKIP_SENTINEL}
     * are skipped. Returns how many were successfully registered.
     */
    public static int syncMissingUsers(List<Utilisateur> users) {
        try {
            ensureFaceSet();
        } catch (IOException e) {
            System.err.println("FaceAuthService.syncMissingUsers: ensureFaceSet failed - " + e.getMessage());
            return 0;
        }

        int registered = 0;
        for (Utilisateur u : users) {
            byte[] img = u.getImage();
            if (img == null || img.length == 0) continue;
            if (FaceRegistry.contains(u.getId())) continue;

            try {
                registerUser(u.getId(), img);
                registered++;
                Thread.sleep(SYNC_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("FaceAuthService: sync register failed for user "
                        + u.getEmail() + " - " + e.getMessage());
            }
        }
        return registered;
    }

    /**
     * Kicks off a one-time background sync of all users with photos. Idempotent
     * within a JVM session: subsequent calls are no-ops. Called from the signin
     * screen so that by the time a user attempts face login, registration is
     * either complete or in progress.
     */
    public static void kickoffBackgroundSync() {
        if (!syncStarted.compareAndSet(false, true)) return;

        Thread t = new Thread(() -> {
            try {
                UserService userService = new UserService();
                List<Utilisateur> users = userService.read();
                int registered = syncMissingUsers(users);
                if (registered > 0) {
                    System.out.println("FaceAuthService: background sync registered "
                            + registered + " user(s)");
                }
            } catch (SQLException e) {
                System.err.println("FaceAuthService.kickoffBackgroundSync: " + e.getMessage());
            }
        }, "face-background-sync");
        t.setDaemon(true);
        t.start();
    }
}
