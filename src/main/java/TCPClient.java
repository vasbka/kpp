import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import entity.Dialog;
import entity.User;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static java.util.Objects.nonNull;

class TCPClient {
    public static void main(String argv[]) throws Exception {
        String name;
        Scanner scanner = new Scanner(new InputStreamReader(System.in));
        System.out.println("Enter your nick name : ");
        name = scanner.nextLine();
        User user = new User();
        user.setName(name);
        while (true) {
            System.out.printf("1 - register in system%n2 - remove from system%n3 - show list user%n" +
                    "4 - start dialogs%n5 - get all dialogs with me%n6 - open chat by dialogs id%n");
            Map<String, String> parameters = new HashMap<>();
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            Socket clientSocket = new Socket("localhost", 4000);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            int choice = Integer.valueOf(inFromUser.readLine());
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
            if (choice == 7) {
                System.out.print("Enter chat id : ");
                int chatId = 0;
                try {
                    chatId = Integer.valueOf(inFromUser.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (true) {
                    System.out.print("Enter your message : ");
                    String message = null;
                    try {
                        message = inFromUser.readLine();
                        parameters.put("message", new Gson().toJson(message, String.class));
                        parameters.put("chatId", new Gson().toJson(chatId, Integer.class));
                        outToServer.writeBytes(new Gson().toJson(parameters, HashMap.class) + "\r\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            outToServer.writeBytes(new Gson().toJson(parameters, HashMap.class) + "\r\n");
            inFromServer.readLine();
            HashMap<String, String> lst = new Gson().fromJson(inFromServer.readLine(), HashMap.class);
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
                System.out.println(dialogs);
                if (dialogs.isEmpty()) {
                    System.out.println("Your dialogs list is empty.");
                } else {
                    for (Dialog dialog : dialogs) {
                        System.out.println("Dialog id : " + dialog.getId());
                    }
                }
            }
            if (nonNull(lst.get("dialog"))) {
                Dialog dialog = new Gson().fromJson(lst.get("dialog"), Dialog.class);
//                for (String string : dialog.getDialog()) {
//                    System.out.println(string);
//                }
                new Thread(() -> {
                    while (true) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println(dialog.getDialog().size());
                    }
                }).start();
            }
            clientSocket.close();
        }
    }
}