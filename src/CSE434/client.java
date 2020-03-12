package CSE434;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class client
{
    public static void main(String args[]) throws IOException
    {
        Scanner sc = new Scanner(System.in);

        // Step 1:Create the socket object for
        // carrying the data.
        DatagramSocket ds = new DatagramSocket();

        InetAddress ip = InetAddress.getLocalHost();
        byte buf[] = null;
        byte[] receive = new byte[65535];
        DatagramPacket DpReceive = null;
        String[] token;
        String recStr = "";
        ArrayList<ring> userRing = new ArrayList<>();
        DatagramSocket p2p = new DatagramSocket();
        p2p.setSoTimeout(100);

        // loop while user not enters "bye"
        while (true)
        {
            String inp = null;
            if(sc.hasNextLine()){
                inp = sc.nextLine();
                token = tokenize(inp);

                if(token[0].equals("register")){
                    //Convert string to bytes and send to server
                    buf = inp.getBytes();

                    //Send message to server
                    sendToServer(buf, ip, ds);

                    //Receive message from server
                    DpReceive = recvFServer(ds, receive);


                    SocketAddress address = new InetSocketAddress(token[2],Integer.parseInt(token[3]));
                    p2p = new DatagramSocket(address);
                    p2p.setSoTimeout(100);

//
//                    System.out.println(p2p.getLocalSocketAddress());
//
//                    buf = "hello there".getBytes();
//                    DatagramPacket p2pSend =
//                            new DatagramPacket(buf, buf.length, InetAddress.getByName(token[2]), 4001);
//                    p2p.send(p2pSend);
//                    p2p.receive(DpReceive);
//                    System.out.println("client:- " + data(receive));
//                    receive = new byte[65535];

                }
                if(token[0].equals("setup-dht")) {
                    buf = inp.getBytes();

                    sendToServer(buf, ip, ds);


                    DpReceive.setData(receive,0,receive.length);
                    ds.receive(DpReceive);
                    System.out.println(data(receive));

                    token = tokenize(data(receive).toString());
                    if(token[0].equals("Success")) {

                        receive = new byte[65535];

                        DpReceive.setData(receive, 0, receive.length);
                        ds.receive(DpReceive);
                        System.out.println(data(receive));

                        dht d = new dht();
                        token = tokenize(data(receive).toString());
                        d.dhtCheck = Boolean.parseBoolean(token[0]);
                        d.nUsers = Integer.parseInt(token[1]);
                        d.leader = token[2];

                        receive = new byte[65535];

                        d = setupDht(ds, receive, DpReceive, token);
                        setUserId(p2p, d, receive, userRing);
                    }
                    else{
                        System.out.println(data(receive));
                        receive = new byte[65536];
                    }
                }
            }

            try {
                p2p.receive(DpReceive);
                if(!data(receive).toString().isEmpty()) {
                    token = tokenize(data(receive).toString());
                }
            } catch(IOException e) {
                continue;
            }




            // break the loop if user enters "bye"
            if (inp.equals("bye"))
                break;
        }
    }
    public static StringBuilder data(byte[] a)    {
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

    static void sendToServer(byte[] a, InetAddress ip, DatagramSocket ds) throws IOException {
        DatagramPacket DpSend =
                new DatagramPacket(a, a.length, ip, 1234);

        ds.send(DpSend);
    }

    static DatagramPacket recvFServer(DatagramSocket ds, byte[] receive) throws IOException {
        DatagramPacket DpReceive = new DatagramPacket(receive, receive.length);
        ds.receive(DpReceive);
        DpReceive.setData(receive, 0, DpReceive.getLength());
        String returnString = data(receive).toString();
        System.out.println("Server:- " + returnString);

        receive = new byte[65535];

        return DpReceive;
    }

    static dht setupDht(DatagramSocket ds, byte[] receive, DatagramPacket DpReceive, String token[]) throws IOException {
        dht d = new dht();

        boolean check = true;
        while(check){

            String tokenUser[];
            DpReceive = recvFServer(ds, receive);

            tokenUser = tokenize(data(DpReceive.getData()).toString());

            if(!tokenUser[0].equals("done")) {
                user temp = new user();
                temp.username = tokenUser[0];
                temp.ip_addr = tokenUser[1];
                temp.state = tokenUser[2];
                temp.port = Integer.parseInt(tokenUser[3]);
                d.n.add(temp);
            }
            if(tokenUser[0].equals("done")){
                check = false;
            }

            receive = new byte[65535];
        }

        return d;
    }

    static void setUserId(DatagramSocket p2p, dht d, byte[] receive, ArrayList<ring> userRing){
        userRing.get(0).identifier = 0;
        userRing.get(0).ip_addr = d.n.get(0).ip_addr;
        userRing.get(0).port = d.n.get(0).port;

        for(int i = 1; i < d.nUsers; i++){
            userRing.get(i).identifier = i;
            userRing.get(i).ip_addr = d.n.get(i).ip_addr;
            userRing.get(i).port = d.n.get(i).port;
        }

        String setIdMessage = "";
    }
}