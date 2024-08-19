import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    static String serverIP = "127.0.0.1";
    static final int port = Server.port;
    static final String clientPath = "./ClientPath/";
    static final int threadN = 10;

    public Client() {
        try {
            System.out.println("This is Client");
            Socket clientSocket = new Socket(serverIP, port);
            System.out.println("Connected to server -> " + clientSocket);

            BufferedReader inputFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter outputToServer = new PrintWriter(clientSocket.getOutputStream(), true);

            Scanner sc = new Scanner(System.in);
            String clientCommand;
            while (!clientSocket.isClosed()) {
                System.out.println("----------------------------------------");
                System.out.println("-- All Command --");
                System.out.println("Type /filelist to see all files in server");
                System.out.println("Type /request to download file from server");
                System.out.println("Type /exit to disconnect to server");
                System.out.print("Enter your command: ");
                clientCommand = sc.nextLine();

                if (clientCommand.equals("/filelist")) {
                    outputToServer.println(clientCommand);
                    String allFiles;
                    while ((allFiles = inputFromServer.readLine()) != null) {
                        if ("EOF".equals(allFiles)) {
                            break;
                        }
                        System.out.println(allFiles);
                    }
                } else if (clientCommand.equals("/request")) {
                    outputToServer.println(clientCommand);
                    System.out.print("Enter file name to download: ");
                    String fileName = sc.nextLine();
                    outputToServer.println(fileName);

                    /* String messageFromServer = inTextFromServer.readLine();
                    long fileSize = 0;
                    String errorFromServer = "";
                    if(messageFromServer.equals("Long")){
                        fileSize = Long.parseLong(inTextFromServer.readLine());
                    }
                    else if(messageFromServer.equals("String")){
                        errorFromServer = inTextFromServer.readLine();
                    } */

                    try {
                        long fileSize = Long.parseLong(inputFromServer.readLine());
                        long partSize = fileSize / threadN;
                        long remainingSize = fileSize % threadN;

                        if (fileSize > 0) {
                            System.out.println("FileSize: " + fileSize);
                            ArrayList<Thread> threads = new ArrayList<>();
                            for (int i = 0; i < threadN; i++) {
                                long startByte = i * partSize;
                                long endByte;
                                if (i == threadN - 1) {
                                    endByte = startByte + partSize + remainingSize;
                                } else {
                                    endByte = startByte + partSize;
                                }
                                System.out.println("Thread " + (i + 1) + " Start bytes: " + startByte + " End bytes: " + endByte);
                                Socket clientSocket2 = new Socket(serverIP, port);
                                Downloader t = new Downloader(startByte, endByte, fileName, clientSocket2);
                                threads.add(t);
                                t.start();
                            }
                            for (Thread thread : threads) {
                                try {
                                    thread.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            System.out.println("Download completed");
                        }
                    } catch (Exception e) {
                        System.err.println("File not found");
                    }
                } else if (clientCommand.equals("/exit")) {
                    outputToServer.println(clientCommand);
                    System.out.println("Disconneting...");
                    clientSocket.close();
                } else {
                    System.out.println("Wrong command, Please try again");
                }
            }
        } catch (Exception e) {
            System.out.println("Connection error");
        }
    }   

    private static class Downloader extends Thread {
        private long startByte, endByte;
        private String fileName;
        private Socket clientSocket;

        public Downloader(long startByte, long endByte, String fileName, Socket clientSocket) {
            this.startByte = startByte;
            this.endByte = endByte;
            this.fileName = fileName;
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                out.println("download");
                out.println(fileName);
                out.println(startByte);
                out.println(endByte);

                byte[] buffer = new byte[1024];
                int bytesRead;
                InputStream inByteFromServer = clientSocket.getInputStream();

                try (RandomAccessFile threadFile = new RandomAccessFile(clientPath + "copy-" + fileName, "rw")) {
                    while (startByte < endByte && (bytesRead = inByteFromServer.read(buffer)) != -1) {
                        threadFile.seek(startByte);
                        threadFile.write(buffer, 0, bytesRead);
                        startByte += bytesRead;
                    }
                     /*
                    if(startByte == endByte){
                    System.out.println(Thread.currentThread().getName()+1 + " Final Downloaded part from: " + startByte + " to " + endByte + " [real position("+ (startByte+1) +")]");   
                    }
                    else{
                        System.err.println(Thread.currentThread().getName()+1 + " Final Downloaded part from: " + startByte + " to " + endByte +"  [real position("+ (startByte+1) +")]");   
                    }
                    */
                }
            } catch (IOException e) {
                e.printStackTrace();
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
        new Client();
    }
}