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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

class TCPServer {
    public static void main(String argv[]) throws IOException {

        List<User> users = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger id = new AtomicInteger(0);
        AtomicInteger idDialog = new AtomicInteger(0);
        List<Dialog> dialogs = Collections.synchronizedList(new ArrayList<>());
        new Thread(() -> {
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
                Socket finalConnectionSocket = connectionSocket;
                new Thread(() -> {
                    try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(finalConnectionSocket.getInputStream()));
                         ObjectOutputStream outToClient = new ObjectOutputStream(finalConnectionSocket.getOutputStream())) {
                        Gson gson = new Gson();
                        HashMap<String, String> param = gson.fromJson(inFromClient.readLine(), HashMap.class);
                        Map<String, Object> parameters = new HashMap<>();
                        List<String> errors = new ArrayList<>();
                        User user = gson.fromJson(param.get("user"), User.class);
                        Iterator<User> userIterator = users.iterator();
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
                        if (choice == 2) {
                            //leave
                            userIterator = users.iterator();
                            while (userIterator.hasNext()) {
                                if (userIterator.next().getName().equals(user.getName())) {
                                    userIterator.remove();
                                    return;
                                }
                            }
                        }
                        //get all user
                        if (choice == 3) {
                            parameters.put("users", gson.toJson(users, List.class));
                            System.out.println("in third");
                            outToClient.writeBytes("\r\n" + gson.toJson(parameters, HashMap.class) + "\r\n");
                            return;
                        }
                        //start dialog
                        if (choice == 4) {
                            int idUserToChat = gson.fromJson(param.get("userToChat"), Integer.class);
                            if (isNull(idUserToChat)) {
                                errors.add("Dialog can't be creating with partner with this id.");
                            }
                            for (User userWithChat : users) {
                                if (userWithChat.getId() == idUserToChat) {
                                    Dialog dialog = new Dialog();
                                    dialog.setId(idDialog.getAndIncrement());
                                    dialog.addUser(userWithChat);
                                    dialog.addUser(user);
                                    dialogs.add(dialog);
                                }
                            }
                        }
                        //get all dialogs in which user is exist
                        if (choice == 5) {
                            List<Dialog> userDialogs = new ArrayList<>();
                            for (Dialog dialog : dialogs) {
                                if (dialog.userExistInDialog(user)) {
                                    System.out.println("user with dialogs");
                                    userDialogs.add(dialog);
                                }
                            }
                            parameters.put("dialogs", gson.toJson(userDialogs, List.class));
                        }
                        if (choice == 6) {
                            int idChatToStart = gson.fromJson(param.get("idChatToStart"), Integer.class);
                            if (isNull(idChatToStart)) {
                                errors.add("Dialog can't be creating with partner with this id.");
                            }
                            for (Dialog dialog : dialogs) {
                                if (dialog.getId() == idChatToStart) {
                                    parameters.put("dialog", new Gson().toJson(dialog, Dialog.class));
                                }
                            }
                        }
                        if (choice == 7) {
                            int chatId = gson.fromJson(param.get("chatId"), Integer.class);
                            String message = gson.fromJson(param.get("message"), String.class);
                            for (Dialog dialog : dialogs) {
                                if (dialog.getId() == chatId && dialog.getUsers().contains(user)) {
                                    parameters.put("dialog", new Gson().toJson(dialog, Dialog.class));
                                    parameters.put("chatId", chatId);
                                    if (nonNull(param.get("option"))) {
                                        System.out.println("0");
                                        int option;
                                        System.out.println("1");
                                        while (true) {
                                            try {
                                                option = Integer.parseInt(param.get("option"));
                                                break;
                                            } catch (NumberFormatException e) {
                                            }
                                        }
                                        int userId;
                                        System.out.println("2");
                                        while (true) {
                                            try {
                                                userId = Integer.parseInt(param.get("userIdForAddedToChat"));
                                                break;
                                            } catch (NumberFormatException e) {

                                            }
                                        }
                                        System.out.println("3");
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
                        outToClient.writeBytes("\r\n" + gson.toJson(parameters, HashMap.class) + "\r\n");
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }
                }).start();
            }
        }).start();
        Thread welcomeNewUser = new Thread(() -> {

        });
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
            dialogs.get(Integer.valueOf(choiceDialog)).printDialog();

        }
    }
}