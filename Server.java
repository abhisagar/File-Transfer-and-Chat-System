import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import static java.lang.Runtime.getRuntime;

public class Server {
    private static int PORT = 7000;
    public static String SERVER_DIRECTORY = "ServerDir/";
    public static Vector<String> Users = new Vector<>();
    public static Vector<Socket> clientSockets = new Vector<>();
    public static Map<String, Set<Chatroom>> userChatroomMap = new HashMap<>();
    public static Vector<Chatroom> groups = new Vector<>();
    private static ServerSocket serverSocket;
    static DatagramSocket udpSocket;

    public static void main(String[] args) {
        try {
            udpSocket = new DatagramSocket(PORT+1);
            serverSocket = new ServerSocket(PORT);
            getRuntime().addShutdownHook(new ShutDownThread());
            System.out.printf("[SERVER]: Server running on port: %d%n", PORT);
            File serverDir = new File(SERVER_DIRECTORY);
            if (!serverDir.exists()) {
                serverDir.mkdir();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        while (true) {
            try {
                Socket ClientSock = serverSocket.accept();
                if (Server.addNewUser(ClientSock)) {
                    new ClientListener(ClientSock, udpSocket).start();
                }
            } catch (Exception ex) {
                System.out.printf("[SERVER]: Could not accept client%s%n", ex.toString());
            }
        }
    }

    public static boolean addNewUser(Socket ClientSock) {
        try {
            DataOutputStream dout = new DataOutputStream(ClientSock.getOutputStream());
            DataInputStream clientInput = new DataInputStream(ClientSock.getInputStream());
            String username = clientInput.readUTF();
            if (Users.contains(username)) {
                dout.writeUTF("[SERVER]: Could not login user " + username + "...a client with same username is connected");
                System.out.println("[SERVER]: Could not login user " + username + "...a client with same username is connected");
                ClientSock.close();
                return false;
            } else {
                File userFolder = new File(String.format("%s%s", SERVER_DIRECTORY, username));
                userFolder.mkdir();
                userChatroomMap.put(username, new HashSet<>());
                Users.add(username);
                clientSockets.add(ClientSock);
                System.out.printf("[SERVER]: %s connected to server%n", username);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public static synchronized void deleteUser(String username, Socket clientSock) {
        System.out.printf("[SERVER]: %s disconnected%n", username);
        if (userChatroomMap.containsKey(username)) {
            for (Chatroom group : userChatroomMap.get(username)) {
                if (!group.remove_user(username, clientSock, false)) {
                    groups.remove(group);
                    System.out.printf("[SERVER]: Removed group %s%n", group.groupName);
                }
            }
            userChatroomMap.remove(username);
        }
        try {
            clientSock.close();
            Users.remove(username);
            clientSockets.remove(clientSock);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static synchronized void createGroup(String groupName, String userName, Socket clientSock) {
        try {
            DataOutputStream dout = new DataOutputStream(clientSock.getOutputStream());
            boolean flag = false;
            for (Chatroom group: groups) {
                if (group.groupName.equals(groupName)) {
                    dout.writeUTF("[SERVER]: Group already created");
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                dout.writeUTF("[SERVER]: Created group");
                Chatroom groupObj = new Chatroom(groupName, userName, clientSock);
                groups.add(groupObj);
                userChatroomMap.get(userName).add(groupObj);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static synchronized void joinGroup(String groupName, String userName, Socket clientSock) {
        try {
            DataOutputStream dout = new DataOutputStream(clientSock.getOutputStream());
            boolean flag = false;
            for (Chatroom group: groups) {
                if (group.groupName.equals(groupName)) {
                    group.add_user(userName, clientSock);
                    userChatroomMap.get(userName).add(group);
                    dout.writeUTF(String.format("[SERVER]: Added to group: %s", groupName));
                    flag = true;
                    break;
                }
            }
            if (!flag)
                dout.writeUTF("[SERVER]: No group with the given name");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static synchronized void leaveGroup(String groupName, String userName, Socket clientSock) {
        try {
            DataOutputStream dout = new DataOutputStream(clientSock.getOutputStream());
            boolean flag = false;
            for (int i = 0; i < groups.size(); i++) {
                if (groups.get(i).groupName.equals(groupName)) {
                    if (!groups.get(i).remove_user(userName, clientSock, true)) {
                        Chatroom group = groups.get(i);
                        userChatroomMap.get(userName).remove(group);
                        System.out.printf("[SERVER]: Removed group %s%n", group.groupName);
                        groups.remove(group);
                    } else {
                        userChatroomMap.get(userName).remove(groups.get(i));
                    }
                    flag = true;
                    break;
                }
            }
            if (!flag)
                dout.writeUTF("[SERVER]: No group with the given name");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void listGroups(Socket clientSock) {
        try {
            DataOutputStream dout = new DataOutputStream(clientSock.getOutputStream());
            if (groups.size() != 0) {
                String availableGroups = "Available groups: ";
                for (int i = 0; i < groups.size(); i++) {
                    availableGroups += groups.get(i).groupName;
                    if (i != groups.size() - 1) {
                        availableGroups += ", ";
                    }
                }
                dout.writeUTF(availableGroups);
            } else {
                dout.writeUTF("[SERVER]: No groups available");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void shareMessage(String userName, String groupName, Socket clientSock, StringTokenizer st) {
        DataOutputStream dout;
        try {
            dout = new DataOutputStream(clientSock.getOutputStream());
            if (userChatroomMap.containsKey(userName)) {
                for (Chatroom group : userChatroomMap.get(userName)) {
                    if (group.groupName.equals(groupName))
                        group.share_msg(userName, st);
                }
            } else {
                dout.writeUTF("[SERVER]: User is not part of any group");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void listDetails(String groupName, Socket clientSock) {
        DataOutputStream dout;
        try {
            dout = new DataOutputStream(clientSock.getOutputStream());
            boolean flag = false;
            for (Chatroom group: groups) {
                if (group.groupName.equals(groupName)) {
                    group.showDetails(clientSock);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                dout.writeUTF("[SERVER]: No group with the given name");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void logout(String userName, Socket clientSock) {
        try {
            deleteUser(userName, clientSock);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

class ShutDownThread extends Thread {
    public void run() {
        System.out.println("Closing Server... :)");
    }
}