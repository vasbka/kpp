package entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dialog {
    private int id;
    private List<User> users;
    private List<String> dialog;

    public Dialog() {
        users = new ArrayList<>();
        dialog = new ArrayList<>();
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public void setDialog(List<String> dialog) {
        this.dialog = dialog;
    }

    public int getId() {
        return id;
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void addMessage(String message) {
        dialog.add(message);
    }

    public boolean userExistInDialog(User user) {
        System.out.println("USER : " + user);
        System.out.println("USERS : " + users);
        return users.contains(user);
    }

    public List<String> getDialog() {
        return dialog;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dialog dialog1 = (Dialog) o;
        return id == dialog1.id &&
                Objects.equals(users, dialog1.users) &&
                Objects.equals(dialog, dialog1.dialog);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, users, dialog);
    }

    @Override
    public String toString() {
        return "Dialog{" +
                "id=" + id +
                ", users=" + users +
                ", dialog=" + dialog +
                '}';
    }
}
