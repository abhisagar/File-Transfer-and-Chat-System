import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

class Chatroom {
    String groupName;
    private Vector<String> groupMembers = new Vector<>();
    private Vector<Socket> memberSockets = new Vector<>();
    private DataOutputStream dataOut;

    Chatroom(String groupName, String userName, Socket clientSocket) throws IOException {
        this.groupName = groupName;
        groupMembers.add(userName);
        memberSockets.add(clientSocket);
        dataOut = new DataOutputStream(clientSocket.getOutputStream());
        System.out.println("[SERVER]: Created group " + this.groupName);
    }

    public void add_user(String username, Socket clientSocket){
        try {
            if (groupMembers.contains(username)) {
                dataOut.writeUTF("[SERVER]: User already joined in this group " + groupName);
            } else {
                groupMembers.add(username);
                memberSockets.add(clientSocket);
                dataOut.writeUTF("[SERVER]: User added in group " + groupName);
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }
    public boolean remove_user(String username, Socket clientSocket, boolean write_output){
        try{
            if(groupMembers.contains(username)){
                groupMembers.remove(username);
                memberSockets.remove(clientSocket);
                if(write_output){
                    dataOut.writeUTF("[SERVER]: User removed from group " + groupName);
                }
            }
            else{
                if(write_output){
                    dataOut.writeUTF("[SERVER]: User not present in group " + groupName);
                }
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        if (groupMembers.size() == 0)
            return false;
        return true;
    }
    public void share_msg(String sender, StringTokenizer st){
        try{
            String receivedMessage = new String();
            while(st.hasMoreTokens()){
                String msg = st.nextToken();
                receivedMessage += msg + " ";
            }
            for(int i = 0; i< groupMembers.size(); i++){
                DataOutputStream dout = new DataOutputStream(memberSockets.get(i).getOutputStream());
                if (groupMembers.get(i).equals(sender)) {
                    dout.writeUTF(String.format("[SERVER]: Message delivered in group: %s", groupName));
                } else {
                    dout.writeUTF(String.format("[Group: %s][User: %s]: %s", groupName, sender, receivedMessage));
                }
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void showDetails(Socket clientSocket){
        String detailOutput = new String();
        try{
            DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream());
            detailOutput += "[SERVER]: Details of Group: " + groupName + "\n";
            for(int i = 0; i< groupMembers.size(); i++){
                File[] userFiles = new File(Server.SERVER_DIRECTORY + groupMembers.get(i)).listFiles();
                detailOutput += "User: "+ groupMembers.get(i);
                detailOutput += "\n********************\n";
                if (userFiles.length == 0) {
                    detailOutput += "No Files present for User: " + groupMembers.get(i) + "\n";
                } else {
                    detailOutput += "Files of user: " + groupMembers.get(i) + "\n";
                    detailOutput += getUserFiles("", userFiles);
                }
                if (i+1 != groupMembers.size())
                    detailOutput += "\n";
            }
            dout.writeUTF(detailOutput);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private static String getUserFiles(String prefix, File[] arr){
        String userFileDetails = new String();
        for(File file: arr){
            if(file.isFile())
                userFileDetails += prefix + file.getName() + "\n";
            else if(file.isDirectory()){
                userFileDetails += prefix + "Directory: " + file.getName() + "/" + "\n";
                userFileDetails += getUserFiles(prefix + file.getName() + "/", file.listFiles());
            }
        }
        return userFileDetails;
   }
}