import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import entity.Dialog;
import entity.User;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

class TCPClient {

    public static void main(String argv[]) throws Exception {
        String name;
        Scanner scanner = new Scanner(new InputStreamReader(System.in));
        User user;
        while (true) {
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            Socket clientSocket = new Socket("localhost", 4000);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            Map<String, String> parameters = Collections.synchronizedMap(new HashMap<>());

            System.out.println("Enter your nick name : ");
            name = scanner.nextLine();
            user = new User();
            user.setName(name);
            parameters.put("choice", "0");
            parameters.put("user", new Gson().toJson(user, User.class));
            outToServer.writeBytes(new Gson().toJson(parameters, HashMap.class) + "\r\n");
            try {
                inFromServer.readLine();
            } catch (SocketException e) {

            }
            HashMap<String, String> lst = new Gson().fromJson(inFromServer.readLine(), HashMap.class);
            if (isNull(lst.get("errors"))) {
                user = new Gson().fromJson(lst.get("user"), User.class);
                break;
            }
            new Gson().fromJson(lst.get("errors"), List.class).forEach((error) -> System.out.println(error));
        }

        while (true) {
            System.out.printf("1 - remove from system%n3 - show list user%n" +
                    "4 - start dialogs%n5 - get all dialogs with me%n6 - open chat by dialogs id%n7 - start chat by chat id%n");
            Map<String, String> parameters = Collections.synchronizedMap(new HashMap<>());
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            Socket clientSocket = new Socket("localhost", 4000);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            int choice;
            choice = Integer.valueOf(inFromUser.readLine());
            parameters.put("choice", String.valueOf(choice));
            parameters.put("user", new Gson().toJson(user, User.class));
            if (choice == 4) {
                System.out.print("Enter id user for start chat : ");
                int idUser = Integer.valueOf(inFromUser.readLine());
                parameters.put("userToChat", new Gson().toJson(idUser, Integer.class));
            }
            if (choice == 6) {
                System.out.print("Enter id chat which you want to starting: ");
                int idChat = Integer.valueOf(inFromUser.readLine());
                parameters.put("idChatToStart", new Gson().toJson(idChat, Integer.class));
            }
            int chatId = 0;
            if (choice == 7) {
                System.out.print("Enter chat id : ");
                while (true) {
                    try {
                        chatId = Integer.valueOf(inFromUser.readLine());
                        break;
                    } catch (IOException e) {
//                    e.printStackTrace();
                    }
                }
                parameters.put("chatId", new Gson().toJson(chatId, Integer.class));
            }
            outToServer.writeBytes(new Gson().toJson(parameters, HashMap.class) + "\r\n");
            inFromServer.readLine();
            HashMap<String, String> lst = new Gson().fromJson(inFromServer.readLine(), HashMap.class);

            if (nonNull(lst.get("errors"))) {
                new Gson().fromJson(lst.get("errors"), List.class).forEach((error) -> System.out.println(error));
            }
            if (nonNull(lst.get("users"))) {
                List<User> users = new Gson().fromJson(lst.get("users"), new TypeToken<List<User>>() {
                }.getType());
                if (users.isEmpty()) {
                    System.out.println("Users list is empty!");
                } else {
                    for (User userFromList : users) {
                        if (!userFromList.getName().equals(user.getName())) {
                            System.out.println("user: " + userFromList.getName() + " : " + userFromList.getId());
                        }
                    }
                }
            }
            if (nonNull(lst.get("dialogs"))) {
                List<Dialog> dialogs = new Gson().fromJson(lst.get("dialogs"), new TypeToken<List<Dialog>>() {
                }.getType());
                if (dialogs.isEmpty()) {
                    System.out.println("Your dialogs list is empty.");
                } else {
                    for (Dialog dialog : dialogs) {
                        System.out.print("Dialog id : " + dialog.getId() + "<");
                        dialog.getUsers().forEach((v) -> System.out.print(v));
                        System.out.print(">");
                    }
                }
            }
            if (nonNull(lst.get("chatId"))) {
                int finalChatId = chatId;
                User finalUser = user;
                Thread messages = new Thread(() -> {
                    int hashLastDialog = 0;
                    Map<String, String> localParam;
                    while (!Thread.currentThread().isInterrupted()) {
                        localParam = new HashMap<>();
                        Socket localClient = null;
                        DataOutputStream localToServer = null;
                        BufferedReader localInFromServer = null;
                        localParam.put("chatId", new Gson().toJson(finalChatId, Integer.class));
                        localParam.put("choice", String.valueOf(choice));
                        localParam.put("user", new Gson().toJson(finalUser, User.class));
                        Dialog dlg = null;
                        while (isNull(dlg)) {
                            try {
                                localClient = new Socket("localhost", 4000);
                                localToServer = new DataOutputStream(localClient.getOutputStream());
                                localInFromServer = new BufferedReader(new InputStreamReader(localClient.getInputStream()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                localToServer.writeBytes(new Gson().toJson(localParam, HashMap.class) + "\r\n");
                                localInFromServer.readLine();
                                HashMap<String, String> res = new Gson().fromJson(localInFromServer.readLine(), HashMap.class);
                                if (nonNull(res)) {
                                    dlg = new Gson().fromJson(res.get("dialog"), Dialog.class);
                                }
                            } catch (IOException e) {
                            } finally {
                                try {
                                    localClient.close();
                                } catch (IOException e) {
                                }
                            }
                        }
                        if (dlg.hashCode() != hashLastDialog) {
                            if (dlg.getDialog().size() >= 1) {
                                if (hashLastDialog == 0) {
                                    for (int indexLastRedMessages = 0; indexLastRedMessages < dlg.getDialog().size(); indexLastRedMessages++) {
                                        if (dlg.getDialog().size() > 1) {
                                            System.out.println(dlg.getDialog().get(indexLastRedMessages));
                                        }
                                    }
                                } else {
                                    String message = dlg.getDialog().get(dlg.getDialog().size() - 1);
                                    if (!message.contains(finalUser.getName())) {
                                        System.out.println(message);
                                    }
                                }
                                hashLastDialog = dlg.hashCode();
                            }
                        }
                    }
                });
                messages.start();
                Thread chat = new Thread(() -> {
                    while (isNull(parameters.get("messages")) || !parameters.get("messages").equals("stop")) {
                        Socket localClient = null;
                        BufferedReader localInFromUser = new BufferedReader(new InputStreamReader(System.in));
                        DataOutputStream localOutToServ = null;
                        try {
                            localClient = new Socket("localhost", 4000);
                            localOutToServ = new DataOutputStream(localClient.getOutputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            String message = localInFromUser.readLine();
                            if (message.equals("stop")) {
                                messages.interrupt();
                                localOutToServ.close();
                                return;
                            }
                            if (message.equals("options")) {
                                System.out.printf("1 - added user to chat by id%n0 - exit%n");
                                int option = -1;
                                Scanner scanner1 = new Scanner(System.in);
                                while (option != 0 && option != 1) {
                                    option = scanner1.nextInt();
                                }
                                System.out.println("Enter user id for added to chat or 0 to exit: ");
                                int userId = scanner1.nextInt();
                                System.out.println(option + " : " + userId);
                                if (option == 1 && userId != 0) {
                                    parameters.put("option", new Gson().toJson(option, Integer.class));
                                    parameters.put("userIdForAddedToChat", new Gson().toJson(userId, Integer.class));
                                }
                            }
                            parameters.put("message", new Gson().toJson(message, String.class));
                            parameters.put("chatId", new Gson().toJson(finalChatId, Integer.class));
                            localOutToServ.writeBytes(new Gson().toJson(parameters, HashMap.class) + "\r\n");
                            parameters.put("message", new Gson().toJson(null, String.class));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                localOutToServ.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                chat.start();
                chat.join();
            }
            clientSocket.close();
        }
    }
}