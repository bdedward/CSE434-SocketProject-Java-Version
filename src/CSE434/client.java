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

            System.out.println("start");
            String inp = null;
            if(sc.hasNextLine()) {
                inp = sc.nextLine();
                token = tokenize(inp);

                if(token[0].equals("register")){
                    //Convert string to bytes and send to server
                    buf = inp.getBytes();

                    //Send message to server
                    sendToServer(buf, ip, ds);

                    //Receive message from server
                    DpReceive = recvFServer(ds, receive);


//                    SocketAddress address = new InetSocketAddress(token[2],Integer.parseInt(token[3]));
                    p2p = new DatagramSocket(Integer.parseInt(token[3]), InetAddress.getByName(token[2]));
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


            System.out.println(p2p.getSoTimeout());
            p2p.setSoTimeout(100);
            try {
                DpReceive.setData(receive, 0, receive.length);
                p2p.receive(DpReceive);
                if(!data(receive).toString().isEmpty()) {
                    token = tokenize(data(receive).toString());
                    System.out.println(data(receive));
                }
            } catch(IOException e) {
//                continue;
            }

            System.out.println("NonBlock");


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

    static DatagramPacket recvFClient(DatagramSocket p2p, byte[] receive) throws IOException {
        DatagramPacket DpReceive = new DatagramPacket(receive, receive.length);
        p2p.receive(DpReceive);
        DpReceive.setData(receive, 0, DpReceive.getLength());
        String returnString = data(receive).toString();
        System.out.println("Client:- " + returnString);

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

    static void setUserId(DatagramSocket p2p, dht d, byte[] receive, ArrayList<ring> userRing) throws IOException {
        ring n = new ring();

        n.identifier = 0;
        n.ip_addr = d.n.get(0).ip_addr;
        n.port = d.n.get(0).port;

        userRing.add(n);

        byte[] buf = null;

        String setIdMessage;

        for(int i = 1; i < 2; i++){
            n.identifier = i;
            n.ip_addr = d.n.get(i).ip_addr;
            n.port = d.n.get(i).port;

            userRing.add(n);
        }

        for(int i = 1; i < userRing.size(); i++){
            setIdMessage =  userRing.get(i-1).identifier + " " +
                            userRing.get(i-1).ip_addr + " " +
                            userRing.get(i-1).port;

            buf = setIdMessage.getBytes();
            DatagramPacket p2pSend =
                            new DatagramPacket(
                                    buf,
                                    buf.length,
                                    InetAddress.getByName(userRing.get(i).ip_addr),
                                    userRing.get(i).port
                            );
            System.out.println(p2pSend.getAddress() + " " + InetAddress.getByName(userRing.get(i).ip_addr));
            System.out.println(p2pSend.getPort() + " " + userRing.get(i).port);
            System.out.println(p2pSend.getSocketAddress());
                    p2p.send(p2pSend);

        }

        for(int i = 0; i < userRing.size(); i++) {
            System.out.println(userRing.get(i).identifier);
        }

    }
}