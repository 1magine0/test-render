package Wantora.Repository;

import Wantora.Models.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.lang.*;

public class MySQLDAO implements IDAO {
    private static Connection con = null;
    public MySQLDAO() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://yamanote.proxy.rlwy.net:19636/Wantora";
            String username = "andryusha";
            String password = "parol_andryushi";
            con = DriverManager.getConnection(url, username, password);
            if (con != null) {
                System.out.println("Connected to the database!");
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    /*public List<User> getUsers() {
        List<User> users = new ArrayList<User>();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM Wantora.Users");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = new User
                        (rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6));
                users.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return users;
    }*/

    public User getUserByEmail(String email) {
        User user = null;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT u.*, i.url FROM Wantora.Users u LEFT JOIN Wantora.Images i ON u.image_id = i.id WHERE u.email = ?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                user = new User(rs.getInt(1), email, rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(8));
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean createUser(User user) {
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM Wantora.Users where email = ? LIMIT 1");
            ps.setString(1, user.getEmail());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return false;
            } else {
                ps = con.prepareStatement("INSERT INTO Wantora.Users(email, fname, lname, password, birth_date) VALUES(?,?,?,?,?)");
                ps.setString(1, user.getEmail());
                ps.setString(2, user.getFname());
                ps.setString(3, user.getLname());
                ps.setString(4, hashPassword(user.getPassword()));
                ps.setString(5, user.getDate());
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateUser(User user) {
        try {
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE Wantora.Users SET email = ?, fname = ?, lname = ?, password = ?, birth_date = ? WHERE email = ?");
                ps.setString(1, user.getEmail());
                ps.setString(2, user.getFname());
                ps.setString(3, user.getLname());
                ps.setString(4, user.getPassword());
                ps.setString(5, user.getDate());
                ps.setString(6, user.getEmail());
                ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updatePfp(User user) {
        try {
            User temp = getUserByEmail(user.getEmail());
            PreparedStatement ps;
            if (temp.getImageName() == null) {
                ps = con.prepareStatement("INSERT INTO Wantora.Images (url, type, object_id) VALUES(?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, user.getImageName());
                ps.setString(2, "user");
                ps.setInt(3, temp.getId());
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int imageId = rs.getInt(1);
                    ps = con.prepareStatement("UPDATE Wantora.Users SET image_id = ? WHERE id = ?");
                    ps.setInt(1, imageId);
                    ps.setInt(2, temp.getId());
                    ps.executeUpdate();
                }
            } else {
                ps = con.prepareStatement("UPDATE Wantora.Images SET url = ? WHERE object_id = ?");
                ps.setString(1, user.getImageName());
                ps.setInt(2, temp.getId());
                ps.executeUpdate();
            }
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteUser(User user) {
        try {
            user = getUserByEmail(user.getEmail());
            PreparedStatement ps = con.prepareStatement("DELETE FROM Wantora.Users WHERE id = ?");
            ps.setInt(1, user.getId());
            ps.executeUpdate();
            ps = con.prepareStatement("DELETE FROM Wantora.Images WHERE object_id = ?");
            ps.setInt(1, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

}