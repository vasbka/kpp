import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import entity.User;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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
            System.out.printf("1 - register in system%n2 - remove from system%n3 - show list user%n");
            Map<String, String> parameters = new HashMap<>();
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            Socket clientSocket = new Socket("localhost", 4000);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            user.setChoice(Integer.valueOf(inFromUser.readLine()));
            parameters.put("user", new Gson().toJson(user, User.class));
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
            clientSocket.close();
        }
    }
}