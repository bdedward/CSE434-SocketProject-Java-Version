package CSE434;

// Java program to illustrate Server side
// Implementation using DatagramSocket
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class server
{
    public static void main(String[] args) throws IOException
    {
        // Step 1 : Create a socket to listen at port 1234
        DatagramSocket ds = new DatagramSocket(1234, InetAddress.getByName("192.168.0.236"));
        byte[] receive = new byte[65535];
        byte buf[] = null;
        String[] token;
        ArrayList<user> users = new ArrayList<>();

        DatagramPacket DpReceive = null;
        while (true)
        {

            DpReceive = recvFClient(ds, receive);
            InetAddress ip = DpReceive.getAddress();
            int port = DpReceive.getPort();

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
                dht d = new dht();

                String message;
                int check = setupDht(users, token, d);
                if (check == -3) {
                    message = "Failure: You need at least two users";
                } else if (check == -2) {
                    message = "Failure: User does not exist, please use another user for leader";
                } else {
                    message = "Success in setting up DHT";
                    d.dhtCheck = true;
                }
                d.message = message;

                buf = d.message.getBytes();

                sendToClient(buf, ip, ds, port);

                String temp = d.dhtCheck + " " + d.nUsers + " " + d.leader;
                buf = temp.getBytes();
                sendToClient(temp.getBytes(), ip, ds, port);

                for(int j = 0; j < d.n.size(); j++){
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


            // Exit the server if the client sends "bye"
            if (data(receive).toString().equals("bye"))
            {
                System.out.println("Client sent bye.....EXITING");
                break;
            }

            // Clear the buffer after every message.
            receive = new byte[65535];
        }
    }

    // A utility method to convert the byte array
    // data into a string representation.
    public static StringBuilder data(byte[] a)
    {
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

    static void sendToClient(byte[] a, InetAddress ip, DatagramSocket ds, int port) throws IOException {
        DatagramPacket DpSend =
                new DatagramPacket(a, a.length, ip, port);
        ds.send(DpSend);
    }

    static DatagramPacket recvFClient(DatagramSocket ds, byte[] receive) throws IOException {
        DatagramPacket DpReceive = new DatagramPacket(receive, receive.length);

        ds.receive(DpReceive);
        System.out.println("Client:- " + data(receive));
        String returnString = data(receive).toString();
        receive = new byte[65535];

        return DpReceive;
    }

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

    static int setupDht(ArrayList<user> users, String token[], dht d){
        int indexOfLeader = 0;
        int i = 0;
        for(i = 0; i < users.size() - 1; i++){
            if(users.get(i).state.equals("Free")){
                users.get(i).state = "InDHT";
            }
            if(token[2].equals(users.get(i).username)){
                users.get(i).state = "Leader";
                indexOfLeader = i;
                d.leader = users.get(i).username;
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