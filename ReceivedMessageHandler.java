import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

class ReceivedMessageHandler implements Runnable{
    DataInputStream server;
    ReceivedMessageHandler(DataInputStream clientInput){
        server = clientInput;
    }
    @Override
    public void run() {
        try{
            String serverOutput;
            while(true){
                serverOutput = server.readUTF();
                if(serverOutput.contains("start-download"))
                    download_file(serverOutput);
                else
                    System.out.println(serverOutput);
            }
        }
        catch(IOException e){
            System.out.println("Connection to server lost");
            System.exit(0);
        }
    }

    private void download_file(String cmd){
        FileOutputStream fpout;
        try{
            String[] tokens = cmd.split(" ");
            fpout = new FileOutputStream(Client.CLIENT_DIR + "/" + tokens[1]);
            int fileSizeRem = Integer.parseInt(tokens[2]);
            while(fileSizeRem > 0){
                int possibleSize = Integer.min(fileSizeRem, 1000);
                byte[] data = new byte[possibleSize];
                server.read(data, 0, possibleSize);
                fpout.write(data, 0, possibleSize);
                fileSizeRem -= 1000;
            }
            fpout.close();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
