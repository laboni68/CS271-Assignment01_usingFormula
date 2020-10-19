package CS271_Assignment01_usingFormula;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class CentralServer {
    public static void main(String[] args) {
        int clientCount=1;
        ArrayList<Process> processes=new ArrayList<>();
        try {
            ServerSocket serverSocket = new ServerSocket(6767);
            while(true)
            {
                System.out.println("Waiting for the client to connect.....");
                Socket s = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                Process process=new Process(clientCount,s,in,out);
                processes.add(process);
                System.out.println("Client is connected "+clientCount);
                out.writeObject(String.valueOf(clientCount));
                new MultiClientBroadCastThread(s,clientCount,processes,out,in);
                clientCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
class MultiClientBroadCastThread implements Runnable
{
    Socket socket;
    int clientNumber;
    ArrayList<Process> processes;
    ObjectOutputStream out;
    ObjectInputStream in;
    Thread t;
    MultiClientBroadCastThread(Socket socket, int clientNumber, ArrayList<Process> processes, ObjectOutputStream out, ObjectInputStream in)
    {
        t=new Thread(this);
        this.socket=socket;
        this.clientNumber = clientNumber;
        this.processes = processes;
        this.out = out;
        this.in = in;
        t.start();

    }
    public void run()
    {

        try {
            while(true)
            {
                String msg= (String) in.readObject();
                System.out.println(msg);
                String[] arrayOfmsg= msg.split(" ");
                int sender=Integer.parseInt(arrayOfmsg[0]);
                int receiver=Integer.parseInt(arrayOfmsg[1]);
                int amount=Integer.parseInt(arrayOfmsg[2]);
                int timeStampHour=Integer.parseInt(arrayOfmsg[3]);
                int timeStampMinute=Integer.parseInt(arrayOfmsg[4]);
                int timeStampSecond=Integer.parseInt(arrayOfmsg[5]);
                System.out.println("Sender "+sender+" Receiver "+receiver+" Amount "+amount+" TimeStamp "+timeStampHour+":"+timeStampMinute+":"+timeStampSecond);
                for(int i=0;i<processes.size();i++)
                {
                    System.out.println(processes.get(i).processId);
                    if(processes.get(i).processId==sender)
                            continue;
                    processes.get(i).out.writeObject(msg);

                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
class Process
{
    int processId;
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    Process(int processId, Socket socket,ObjectInputStream in,ObjectOutputStream out)
    {
        this.processId = processId;
        this.socket = socket;
        this.in=in;
        this.out=out;
    }
}
