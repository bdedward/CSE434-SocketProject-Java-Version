package CSE434;

// Java program to illustrate Server side
// Implementation using DatagramSocket
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Random;

public class server
{
    public static void main(String[] args) throws IOException
    {
        //Global Variables
        // Server socket used to listen for message from client
        //TODO: port should be given from command line, ip should be based on host ip.
        DatagramSocket ds = new DatagramSocket(1234, InetAddress.getByName("192.168.0.236"));
        DatagramPacket DpReceive = null;

        // Buffers for receive and send
        byte[] receive = new byte[65535];
        byte buf[] = null;

        // Arraylist to store the users
        ArrayList<user> users = new ArrayList<>();

        //Other Global variables
        String[] token;
        dht d = new dht();
        d.dhtCheck = false;
        boolean dhtInitiated = false;
        InetAddress queryIpSource;
        int queryPortSource;
        String leaveDhtUName = "";

        while (true)
        {
            //When receive from client get ip and port for response
            DpReceive = recvFClient(ds, receive);
            InetAddress ip = DpReceive.getAddress();
            int port = DpReceive.getPort();



            //Tokenize received message
            token = tokenize(data(receive).toString());


            if(token[0].equals("register")){
                int check = registerUser(users, token);
                String message;
                if(check == -1){
                    message = "Failure User Already Exist";
                }
                else if (check == -2){
                    message = "Failure Username is too long";
                }
                else
                    message = "success";
                buf = message.getBytes();

                // Send buf to client
                sendToClient(buf, ip, ds, port);
            }
            else if(token[0].equals("setup-dht")){
                if(!d.dhtCheck) {

                    for(int i = 0; i < users.size(); i++){
                        if(token[1].equals(users.get(i).username)){
                           user temp;
                           temp = users.get(i);
                           users.set(i, users.get(0));
                           users.set(0,temp);
                        }
                        System.out.println(users.get(i).username + " " + users.get(i).identifier);
                    }

                    String message;
                    int check = setupDht(users, token, d);
                    if (check == -3) {
                        message = "Failure: You need at least two users";
                    } else if (check == -2) {
                        message = "Failure: User does not exist, please use another user for leader";
                    } else {
                        message = "Success in setting up DHT";

                        d.message = message;

                        buf = d.message.getBytes();

                        sendToClient(buf, ip, ds, port);

                        String temp = d.dhtCheck + " " + d.nUsers + " " + d.leader;
                        buf = temp.getBytes();
                        sendToClient(temp.getBytes(), ip, ds, port);

                        for (int j = 0; j < d.n.size(); j++) {
                            String userTuples = d.n.get(j).username + " " +
                                    d.n.get(j).ip_addr + " " +
                                    d.n.get(j).state + " " +
                                    d.n.get(j).port;

                            buf = userTuples.getBytes();
                            System.out.println(userTuples);
                            sendToClient(buf, ip, ds, port);
                        }

                        d.message = "done";
                        buf = d.message.getBytes();
                        sendToClient(buf, ip, ds, port);

                        dhtInitiated = true;
                        while(dhtInitiated){
                            DpReceive = recvFClient(ds,receive);
                            receive = new byte[65535];
                            ip = DpReceive.getAddress();
                            port = DpReceive.getPort();
                            token = tokenize(data(DpReceive.getData()).toString());
                            if((token[0].equals("dht-complete"))){
                                if(token[1].equals(d.leader)){
                                    dhtInitiated = false;
                                    d.dhtCheck = true;
                                    buf = "Success : DHT is complete".getBytes();
                                    sendToClient(buf, ip, ds, port);
                                }
                                else{
                                    buf = "Failure : You are not the leader".getBytes();
                                    sendToClient(buf, ip, ds, port);
                                }
                            }
                            else {
                                message = "Failure waiting for leader to complete dht";
                                buf = message.getBytes();
                                sendToClient(buf,ip,ds,port);
                            }
                        }
                    }

                }
                else {
                    d.message = "Failure: Dht has already been setup by " + d.leader;
                    buf = d.message.getBytes();
                    sendToClient(buf, ip, ds, port);
                }
            }
            else if(token[0].equals("query-dht")){
                //random index between 0 and nUsers-1

                queryIpSource = DpReceive.getAddress();
                queryPortSource = DpReceive.getPort();

                Random rand = new Random();
                int rand_index = rand.nextInt(d.nUsers);

                String queryResponse = "Success " + d.n.get(rand_index).identifier + " " + d.n.get(rand_index).ip_addr +
                        " " + d.n.get(rand_index).port;

                buf = queryResponse.getBytes();
                sendToClient(buf, ip, ds, port);
                receive = new byte[65535];

                DpReceive = recvFClient(ds, receive);
                buf = data(DpReceive.getData()).toString().getBytes();
                sendToClient(buf, queryIpSource, ds, queryPortSource);

            }
            else if(token[0].equals("deregister")){
                String message;
                for(int i = 0; i < users.size(); i++){
                    if(token[1].equals(users.get(i).username)){
                        if(users.get(i).state.equals("Free")){
                            users.remove(i);
                            message = "Success";
                            buf = message.getBytes();
                            sendToClient(buf,ip,ds,port);
                        }
                        else{
                            message = "Failure User is in DHT use leave-dht command";
                            buf = message.getBytes();
                            sendToClient(buf,ip,ds,port);
                        }
                    }
                }
            }
            else if(token[0].equals("leave-dht")) {
                d.nUsers--;
                String message;
                if (d.nUsers < 2) {
                    message = "Failure: There has to be at least two users";
                    buf = message.getBytes();
                    sendToClient(buf, ip, ds, port);
                } else {

                    dhtInitiated = false;

                    for (int i = 0; i < users.size(); i++) {
                        if (leaveDhtUName == users.get(i).username) {
                            users.get(i).state.equals("Free");
                        }
                    }

                    boolean flag = false;
                    for (int i = 0; i < d.nUsers; i++) {
                        if (token[1].equals(d.n.get(i).username)) {
                            flag = true;
                            if ((i + 1) == d.nUsers-1) {
                                message = "Success " + d.n.get(0).username;
                            } else {
                                message = "Success " + d.n.get(i + 1).username;
                            }
                            for(int j = 0; j < users.size(); j++ ){
                                if((users.get(j).state.equals("InDHT")) || (users.get(j).state.equals("Leader"))){
                                    users.get(j).state = "Free";
                                }
                            }
                            buf = message.getBytes();
                            sendToClient(buf, ip, ds, port);
                            i = d.nUsers;
                            leaveDhtUName = token[1];
                        }
                    }
                    d = null;
                    d = new dht();
                    d.dhtCheck = false;
                    if (flag == false) {
                        message = "Failure you are not in DHT";
                        buf = message.getBytes();
                        sendToClient(buf, ip, ds, port);
                    }
                }
            }



            // Clear the buffer after every message.
            receive = new byte[65535];
        }
    }

