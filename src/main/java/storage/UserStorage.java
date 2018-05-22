package storage;

import entity.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserStorage {
    List<User> userList = Collections.synchronizedList(new ArrayList<>());

    public void add(User user) {
        userList.add(user);
    }

    public void remove(User userForRemove) {
        for (User user : userList) {
            if (user.equals(userForRemove)) {
                userList.remove(user);
            }
        }
    }


}
