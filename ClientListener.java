import java.io.* ;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientListener extends Thread{
    Socket clientSock;
    DatagramSocket udpSocket;
    String userName;
    ClientListener(Socket sock, DatagramSocket udpSoc){
        clientSock = sock;
        udpSocket = udpSoc;
        Integer userIndex = Server.clientSockets.indexOf(clientSock);
        userName = Server.Users.get(userIndex);
    }

    public void run(){
        try{
            DataInputStream clientInput = new DataInputStream(clientSock.getInputStream());
            while(true)
                execute_command(clientInput.readUTF());
        }
        catch(Exception ex){
            Server.deleteUser(userName, clientSock);
            ex.printStackTrace();
        }
    }
    public void execute_command(String command) throws IOException {
        StringTokenizer st = new StringTokenizer(command);
        String cmd = st.nextToken();
        if(cmd.equals("create_group")){
            if (st.countTokens() > 0)
                Server.createGroup(st.nextToken(), userName, clientSock);
            else {
                DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
                clientOutput.writeUTF("[SERVER]: Enter group name also");
            }
        }
        else if(cmd.equals("list_groups")){
            Server.listGroups(clientSock);
        }
        else if(cmd.equals("join_group")){
            if (st.countTokens() > 0)
                Server.joinGroup(st.nextToken(), userName, clientSock);
            else {
                DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
                clientOutput.writeUTF("[SERVER]: Enter group name also");
            }
        }
        else if(cmd.equals("leave_group")){
            if (st.countTokens() > 0)
                Server.leaveGroup(st.nextToken(), userName, clientSock);
            else {
                DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
                clientOutput.writeUTF("[SERVER]: Enter group name also");
            }
        }
        else if(cmd.equals("list_detail")){
            if (st.countTokens() > 0)
                Server.listDetails(st.nextToken(), clientSock);
            else {
                DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
                clientOutput.writeUTF("[SERVER]: Enter group name also");
            }
        }
        else if(cmd.equals("share_msg")){
            if (st.countTokens() > 1) {
                String groupname = st.nextToken();
                Server.shareMessage(userName, groupname, clientSock, st);
            } else {
                DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
                clientOutput.writeUTF("[SERVER]: Enter group name and message also");
            }
        }
        else if(cmd.equals("create_folder")){
            if (st.countTokens() > 0)
                create_folder(st.nextToken());
            else {
                DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
                clientOutput.writeUTF("[SERVER]: Enter folder name");
            }
        } else if(cmd.equals("move_file")){
            if (st.countTokens() > 1) {
                String source = st.nextToken(), dest = st.nextToken();
                move_file(source, dest);
            } else {
                DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
                clientOutput.writeUTF("[SERVER]: Enter source and destination paths");
            }
        } else if (cmd.contains("upload")) {
            if (st.countTokens() > 1) {
                uploadFile(st, cmd);
            } else {
                DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
                clientOutput.writeUTF("[SERVER]: Please enter a file path");
            }
        } else if (cmd.equals("get_file")) {
            if (st.countTokens() > 0) {
                downloadFile(st.nextToken());
            } else {
                DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
                clientOutput.writeUTF("[SERVER]: Please enter a file path");
            }
        } else if (cmd.equalsIgnoreCase("logout"))
            Server.logout(userName, clientSock);
        else{
            DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
            clientOutput.writeUTF("[SERVER]: Invalid command format passed");
        }
    }

    public void create_folder(String folderName){
        try{
            DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
            String pathName = Server.SERVER_DIRECTORY + userName + "/" + folderName;
            File folder = new File(pathName);
            if (!folder.exists()){
                if(folder.mkdir())
                    clientOutput.writeUTF("[SERVER]: Folder created");
                else
                    clientOutput.writeUTF("[SERVER]: Could not create folder");
            }
            else{
                clientOutput.writeUTF("[SERVER]: Folder already present on Server");
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void move_file(String source, String target){
        try{
            DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
            String sourcePath = Server.SERVER_DIRECTORY + userName + "/" + source;
            File sourceFile = new File(sourcePath);
            String targetPath = Server.SERVER_DIRECTORY + userName + "/" + target;
            File targetDest = new File(targetPath);
            if (!sourceFile.isFile() || (targetDest.isDirectory())) {
                if(sourceFile.isDirectory()){
                    clientOutput.writeUTF("[SERVER]: Given source path is a folder");
                }
                else if(!sourceFile.exists()){
                    clientOutput.writeUTF("[SERVER]: Given source path does not exists");
                }
                else if(targetDest.isDirectory()){
                    clientOutput.writeUTF("[SERVER]: Given destination path is an existing folder");
                }
            } else {
                if(sourceFile.renameTo(targetDest)){
                    sourceFile.delete();
                    clientOutput.writeUTF("[SERVER]: File moved successfully");
                }
                else {
                    clientOutput.writeUTF("[SERVER]: Could not move file");
                }
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void uploadFile(StringTokenizer st, String cmd) throws IOException {
        try{
            String[] filetokens = st.nextToken().split("/");
            int fileNameIndex = filetokens.length - 1;
            String filename = filetokens[fileNameIndex];
            String newPath = Server.SERVER_DIRECTORY + userName + "/" + filename;
            File file = new File(newPath);
            file.createNewFile();
            int remainingFileSize = Integer.parseInt(st.nextToken());
            FileOutputStream fpout = new FileOutputStream(Server.SERVER_DIRECTORY + userName + "/" + filename);
            DataInputStream clientInput;
            while(remainingFileSize > 0){
                clientInput = new DataInputStream(clientSock.getInputStream());
                int possibleSize = Integer.min(remainingFileSize, 1000);
                byte[] data = new byte[possibleSize];
                if(cmd.equals("upload"))
                    clientInput.read(data, 0, possibleSize);
                else {
                    DatagramPacket recvPacket = new DatagramPacket(data, possibleSize);
                    udpSocket.receive(recvPacket);
                }
                fpout.write(data, 0, possibleSize);
                remainingFileSize -= 1000;
            }
            DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
            clientOutput.writeUTF("[SERVER]: File uploaded");
            System.out.printf("[SERVER]: File uploaded for user: %s, with file Name: %s%n", userName, filename);
            fpout.close();
        }
        catch(IOException e){
            DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
            clientOutput.writeUTF("[SERVER]: Unable to upload file " + e.toString());
        }
    }

    public void downloadFile(String fileName) throws IOException {
        try {
            DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
            String pathName = Server.SERVER_DIRECTORY + userName + "/" + fileName;
            File file = new File(pathName);
            if(file.isFile()){
                String cmd = String.format("start-download %s %d", fileName, file.length());
                clientOutput.writeUTF(cmd);
                InputStream in = new BufferedInputStream(new FileInputStream(file));
                int fileSize = (int) file.length();
                while(fileSize > 0){
                    int possibleSize = Integer.min(fileSize, 1000);
                    byte[] buf = new byte[possibleSize];
                    in.read(buf, 0, possibleSize);
                    clientOutput.write(buf, 0, possibleSize);
                    fileSize -= 1000;
                }
                clientOutput.writeUTF("[SERVER]: File downloaded");
                System.out.println("[SERVER]: File downloaded for user:" + userName + ", File Name: " + fileName);
                in.close();
            }
            else
                clientOutput.writeUTF("[SERVER]: Given path file does not exist");
        } catch (Exception ex) {
            DataOutputStream clientOutput = new DataOutputStream(clientSock.getOutputStream());
            clientOutput.writeUTF(String.format("[SERVER]: Unable to download file %s", ex.toString()));
        }
    }
}