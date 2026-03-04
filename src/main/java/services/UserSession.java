package services;

public class UserSession {
    private static UserSession instance;
    private int userId;
    private String userName;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    // Call this in your Login controller: UserSession.getInstance().initSession(id, name);
    public void initSession(int id, String name) {
        this.userId = id;
        this.userName = name;
    }

    public void cleanSession() {
        userId = 0;
        userName = null;
    }

    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
}