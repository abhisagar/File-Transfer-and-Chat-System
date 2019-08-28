import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class Client {
    static String IP_ADDR = "127.0.0.1";
    static String CLIENT_DIR = "ClientDir/";
    static int PORT = 7000;
    static Socket clientSock;
    static DatagramSocket udpSock;
    static String userName = null;
    static DataOutputStream clientOutput;
    static DataInputStream clientInput;

    public static void main(String[] args) throws IOException {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            clientSock = new Socket(IP_ADDR, PORT);
            udpSock = new DatagramSocket();
            File clientDir = new File(CLIENT_DIR);
            if (!clientDir.exists())
                clientDir.mkdir();
            clientOutput = new DataOutputStream(clientSock.getOutputStream());
            clientInput = new DataInputStream(clientSock.getInputStream());
            new Thread(new ReceivedMessageHandler(clientInput)).start();
            while (userName == null) {
                String input = bufferedReader.readLine();
                if (input.isEmpty())
                    continue;
                StringTokenizer st = new StringTokenizer(input);
                String cmd_option = st.nextToken();
                if (cmd_option.equals("create_user")) {
                    if (st.countTokens() > 0)
                        userName = st.nextToken();
                    else
                        System.out.println("Please Enter User Name");
                }
            }
            clientOutput.writeUTF(userName);
            while (true) {
                String input = bufferedReader.readLine();
                if (!input.isEmpty()) {
                    if (input.contains("upload"))
                        upload_file(input, clientOutput);
                    else
                        clientOutput.writeUTF(input);
                }
            }
        } catch (Exception e) {
            System.out.printf("Problem in connection to Server: %s%n", e);
            e.printStackTrace();
            System.exit(0);
        }
    }

    static int getFileSize(String input) {
        String[] tokens = input.split(" ");
        int fileSize = -1;
        if (tokens.length <= 1) {
            System.out.println("Incorrect File upload command format");
        } else {
            File file = new File(tokens[1]);
            if (!file.isFile())
                System.out.println("Problems occurred while reading file");
            else
                fileSize = (int) file.length();
        }
        return fileSize;
    }

    static byte[] read_file(String input, long fileSize) {
        try {
            String fileName = input.split(" ")[1];
            File file = new File(fileName);
            InputStream in = new BufferedInputStream(new FileInputStream(file));
            byte[] buf = new byte[(int) file.length()];
            in.read(buf);
            in.close();
            return buf;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    static void upload_file(String input, DataOutput clientOutput) {
        try {
            int fileSize = getFileSize(input);
            if (fileSize < 0) {
                return;
            } else {
                clientOutput.writeUTF(input + " " + fileSize);
                String[] tokens = input.split(" ");
                File file = new File(tokens[1]);
                InputStream in = new BufferedInputStream(new FileInputStream(file));
                while (fileSize > 0) {
                    int possibleSize = Integer.min(fileSize, 1000);
                    byte[] buf = new byte[possibleSize];
                    in.read(buf, 0, possibleSize);
                    if (tokens[0].equals("upload_udp")) {
                        DatagramPacket sendPacket;
                        sendPacket = new DatagramPacket(buf, possibleSize, InetAddress.getByName(IP_ADDR), PORT + 1);
                        udpSock.send(sendPacket);
                        Thread.sleep(1);
                    } else {
                        clientOutput.write(buf, 0, Integer.min(fileSize, 1000));
                    }
                    fileSize -= 1000;
                }
                in.close();
            }
        } catch (Exception e) {
            System.out.printf("could not upload file from client%n%s%n", e.toString());
        }

    }
}