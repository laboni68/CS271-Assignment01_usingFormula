package CS271_Assignment01_usingFormula;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


public class TimeServer {
    public static long UTCsecond=0;
    public static long UTCminute=0;
    public static long UTChour=0;
    public static void main(String[] args) {
        try{
            ServerSocket ss=new ServerSocket(6666);
            int clientCount=1;
            while(true)
            {
                System.out.println("Waiting for the client to connect in time Server.....");
                //new globalClock();
                Socket s = ss.accept();
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                new ClockSync(s,out,in,clientCount);
                clientCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

class ClockSync implements Runnable
{
    ObjectOutputStream out;
    ObjectInputStream in;
    Socket socket;
    int clientId;
    Thread t;
    SimpleDateFormat formatter;
    SimpleDateFormat formatterSecond, formatterMinute,formatterHour;
    Date date;
    ClockSync(Socket socket, ObjectOutputStream out, ObjectInputStream in,int clientId)
    {
        t=new Thread(this);
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.clientId = clientId;
        formatter = new SimpleDateFormat("HH:mm:ss z");
        formatterSecond = new SimpleDateFormat("ss");
        formatterMinute = new SimpleDateFormat("mm");
        formatterHour = new SimpleDateFormat("HH");
        date = new Date(System.currentTimeMillis());
        TimeServer.UTCsecond = Long.parseLong(formatterSecond.format(date));
        t.start();
    }
    public void run()
    {
        try {
            while(true)
            {
                //System.out.println("In loop");
                String msg= (String) in.readObject();
                System.out.println("Message From Client "+ clientId +": "+msg);
                date = new Date(System.currentTimeMillis());
                TimeServer.UTCsecond = Long.parseLong(formatterSecond.format(date));
                TimeServer.UTCminute = Long.parseLong(formatterMinute.format(date));
                TimeServer.UTChour = Long.parseLong(formatterHour.format(date));
                System.out.println(formatter.format(date)+" calculated UTC "+ TimeServer.UTChour+":"+TimeServer.UTCminute+":"+TimeServer.UTCsecond);
                String time=TimeServer.UTChour+" "+TimeServer.UTCminute+" "+TimeServer.UTCsecond;
                Random ran = new Random();
                int randomTao = ran.nextInt(Client.tao-1)+1;
                System.out.println("Client "+clientId+" Network delay "+randomTao);
                Thread.sleep(randomTao*1000);
                out.writeObject(time);
                //System.out.println("Message to Client "+clientId+" "+TimeServer.UTC);

            }

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

