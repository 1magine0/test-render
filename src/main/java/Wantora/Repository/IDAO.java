package Wantora.Repository;

import Wantora.Models.*;

import java.util.List;

public interface IDAO {
    /*public List<User> getUsers();*/

    public User getUserByEmail(String email);
    public Boolean createUser(User user);
    public void updateUser(User user);
    public String hashPassword(String password);
    public void updatePfp(User user);
    public void deleteUser(User user);
}