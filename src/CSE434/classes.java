package CSE434;

import java.util.ArrayList;

// User information used by server
class user {
    public String username;
    public String ip_addr;
    public String state;
    public int port;
    public int identifier;
}

// Used to create the logical ring
class ring {
    public int identifier;
    public String ip_addr;
    public int port;
}

// Hold information about the records
class record {
    public String countrycode;
    public String shortName;
    public String tableName;
    public String longName;
    public String twoAlphaCode;
    public String currency;
    public String region;
    public String wbTwoCode;
    public int ltPopCen;
}

//Holds information about the DHT
class dht{
    public boolean dhtCheck;
    public int nUsers;
    public String leader;
    public ArrayList<user> n = new ArrayList<>();
    public String message;

}


