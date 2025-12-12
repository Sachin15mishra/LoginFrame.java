package VehicleIdentificationSystem;

import java.io.*;
import java.util.ArrayList;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    String username;
    String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    // ---- Storage methods ----
    private static final String FILE = "users.dat";

    @SuppressWarnings("unchecked")
    public static ArrayList<User> loadUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE))) {
            return (ArrayList<User>) ois.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void saveUsers(ArrayList<User> list) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE))) {
            oos.writeObject(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
