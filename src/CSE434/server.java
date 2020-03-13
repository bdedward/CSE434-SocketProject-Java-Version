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
        boolean dhtInitiated = false;

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
                    message = "User Already Exist";
                }
                else
                    message = "success";
                buf = message.getBytes();

                // Send buf to client
                sendToClient(buf, ip, ds, port);
            }
            else if(token[0].equals("setup-dht")){
                if(!dhtInitiated) {
                    String message;
                    int check = setupDht(users, token, d);
                    if (check == -3) {
                        message = "Failure: You need at least two users";
                    } else if (check == -2) {
                        message = "Failure: User does not exist, please use another user for leader";
                    } else {
                        message = "Success in setting up DHT";
                        dhtInitiated = true;
                    }
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
                }
                else {
                    d.message = "Failure: Dht has been initiated by someone else";
                    buf = d.message.getBytes();
                    sendToClient(buf, ip, ds, port);
                }
            }
            else if(token[0].equals("dht-complete")){
                if(token[1].equals(d.leader)){
                    d.dhtCheck = true;
                    buf = "Success: DHT is complete".getBytes();
                    sendToClient(buf, ip, ds, port);
                }
                else{
                    buf = "Failure: You are not the leader".getBytes();
                    sendToClient(buf, ip, ds, port);
                }
            }
            else if(token[0].equals("query-dht")){
                //random index between 0 and nUsers-1
                Random rand = new Random();
                int rand_index = rand.nextInt(d.nUsers);

                String queryResponse = "Success " + d.n.get(rand_index).identifier + " " + d.n.get(rand_index).ip_addr +
                        " " + d.n.get(rand_index).port;

                buf = queryResponse.getBytes();
                sendToClient(buf, ip, ds, port);

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

        for(int i = 0; i <= users.size() - 1; i++) {
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
        for(i = 0; i < users.size() - 1; i++){
            if(users.get(i).state.equals("Free")){
                users.get(i).state = "InDHT";
                users.get(i).identifier = i;
            }
            if(token[2].equals(users.get(i).username)){
                users.get(i).state = "Leader";
                indexOfLeader = i;
                d.leader = users.get(i).username;
                users.get(i).identifier = 0;
            }
            else {
                return -2;
            }

        }
        d.nUsers = i + 1;
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