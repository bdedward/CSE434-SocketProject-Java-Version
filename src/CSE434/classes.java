package CSE434;

import java.util.ArrayList;

class user {
    public String message;
    public String username;
    public String ip_addr;
    public String state;
    public int port;
    public record userRecord;
    public int identifier;
}

class ring {
    public int identifier;
    public String ip_addr;
    public int port;
}

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

class dht{
    public boolean dhtCheck;
    public int nUsers;
    public String leader;
    public ArrayList<user> n = new ArrayList<>();
    public String message;

}


