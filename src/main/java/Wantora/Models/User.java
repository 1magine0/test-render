package Wantora.Models;

import java.util.Objects;

public class User {
    private int id;
    private String email;
    private String fname;
    private String lname;
    private String password;
    private String date;
    private String imageName = "null";

    public User() {}

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(int id, String email, String fname, String lname, String password, String date, String imageName) {
        this.id = id;
        this.email = email;
        this.fname = fname;
        this.lname = lname;
        this.password = password;
        this.date = date;
        this.imageName = imageName;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFname() {
        return fname;
    }

    public String getLname() {
        return lname;
    }

    public String getPassword() {
        return password;
    }

    public String getDate() {
        return date;
    }

    public String getImageName() {
        return imageName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public String toString() {return "User [id=" + id +
            ", email=" + email + ", fname=" + fname + ", password="
            + password + ", date=" + date + ", imageName=" + imageName + "]";}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(email, user.email) &&
                Objects.equals(fname, user.fname) &&
                Objects.equals(lname, user.lname) &&
                Objects.equals(password, user.password) &&
                Objects.equals(date, user.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, fname, lname, password, date);
    }

}
