package CS271_Assignment01_usingFormula;


import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public class Client {
    public static Clock current_sim_time= new Clock();
    public static LinkedList<Node> buffer = new LinkedList<>();
    public static LinkedList<Node> blockChain = new LinkedList<>();
    public static int tao=5;
    public static int delta=20; //[drift of all clock is by delta]
    public static double rho= 0.5; //[We want to ensure no clocks differ more than rho]
    public static Function function=new Function();
    public static FileWriter myWriter;
    public static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss z");
    public static Date date;
    //public static int interrupt=667;
    public static int current_sys_time=0;
    public static int sys_time_at_sync=0;
    public static Clock sim_time_at_Sync;
    public static int interrupt=0;
    //public static Clock sim_time_at_sync=new Clock();
    public static void main(String[] args) {

        try{
            InetAddress ip = InetAddress.getByName("localhost");
            System.out.println(ip);
            Socket socket= new Socket("127.0.0.1", 6666);
            Socket socketWithCentral = new Socket("127.0.0.1",6767);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream outCentral = new ObjectOutputStream(socketWithCentral.getOutputStream());
            ObjectInputStream inCentral = new ObjectInputStream(socketWithCentral.getInputStream());
            String msg= (String) inCentral.readObject();
            System.out.println("You are client/Process Number "+msg);
            int clientId= Integer.parseInt(msg);
            String fileName = "LogOfClient_"+clientId+".txt";
            myWriter= new FileWriter(fileName);
            SimpleDateFormat formatterSecond = new SimpleDateFormat("ss");
            SimpleDateFormat formatterMinute = new SimpleDateFormat("mm");
            SimpleDateFormat formatterHour = new SimpleDateFormat("HH");
            Client.date = new Date(System.currentTimeMillis());
            Client.current_sim_time.second = Long.parseLong(formatterSecond.format(Client.date));
            Client.current_sim_time.minute = Long.parseLong(formatterMinute.format(Client.date));
            Client.current_sim_time.hour = Long.parseLong(formatterHour.format(Client.date));
            System.out.println("Initial Time: "+ formatter.format(Client.date));
            Client.sim_time_at_Sync=new Clock(Client.current_sim_time.hour,Client.current_sim_time.minute,Client.current_sim_time.second);
            Client.function.calculateInterrupt();
            new BlockChainThread(socketWithCentral, clientId,outCentral,inCentral);
            new ClockSyncThread(socket,in,out,clientId);
            new ListenThread(socketWithCentral,inCentral,outCentral);
            while(true)
            {
                Thread.sleep(1000);
                Client.current_sys_time++;
                //System.out.println(Client.current_sys_time);
            }
        }catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }


    }

}

