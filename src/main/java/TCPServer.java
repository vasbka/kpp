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

class TCPServer {
    public static void main(String argv[]) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(4000);
        List<User> users = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger id = new AtomicInteger(0);
        AtomicInteger idDialog = new AtomicInteger(0);
        List<Dialog> dialogs = new ArrayList<>();
        new Thread(() -> {
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
                        if (choice == 1) {
                            while (userIterator.hasNext()) {
                                if (userIterator.next().getName().equals(user.getName())) {
                                    errors.add("User with this login already exists.");
                                }
                            }
                            if (errors.isEmpty()) {
                                user.setId(id.getAndIncrement());
                                users.add(user);
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
                                if (dialog.getId() == chatId) {
                                    dialog.addMessage(message);
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
                        System.out.println(e.getMessage());
                    }
                }).start();
            }
        }).start();

        while (true) {
            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
            System.out.println(bf.readLine());
        }
    }
}