    // A utility method to convert the byte array
    // data into a string representation.
    public static StringBuilder data(byte[] a) {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0)
        {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }

    //Tokenize the String
    static String[] tokenize(String str){
        StringTokenizer tokenize = new StringTokenizer(str);
        String token[] = new String[100];
        int i = 0;
        while(tokenize.hasMoreElements()){
            token[i] = tokenize.nextToken();
            i++;
        }
        return token;
    }

    // Function to send message to Client
    static void sendToClient(byte[] a, InetAddress ip, DatagramSocket ds, int port) throws IOException {
        DatagramPacket DpSend =
                new DatagramPacket(a, a.length, ip, port);
        ds.send(DpSend);
    }

    // Function to receive from Client
    static DatagramPacket recvFClient(DatagramSocket ds, byte[] receive) throws IOException {
        DatagramPacket DpReceive = new DatagramPacket(receive, receive.length);

        ds.receive(DpReceive);
        System.out.println("Client:- " + data(receive));
        String returnString = data(receive).toString();
        receive = new byte[65535];

        return DpReceive;
    }

    // Function to register a user
    static int registerUser(ArrayList<user> users, String token[]){

        if(token[1].length() > 15){
            return -2;
        }
        for(int i = 0; i < users.size(); i++) {
            if(token[1].equals(users.get(i).username)){
                return -1;
            }
        }
        user n = new user();
        n.username = token[1];
        n.ip_addr = token[2];
        n.port = Integer.parseInt(token[3]);

        n.state = "Free";

        users.add(n);

        return 0;

    };

    // Function to setup DHT
    static int setupDht(ArrayList<user> users, String token[], dht d){
        int indexOfLeader = 0;
        int i = 0;
        boolean flag = false;
        for(i = 0; i < users.size(); i++){
            if(users.get(i).state.equals("Free")){
                users.get(i).state = "InDHT";
                users.get(i).identifier = i;
            }
            System.out.println(i + " " + users.get(i).state + " " + users.get(i).username);
            if(token[2].equals(users.get(i).username)){
                flag = true;
                users.get(i).state = "Leader";
                indexOfLeader = i;
                d.leader = users.get(i).username;
                users.get(i).identifier = 0;
            }

        }
        if(flag == false){
            return -2;
        }
        d.nUsers = i;
        d.n = users;

        if(!token[2].equals(users.get((indexOfLeader)))){
            return -1;
        }
        //Error handling
        if(Integer.parseInt(token[1]) < 2){
            return -3;
        }
        return 0;
    }
}