class ClockSyncThread implements Runnable
{
    Socket socket;
    ObjectInputStream in;
    ObjectOutputStream out;
    Thread t;
    int clientId;
    ClockSyncThread(Socket socket, ObjectInputStream in, ObjectOutputStream out, int clientId)
    {
        t= new Thread(this);
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.clientId = clientId;
        t.start();
    }
    public void run()
    {
        try {
            while(true)
            {

                Client.function.calculateCurrentSimulatedTime();
                Clock sentTime = new Clock(Client.current_sim_time);
                Client.myWriter.write("Message sent to Time Server\n");
                Client.myWriter.write("Sent time "+Client.current_sim_time.hour+":"+Client.current_sim_time.minute+":"+Client.current_sim_time.second+"\n");
                Random ran = new Random();
                int randomTao = ran.nextInt(Client.tao-1)+1;
                Client.myWriter.write("Network delay "+randomTao+"\n");
                Thread.sleep(randomTao*Client.interrupt);
                out.writeObject("What is the time?");
                //System.out.println("Client Local Time "+ Client.time);
                String msg= (String) in.readObject();
               // System.out.println("Time is "+ msg);
                String[] arrayOfmsg= msg.split(" ");
                Clock UTCtime=new Clock(Long.parseLong(arrayOfmsg[0]),Long.parseLong(arrayOfmsg[1]),Long.parseLong(arrayOfmsg[2]));
                Client.myWriter.write("UTC time "+UTCtime.hour+":"+UTCtime.minute+":"+UTCtime.second+"\n");
                Client.date = new Date(System.currentTimeMillis());
                Client.function.calculateCurrentSimulatedTime();
                Client.myWriter.write("Current Clock Time: "+ Client.current_sim_time.hour+":"+Client.current_sim_time.minute+":"+Client.current_sim_time.second+": Actual :"+ Client.formatter.format(Client.date)+"\n");
                //Client.sys_time_at_sync= Client.current_sys_time;
                Client.sim_time_at_Sync = new Clock(Client.function.calculateChristianTime(sentTime,UTCtime));
                Client.myWriter.write("Updated Clock Time: "+ Client.sim_time_at_Sync.hour+":"+Client.sim_time_at_Sync.minute+":"+Client.sim_time_at_Sync.second+"\n");
                Client.function.calculateCurrentSimulatedTime();
                System.out.println("====================================");
                System.out.print("Sent ");
                Client.function.printClockTime(sentTime);
                System.out.println("Network delay "+randomTao);
                System.out.print("Received from Server ");
                Client.function.printClockTime(UTCtime);
                System.out.print("Updated Time ");
                Client.function.printClockTime(Client.sim_time_at_Sync);
                System.out.println("Actual :"+ Client.formatter.format(Client.date));
                System.out.println("===================================="+"\n");
                int sleepTime = (int)(Client.delta/ (2*Client.rho));
                Thread.sleep(Client.interrupt*sleepTime);
                //System.out.println(Client.current_sim_time.hour+":"+Client.current_sim_time.minute+":"+Client.current_sim_time.second);
                //Clock newTime=Client.function.addWaitingTime(sleepTime);
                //System.out.println(newTime.hour+":"+newTime.minute+":"+newTime.second);
                //System.out.println("Client system time "+Client.current_sys_time);
                //Scanner s=new Scanner(System.in);
               /* while (newTime.hour!=Client.clock.hour||newTime.minute!=Client.clock.minute||newTime.second!=Client.clock.second)
                {
                    Client.function.calculateCurrentSimulatedTime();
                    System.out.println("In loop: "+Client.clock.hour+":"+Client.clock.minute+":"+Client.clock.second);
                    System.out.println(newTime.hour+":"+newTime.minute+":"+newTime.second);
                    //s.nextInt();
                    if(newTime.second==Client.clock.second)
                        break;
                }*/
            }

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
class BlockChainThread implements Runnable
{

    Socket socket;
    Node newNode = new Node();
    int clientId;
    Thread t;
    ObjectOutputStream outCentral;
    ObjectInputStream inCentral;
    BlockChainThread(Socket socket, int clientId,ObjectOutputStream outCentral,ObjectInputStream inCentral)
    {
        t=new Thread(this);
        this.socket= socket;
        newNode.receiver=clientId;
        Client.buffer.add(newNode);
        Client.blockChain.add(newNode);
        this.clientId=clientId;
        this.inCentral=inCentral;
        this.outCentral=outCentral;
        t.start();
    }
    public void run()
    {
        Scanner s= new Scanner(System.in);
        while(true)
        {
            String request = s.next();
            System.out.println(request);
            if(request.equals("Balance"))
            {
                Client.function.calculateCurrentSimulatedTime();
                Clock currentTime = new Clock(Client.current_sim_time);
                System.out.println("In check balance "+currentTime.hour+":"+currentTime.minute+":"+currentTime.second+"\n");
                //Have to add waiting time
                try {
                    Thread.sleep((Client.delta+Client.tao)*Client.interrupt);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Collections.sort(Client.buffer);
                //System.out.println("Buffer after sorting------------------------------");
                //Client.function.printLocalBuffer();
                //From buffer, the values will be put to Local chain
                Client.function.sortByTimestampNmove(currentTime);
                /*System.out.println("Buffer------------------------------");
                Client.function.printLocalBuffer();
                System.out.println("BlockChain------------------------------");
                Client.function.printBlockChain();
                System.out.println("Length "+ Client.blockChain.size());*/
                //It will check the localChain to calculate not the buffer
                int totalBalance = Client.function.calculateTotalBalance(clientId);
                System.out.println("Total Balance is "+totalBalance);
            }
            else if(request.equals("Transfer"))
            {

                System.out.println("In money transfer, Who is the sender");
                int sender = s.nextInt();
                System.out.println("Who is the receiver?");
                int receiver = s.nextInt();
                System.out.println("What is the amount?");
                int amount = s.nextInt();
                Client.function.calculateCurrentSimulatedTime();
                Clock currentTime = new Clock(Client.current_sim_time);
                System.out.println("TimeStamp: "+currentTime.hour+":"+currentTime.minute+":"+currentTime.second);
                //int timeStamp = 0; // update the timeStamp using TimerServer
                int totalBalance = Client.function.calculateTotalBalance(clientId);
                if(amount<0 || amount>totalBalance || sender!= clientId)
                {
                    System.out.println("INCORRECT");
                }
                else
                {
                    System.out.println("SUCCESS");
                    //broadcast with timestamp
                    String msgToSend = sender+" "+receiver+" "+amount+" "+Client.current_sim_time.hour+" "+Client.current_sim_time.minute+" "+Client.current_sim_time.second;
                    Random ran = new Random();
                    int randomTao = ran.nextInt(Client.tao-1)+1;
                    System.out.println("Network delay to transfer message "+randomTao+" sec");
                   // long estimatedTime = Client.time + randomTao;
                   // System.out.println("CurrentTime "+Client.clock.hour+":"+Client.clock.minute+":"+Client.clock.second +" Estimated Time "+estimatedTime);

                    try {
                        Client.myWriter.write("Current Balance before sending "+totalBalance+"\n");
                        Client.myWriter.write("Current Balance after sending "+(totalBalance-amount)+"\n");
                        Client.myWriter.write("Message sent to client "+receiver+"\n");
                        Thread.sleep(randomTao*Client.interrupt);
                        outCentral.writeObject(msgToSend);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    Node n= new Node(sender, receiver, amount, currentTime);
                    Client.buffer.add(n);
                    try {
                        Thread.sleep((Client.delta+Client.tao)*Client.interrupt);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //sort the buffer on the basis of timeStamp
                    Collections.sort(Client.buffer);
                    //System.out.println("Buffer after sorting------------------------------");
                    //Client.function.printLocalBuffer();
                    /*for(Node node:Client.buffer)
                    {
                        System.out.println("Time "+node.timeStamp.hour+":"+node.timeStamp.minute+":"+node.timeStamp.second+" sender"+node.sender);
                    }*/
                    //Move the transactions to  local BlockChain
                    Client.function.sortByTimestampNmove(currentTime);
                    //System.out.println("Buffer------------------------------");
                    //Client.function.printLocalBuffer();
                    //System.out.println("BlockChain------------------------------");
                    //Client.function.printBlockChain();
                    System.out.println("Message is sent to others successfully....");

                }



            }
            else if(request.equals("print"))
            {
                System.out.println("---------------------------");
                System.out.println("Local Buffer");
                Client.function.printLocalBuffer();
                System.out.println("Block Chain");
                Client.function.printBlockChain();
                System.out.println("---------------------------");
            }
            else if(request.equals("exit"))
            {
                try {
                    Client.myWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}

class Node implements Comparable<Node>{
//class Node {
    int sender;
    int receiver;
    int amount;
    Clock timeStamp;
    Node()
    {
        amount=10;
        timeStamp=new Clock();
    }
    Node(int sender, int receiver, int amount, Clock timeStamp)
    {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.timeStamp = timeStamp;
    }

   public int compareTo(Node n) {
        if(n.timeStamp.hour==this.timeStamp.hour)
        {
            if(n.timeStamp.minute==this.timeStamp.minute)
            {
                if(n.timeStamp.second>this.timeStamp.second)
                    return -1;
                else if(n.timeStamp.second==this.timeStamp.second)
                {
                    if(n.sender>this.sender)
                        return -1;
                }
                return 1;
            }
            else if(n.timeStamp.minute>this.timeStamp.minute)
                return -1;
            return 1;
        }
        else if(n.timeStamp.hour>this.timeStamp.hour)
            return -1;
        return 1;
    }
}
class ListenThread  implements Runnable
{
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    Thread t;
    ListenThread(Socket socket, ObjectInputStream in, ObjectOutputStream out)
    {
        t=new Thread(this);
        this.socket=socket;
        this.out = out;
        this.in = in;
        t.start();
    }
    public void run()
    {
        String msg = null;
        try {
            while (true)
            {
                msg = (String) in.readObject();
                //System.out.println("Message is "+msg);
                String[] arrayOfmsg= msg.split(" ");
                int sender=Integer.parseInt(arrayOfmsg[0]);
                int receiver=Integer.parseInt(arrayOfmsg[1]);
                int amount=Integer.parseInt(arrayOfmsg[2]);
                int timeStampHour=Integer.parseInt(arrayOfmsg[3]);
                int timeStampMinute=Integer.parseInt(arrayOfmsg[4]);
                int timeStampSecond=Integer.parseInt(arrayOfmsg[5]);
                System.out.println("Message is: S "+sender+" R "+receiver+" A "+amount+" T "+timeStampHour+":"+timeStampMinute+":"+timeStampSecond);
                Clock timeStamp=new Clock(timeStampHour,timeStampMinute,timeStampSecond);
                Client.myWriter.write("Message received from client "+sender+"\n");
                Node n=new Node(sender,receiver,amount,timeStamp);
                Client.buffer.add(n);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println(msg);

    }
}
class Function{
    public void sortByTimestampNmove(Clock time)
    {
        int lenBlockChain = Client.blockChain.size();
        //System.out.println("Current time "+time.hour+":"+time.minute+":"+":"+time.second+" len "+ lenBlockChain);
        int check =0;
        for(Node n:Client.buffer)
        {
            if(check != lenBlockChain){
                check++;
                //continue;
            }
            else if(n.timeStamp.hour<time.hour||(n.timeStamp.hour==time.hour&&(n.timeStamp.minute<time.minute))||(n.timeStamp.hour==time.hour&&n.timeStamp.minute==time.minute&&n.timeStamp.second<time.second))
                Client.blockChain.add(n);

        }
       // System.out.println("Len after "+Client.blockChain.size());
    }
    public int calculateTotalBalance(int clientId)
    {
        int totalBalance=0;
        for(Node n:Client.blockChain)
        {
            if(n.receiver==clientId)
                totalBalance = totalBalance + n.amount;
            else if(n.sender==clientId)
                totalBalance = totalBalance - n.amount;
        }
        return totalBalance;
    }
    public Clock calculateChristianTime(Clock sentTime,Clock UTCtime)
    {
        long roundTrip;
        Clock currentEstimatedTime=new Clock();
        Client.function.calculateCurrentSimulatedTime();
        //System.out.print("Received time by client ");
        //Client.function.printClockTime(Client.current_sim_time);
        Client.sys_time_at_sync=Client.current_sys_time;
        if(sentTime.hour==Client.current_sim_time.hour)
        {
            currentEstimatedTime.hour=UTCtime.hour;
            if(sentTime.minute==Client.current_sim_time.minute)
            {
                roundTrip=Client.current_sim_time.second-sentTime.second;
                //currentEstimatedTime.minute=UTCtime.minute;
                //currentEstimatedTime.second= UTCtime.second+(roundTrip.second/2);
            }
            else {
                long minuteDiff=Client.current_sim_time.minute-sentTime.minute;
                roundTrip=((minuteDiff*60)+Client.current_sim_time.second)-sentTime.second;
            }

            //currentEstimatedTime.second= UTCtime.second+(roundTrip.second/2);
        }
        else
        {
            long hourDiff=Client.current_sim_time.hour-sentTime.hour;
            long minuteDiff=((hourDiff*60)+Client.current_sim_time.minute)-sentTime.minute;
            roundTrip=((minuteDiff*60)+Client.current_sim_time.second)-sentTime.second;
        }
        long roundTripBytwo = roundTrip/2;
        currentEstimatedTime.hour=UTCtime.hour;
        currentEstimatedTime.minute=UTCtime.minute;
        currentEstimatedTime.second=UTCtime.second+roundTripBytwo;
        if(currentEstimatedTime.second>=60)
        {
            currentEstimatedTime.second=(UTCtime.second+roundTripBytwo)%60;
            currentEstimatedTime.minute=(UTCtime.second+roundTripBytwo)/60+UTCtime.minute;
            if(currentEstimatedTime.minute>=60)
            {
                long saveMinute=currentEstimatedTime.minute;
                currentEstimatedTime.minute=currentEstimatedTime.minute%60;
                currentEstimatedTime.hour=currentEstimatedTime.hour+(saveMinute/60);
            }

        }
        return currentEstimatedTime;

    }
    public void calculateCurrentSimulatedTime()
    {
        int diff = (int)((Client.current_sys_time-Client.sys_time_at_sync)*(1+Client.rho));
        //System.out.println("current "+Client.current_sys_time+" system "+Client.sys_time_at_sync+" diff "+diff);
        Client.current_sim_time.second = Client.sim_time_at_Sync.second+diff;
        if(Client.current_sim_time.second>=60)
        {
            long save= Client.current_sim_time.second;
            Client.current_sim_time.second=(save%60);
            Client.current_sim_time.minute=Client.sim_time_at_Sync.minute+(save/60);
            if(Client.current_sim_time.minute>=60)
            {
                save=Client.current_sim_time.minute;
                Client.current_sim_time.minute=(save%60);
                Client.current_sim_time.hour=Client.sim_time_at_Sync.hour+(save/60);
            }
        }
    }
    public Clock addWaitingTime(long time)
    {
        Clock newTime=new Clock(Client.current_sim_time.hour,Client.current_sim_time.minute,Client.current_sim_time.second);
        newTime.second= Client.current_sim_time.second+time;
        if(newTime.second>=60)
        {
            long save= newTime.second;
            newTime.second=(save%60);
            newTime.minute=Client.current_sim_time.minute+(save/60);
            if(newTime.minute>=60)
            {
                save=newTime.minute;
                newTime.minute=(save%60);
                newTime.hour=Client.current_sim_time.hour+(save/60);
            }
        }
        return newTime;
    }
    public void calculateInterrupt()
    {
        Client.interrupt = (int)(1000/(1+Client.rho));
    }
    public void printBlockChain()
    {
        for(Node n:Client.blockChain)
        {
            System.out.println("S "+n.sender+" R "+n.receiver+" A "+n.amount+" Time "+n.timeStamp.hour+":"+n.timeStamp.minute+":"+n.timeStamp.second);
        }
    }
    public void printLocalBuffer()
    {
        for(Node n:Client.buffer)
        {
            System.out.println("S "+n.sender+" R "+n.receiver+" A "+n.amount+" Time "+n.timeStamp.hour+":"+n.timeStamp.minute+":"+n.timeStamp.second);
        }
    }
    public void printClockTime(Clock clock)
    {
        System.out.println(clock.hour+":"+clock.minute+":"+clock.second);
    }

}
class Clock
{
    long hour;
    long minute;
    long second;
    Clock()
    {
        hour=0;
        minute=0;
        second=0;
    }
    Clock(long hour,long minute, long second)
    {
        this.hour=hour;
        this.minute=minute;
        this.second=second;
    }
    Clock(Clock clock)
    {
        hour=clock.hour;
        minute=clock.minute;
        second=clock.second;
    }
}