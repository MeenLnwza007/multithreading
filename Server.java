import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    static final int port = 12345;
    static final String serverPath = "./ServerPath/";
    private static int clientLeft;

    public Server() {
        int clientN = 0;
        try (ServerSocket serverSocket = new ServerSocket(port);) {
            System.out.println("This is Server");
            System.out.println("Server started on port " + port);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                clientN++;
                new ClientHandler(clientSocket, clientN).start();
                System.out.println("Client " +clientN+ " connected -> " +clientSocket);
                clientLeft++;
                System.out.println("Amount of client connected left: " +clientLeft);
            }
        } catch (Exception e) {
            System.err.println("Server failed");
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader inputFromClient;
        private PrintWriter outputToClient;
        private int clientN;

        public ClientHandler(Socket clientSocket, int clientN) throws IOException {
            this.clientSocket = clientSocket;
            this.clientN = clientN;
            this.inputFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.outputToClient = new PrintWriter(clientSocket.getOutputStream(), true);
        }

        public void fileList() throws IOException {
            File[] files = new File(serverPath).listFiles();
            if (files.length == 0) {
                outputToClient.println("No file in server");
                System.err.println("File list does not exit");
            } else {
                String allFiles = "";
                for (int i = 0; i < files.length; i++) {
                    allFiles += String.format("[%d] - %s\n", i + 1, files[i].getName());
                }
                outputToClient.println(allFiles);
                outputToClient.println("EOF");
                System.out.println("File list sent successfully");
            }
        }

        public void sendFile(String fileName, long startByte, long endByte) {
            try (RandomAccessFile raf = new RandomAccessFile(new File(serverPath+fileName), "r")) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                raf.seek(startByte);
                OutputStream outByteToClient = clientSocket.getOutputStream();

                while (startByte < endByte && (bytesRead = raf.read(buffer)) != -1) {
                    outByteToClient.write(buffer, 0, bytesRead);
                    startByte += bytesRead;
                }
                System.out.println("Client " + clientN + " downloaded file: " + fileName);
                outByteToClient.flush();
                System.out.println("File sent successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            String clientCommand;
            try {
                while ((clientCommand = inputFromClient.readLine()) != null) {
                    if (!clientCommand.equals("download")) {
                        System.out.println("Client " +clientN+ " selected command: " +clientCommand);
                    }
                    if (clientCommand.equals("/filelist")) {
                        fileList();
                    } else if (clientCommand.equals("/request")) {
                        String fileName = inputFromClient.readLine();
                        File file = new File(serverPath + fileName);
                        if (file.exists()) {
                            outputToClient.println(file.length());
                        } else {
                            outputToClient.println("File does not exist");
                            System.out.println("Client " + clientN + " attempted to download a file that does not exist: " + fileName);
                        }
                    } else if (clientCommand.equals("download")) {
                        String fileName = inputFromClient.readLine();
                        long startByte = Long.parseLong(inputFromClient.readLine());
                        long endByte = Long.parseLong(inputFromClient.readLine());
                        sendFile(fileName, startByte, endByte);
                        clientLeft--;
                        System.err.println("Amount of client connected left: " +clientLeft);
                    } else if (clientCommand.equals("/exit")) {
                        System.out.println("Client " +clientN+ " disconneted -> " + clientSocket);
                        clientLeft--;
                        System.err.println("Amount of client connected left: " +clientLeft);
                        clientSocket.close();
                    } else {
                        System.err.println("Wrong command");
                    }
                }
            } catch (Exception e) {
                
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}