import java.util.ArrayList;

public class User {
    String username;
    String password;
    ArrayList<Result> results;

    User(String username, String password, ArrayList<Result> results) {
        this.username = username;
        this.password = password;
        this.results = results;
    }
}