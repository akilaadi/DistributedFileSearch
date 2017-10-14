package lk.uom.dc;

import java.util.HashMap;
import java.util.List;

public class Neighbour {

    private String ip;
    private int port;
    private String username;
    private List<String> fileList;
    private HashMap<String,String> messageList;
    private HashMap<String,String> routingTable;



    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getFileList() {
        return fileList;
    }

    public void setFileList(List<String> fileList) {
        this.fileList = fileList;
    }

    public HashMap<String,String > getMessageList() {
        return messageList;
    }

    public void setMessageList(HashMap<String,String> messageList) {
        this.messageList = messageList;
    }

    public HashMap<String, String> getRoutingTable() {
        return routingTable;
    }

    public void setRoutingTable(HashMap<String, String> routingTable) {
        this.routingTable = routingTable;
    }


    //

    private  void Reg(){
//        this.ip
    }

    private void UnReg(){

    }

    private void join(){

    }

    private void leave(){
    }

    private Neighbour SearchOk(){


        return new Neighbour();
    }

    private int Error(){

        return  -1;
    }

    private void Forward(Neighbour neighbour){

    }

    private boolean isNeighbourAlive(){

        return  true;
    }


}

