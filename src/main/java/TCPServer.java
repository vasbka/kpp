import com.google.gson.Gson;
import entity.Dialog;
import entity.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

class TCPServer {
    public static void main(String argv[]) throws IOException {

        List<User> users = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger id = new AtomicInteger(0);
        AtomicInteger idDialog = new AtomicInteger(0);
        List<Dialog> dialogs = Collections.synchronizedList(new ArrayList<>());
        new Thread(() -> {
            AtomicReference<String> lastUserName = new AtomicReference<>("");
            ServerSocket welcomeSocket = null;
            try {
                welcomeSocket = new ServerSocket(4000);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                Socket connectionSocket = null;
                try {
                    connectionSocket = welcomeSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Socket obtainedSocket = connectionSocket;
                new Thread(() -> {
                    try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(obtainedSocket.getInputStream()));
                         ObjectOutputStream outToClient = new ObjectOutputStream(obtainedSocket.getOutputStream())) {
                        Gson gson = new Gson();
                        HashMap<String, String> param = gson.fromJson(inFromClient.readLine(), HashMap.class);
                        Map<String, Object> parameters = new HashMap<>();
                        List<String> errors = new ArrayList<>();

                        if (!isNull(param)) {
                            User user = gson.fromJson(param.get("user"), User.class);
                            Iterator<User> userIterator = users.iterator();
                            if (param.get("choice").equals("100")) {
                                if (!lastUserName.get().equals("")) {
                                    outToClient.writeBytes("\r\n" + "User " + lastUserName + " was leaved.\r\n");
                                    lastUserName.set("");
                                    return;
                                }
                                outToClient.writeBytes("\r\n" + "Hello " + users.get(users.size() - 1).getName() + ", welcome to chat.\r\n");
                                return;
                            }
                            int choice = gson.fromJson(param.get("choice"), Integer.class);
                            if (choice == 0) {
                                while (userIterator.hasNext()) {
                                    if (userIterator.next().getName().toLowerCase().equals(user.getName().toLowerCase())) {
                                        errors.add("User with this login already exists.");
                                    }
                                }
                                if (errors.isEmpty()) {
                                    user.setId(id.getAndIncrement());
                                    users.add(user);
                                    parameters.put("user", new Gson().toJson(user, User.class));
                                }
                            }
                            if (choice == 10) {
                                //leave
                                userIterator = users.iterator();
                                while (userIterator.hasNext()) {
                                    if (userIterator.next().getName().equals(user.getName())) {
                                        lastUserName.set(user.getName());
                                        userIterator.remove();
                                        outToClient.writeBytes("\r\n" + "exit" + "\r\n");
                                        return;
                                    }
                                }
                            }

                            //get all user
                            if (choice == 1) {
                                parameters.put("users", gson.toJson(users, List.class));
                                System.out.println("in third");
                                outToClient.writeBytes("\r\n" + gson.toJson(parameters, HashMap.class) + "\r\n");
                                return;
                            }
                            //start dialog
                            if (choice == 2) {
                                int idUserToChat = gson.fromJson(param.get("userToChat"), Integer.class);
                                if (isNull(idUserToChat) || (isNull(users.get(idUserToChat)))) {
                                    errors.add("Dialog can't be creating with partner with this id.");
                                }
                                List<User> usersForDialog = new ArrayList<>();
                                usersForDialog.add(users.get(idUserToChat));
                                usersForDialog.add(user);
                                boolean dialogIsExist = false;
                                for (Dialog dialog : dialogs) {
                                    if (dialog.isDialogExist(usersForDialog)) {
                                        errors.add("The dialog exists.");
                                        dialogIsExist = true;
                                        break;
                                    }

                                }
                                if (!dialogIsExist) {
                                    Dialog newDialog = new Dialog();
                                    newDialog.setId(idDialog.getAndIncrement());
                                    newDialog.addUser(user);
                                    newDialog.addUser(users.get(idUserToChat));
                                    dialogs.add(newDialog);
                                }
                            }
                            //get all dialogs in which user is exist
                            if (choice == 3) {
                                List<Dialog> userDialogs = new ArrayList<>();
                                for (Dialog dialog : dialogs) {
                                    if (dialog.userExistInDialog(user)) {
                                        userDialogs.add(dialog);
                                    }
                                }
                                parameters.put("dialogs", gson.toJson(userDialogs, List.class));
                            }
                            if (choice == 4) {
                                int chatId = gson.fromJson(param.get("chatId"), Integer.class);
                                String message = gson.fromJson(param.get("message"), String.class);
                                for (Dialog dialog : dialogs) {
                                    if (dialog.getId() == chatId && dialog.getUsers().contains(user)) {
                                        parameters.put("dialog", new Gson().toJson(dialog, Dialog.class));
                                        parameters.put("chatId", chatId);
                                        if (nonNull(param.get("option"))) {
                                            int option;
                                            while (true) {
                                                try {
                                                    option = Integer.parseInt(param.get("option"));
                                                    break;
                                                } catch (NumberFormatException e) {
                                                }
                                            }
                                            int userId;
                                            while (true) {
                                                try {
                                                    userId = Integer.parseInt(param.get("userIdForAddedToChat"));
                                                    break;
                                                } catch (NumberFormatException e) {

                                                }
                                            }
                                            for (User usr : users) {
                                                if (usr.getId() == userId) {
                                                    System.out.println("4");
                                                    dialog.getUsers().add(usr);
                                                    System.out.println(dialog.getUsers());
                                                    break;
                                                }
                                            }
                                        }
                                        if (nonNull(message)) {
                                            dialog.addMessage(user.getName() + ": " + message);
                                        }
                                    } else {
                                        errors.add("Sorry but you have not access to this dialog");
                                    }
                                }
                            }
                            if (!errors.isEmpty()) {
                                parameters.put("errors", gson.toJson(errors, List.class));
                                outToClient.writeBytes("\r\n" + gson.toJson(parameters, HashMap.class) + "\r\n");
                                return;
                            }
                        }
                        outToClient.writeBytes("\r\n" + gson.toJson(parameters, HashMap.class) + "\r\n");
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }

                }).start();
            }
        }).start();

        while (true) {
            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
            String choice = bf.readLine();
            String choiceDialog = "";
            if (choice.equals("1")) {
                for (Dialog dialog : dialogs) {
                    for (User user : dialog.getUsers()) {
                        System.out.print(user.getName() + " ");
                    }
                    System.out.print(dialog.getId());
                }

                choiceDialog = bf.readLine();
            }
            String finalChoiceDialog = choiceDialog;
            new Thread(() -> {
                Dialog dialog = dialogs.get(Integer.valueOf(finalChoiceDialog));
                dialog.printDialog();
                int last = dialog.getDialog().get(dialog.getDialog().size() - 1).hashCode();
                while (true) {
                    if (dialogs.get(Integer.valueOf(finalChoiceDialog)).getDialog().get(dialog.getDialog().size() - 1).hashCode() != last) {
                        System.out.println(dialogs.get(Integer.valueOf(finalChoiceDialog)).getDialog().get(dialog.getDialog().size() - 1));
                        last = dialogs.get(Integer.valueOf(finalChoiceDialog)).getDialog().get(dialog.getDialog().size() - 1).hashCode();
                    }
                }
            }).start();
            dialogs.get(Integer.valueOf(choiceDialog)).printDialog();

        }
    }
}