import com.google.gson.Gson;
import entity.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class TCPServer {
    public static void main(String argv[]) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(4000);
        List<User> users = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger id = new AtomicInteger(0);
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
                        if (user.getChoice() == 1) {
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
                        if (user.getChoice() == 2) {
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
                        if (user.getChoice() == 3) {
                            parameters.put("users", gson.toJson(users, List.class));
                            outToClient.writeBytes("\r\n" + gson.toJson(parameters, HashMap.class) + "\r\n");
                            return;
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