package CSE434;

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class client
{
    public static void main(String args[]) throws IOException
    {
        ring leftuser = new ring();
        ring rightuser = new ring();
        user myuser = new user();
        ArrayList<record> RecordList = new ArrayList<>();
        int nUsers = 0;
        Scanner sc = new Scanner(System.in);
        // Step 1:Create the socket object for
        // carrying the data.
        DatagramSocket ds = new DatagramSocket();
        System.out.println(InetAddress.getLocalHost());
        InetAddress ip = InetAddress.getByName("192.168.0.97");
        byte buf[] = null;
        byte[] receive = new byte[65535];
        DatagramPacket DpReceive = null;
        String[] token;
        String recStr = "";
        ArrayList<ring> userRing = new ArrayList<>();
        DatagramSocket p2p = new DatagramSocket();
        p2p.setSoTimeout(100);

        // loop while user not enters "bye"
        while (true) {
            System.out.println("start");
            String inp = null;

            if (sc.hasNextLine()) {
                inp = sc.nextLine();
                token = tokenize(inp);

                if (token[0].equals("register")) {
                    //Convert string to bytes and send to server
                    buf = inp.getBytes();

                    //Send message to server
                    sendToServer(buf, ip, ds);

                    //Receive message from server
                    DpReceive = recvFServer(ds, receive);

                    //myuser.
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
                if (token[0].equals("setup-dht")) {
                    buf = inp.getBytes();

                    sendToServer(buf, ip, ds);


                    DpReceive.setData(receive, 0, receive.length);
                    ds.receive(DpReceive);
                    System.out.println(data(receive));

                    token = tokenize(data(receive).toString());
                    if (token[0].equals("Success")) {

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

                        d = setupDht(ds, receive, DpReceive, token, d);
                        setUserId(p2p, d, receive, userRing, RecordList);
                    } else {
                        System.out.println(data(receive));
                        receive = new byte[65536];
                    }
                } else if (token[0].equals("dht-complete")) {
                    String complete = token[0] + " " + token[1];
                    buf = complete.getBytes();
                    sendToServer(buf, ip, ds);

                    DpReceive.setData(receive, 0, receive.length);
                    recvFServer(ds, receive);
                } else if (token[0].equals("query-dht")) {
                    String query = token[0];
                    buf = query.getBytes();
                    sendToServer(buf, ip, ds);

                    DpReceive.setData(receive, 0, receive.length);
                    recvFServer(ds, receive);

                    //Token containing random user from DHT, a 3-tuple
                    String message;
                    token = tokenize(data(receive).toString());
                    if (token[0].equals("Success")) {
                        System.out.println(data(receive));
                        inp = sc.nextLine();
                        message = inp + " " + token[2] + " " + token[3];
                        buf = message.getBytes();
                        DatagramPacket p2pSend =
                                new DatagramPacket(
                                        buf,
                                        buf.length,
                                        InetAddress.getByName(token[2]),
                                        Integer.parseInt(token[3])
                                );
                        p2p.send(p2pSend);
                    } else if (token[0].equals("wait")) {
                        boolean flag = true;
                        DpReceive = new DatagramPacket(receive, 0, receive.length);
                        p2p.setSoTimeout(100);
                        while (flag == true) {
                            try {
                                DpReceive.setData(receive, 0, receive.length);
                                p2p.receive(DpReceive);

                                if (!data(receive).toString().isEmpty()) {
                                    token = tokenize(data(receive).toString());
                                    if (token[0].equals("Ring-Info")) {
                                        setNeighbor(token, leftuser, rightuser, myuser, Integer.parseInt(token[1]));

                                    } else if (token[0].equals("Set-Record")) {
                                        StringTokenizer tokenize = new StringTokenizer(data(receive).toString(), ",");
                                        String tokenComma[] = new String[100];
                                        int i = 0;
                                        while (tokenize.hasMoreElements()) {
                                            tokenComma[i] = tokenize.nextToken();
                                            i++;
                                        }
                                        RecordHandler(tokenComma, rightuser, myuser, RecordList, nUsers, p2p);

                                    } else if (token[0].equals("stop")) {
                                        flag = false;

                                        buf = "stop".getBytes();
                                        DatagramPacket p2pSend =
                                                new DatagramPacket(
                                                        buf,
                                                        buf.length,
                                                        InetAddress.getByName(rightuser.ip_addr),
                                                        rightuser.port
                                                );
                                        p2p.send(p2pSend);
                                    }

                                }
                                receive = new byte[65535];
                            } catch (IOException e) {
//                continue;
                            }
                        }
                    }
                }
            }
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

    static dht setupDht(DatagramSocket ds, byte[] receive, DatagramPacket DpReceive, String token[], dht d) throws IOException {


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

    static void setUserId(DatagramSocket p2p, dht d, byte[] receive,
                          ArrayList<ring> userRing, ArrayList<record> recordList) throws IOException {
        ring n = new ring();

        n.identifier = 0;
        n.ip_addr = d.n.get(0).ip_addr;
        n.port = d.n.get(0).port;

        userRing.add(n);

        byte[] buf = null;

        String setIdMessage;

        for(int i = 1; i < 2; i++){
            ring b = new ring();
            b.identifier = i;
            b.ip_addr = d.n.get(i).ip_addr;
            b.port = d.n.get(i).port;

            userRing.add(b);
        }

        int j = 0;
        int leftUser = 0;
        int rightUser = 0;
        for(int i = 1; i < userRing.size(); i++){
            if(i == userRing.size() - 1){
                rightUser = 0;
            }
            setIdMessage =  "Ring-Info " + d.nUsers + " " +
                            userRing.get(leftUser).identifier + " " +
                            userRing.get(leftUser).ip_addr + " " +
                            userRing.get(leftUser).port + " " +
                            userRing.get(rightUser).identifier + " " +
                            userRing.get(rightUser).ip_addr + " " +
                            userRing.get(rightUser).port;

            buf = setIdMessage.getBytes();
            DatagramPacket p2pSend =
                            new DatagramPacket(
                                    buf,
                                    buf.length,
                                    InetAddress.getByName(userRing.get(i).ip_addr),
                                    userRing.get(i).port
                            );
             p2p.send(p2pSend);


        }

        for(int i = 0; i < userRing.size(); i++) {
            System.out.println(userRing.get(i).identifier);
        }
        LineHashing(p2p, d.nUsers, userRing.get(1), recordList);

    }

    static void setNeighbor(String[] token, ring leftuser, ring rightuser, user myuser, int nUsers){
        nUsers = Integer.parseInt(token[1]);
        leftuser.identifier = Integer.parseInt(token[2]);
        leftuser.ip_addr = token[3];
        leftuser.port = Integer.parseInt(token[4]);
        rightuser.identifier = Integer.parseInt(token[5]);
        rightuser.ip_addr = token[6];
        rightuser.port = Integer.parseInt(token[7]);
        myuser.identifier = Integer.parseInt(token[5]) - 1;
    }

    static void LineHashing(DatagramSocket p2p, int nUsers, ring rightuser, ArrayList<record> recordList) throws IOException {
        byte[] buf = null;

        String recordmessage;
        String delim = ",";

        try (BufferedReader br = new BufferedReader(new FileReader("StatsCountry.csv"))) {

            for (String line; (line = br.readLine()) != null; ) {
                line = br.readLine();
                String inp = line.toString();
                StringTokenizer tokenize = new StringTokenizer(inp, ",");
                String token[] = new String[100];
                record R = new record();
                int i = 0;
                while (tokenize.hasMoreElements()) {
                    token[i] = tokenize.nextToken();
//                    System.out.println(token[i]);
                    i++;
                }
                recordmessage = "Set-Record" + delim + token[0] + delim + token[1] + delim + token[2]
                        + delim + token[3] + delim + token[4] + delim + token[5] + delim + token[6]
                        + delim + token[7] + delim + token[8];

                buf = recordmessage.getBytes();

                int longSum = 0;
                for (int k = 0; k < token[3].length(); k++)
                    longSum += token[3].charAt(k);

                int position = longSum % 353;
                int id = position % nUsers;
                if(id == 0){
                    R.countrycode = token[0];
                    R.shortName = token[1];
                    R.tableName = token[2];
                    R.longName = token[3];
                    R.twoAlphaCode = token[4];
                    R.currency = token[5];
                    R.region = token[6];
                    R.wbTwoCode = token[7];
                    if(token[8] != null) {
                        R.ltPopCen = Integer.parseInt(token[8]);
                    }
                    recordList.add(R);

                }
                else {
                    DatagramPacket p2pSend =
                            new DatagramPacket(
                                    buf,
                                    buf.length,
                                    InetAddress.getByName(rightuser.ip_addr),
                                    rightuser.port
                            );
                    p2p.send(p2pSend);
                }
            }
            // line is not visible here.
        } catch (IOException e) {
            e.printStackTrace();
        }
        buf = "stop".getBytes();
        DatagramPacket p2pSend =
                new DatagramPacket(
                        buf,
                        buf.length,
                        InetAddress.getByName(rightuser.ip_addr),
                        rightuser.port
                );
        p2p.send(p2pSend);

    }

    static void RecordHandler(String[] token, ring rightuser, user myuser,
                              ArrayList<record> RecordList, int nUsers, DatagramSocket p2p) throws IOException {

        nUsers = 2;
        byte[] buf = null;
        int longSum = 0;
        for (int k = 0; k < token[3].length(); k++)
            longSum += token[3].charAt(k);
        record R = new record();
        int position = longSum % 353;
        int id = position % nUsers;

        R.countrycode = token[1];
        R.shortName = token[2];
        R.tableName = token[3];
        R.longName = token[4];
        R.twoAlphaCode = token[5];
        R.currency = token[6];
        R.region = token[7];
        R.wbTwoCode = token[8];
        R.ltPopCen = Integer.parseInt(token[9]);

        if(myuser.identifier == id){
            RecordList.add(R);
        }
        else{
           String message = token.toString();
            buf = message.getBytes();
            DatagramPacket p2pSend =
                    new DatagramPacket(
                            buf,
                            buf.length,
                            InetAddress.getByName(rightuser.ip_addr),
                            rightuser.port
                    );
            p2p.send(p2pSend);
        }
    }
}