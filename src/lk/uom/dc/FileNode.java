package lk.uom.dc;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class FileNode {
    private String bAddress;
    private int bPort;
    private int port;
    private String address;
    private String username;
    DatagramSocket socket;
    private List<Neighbour> neighbours;

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public FileNode(String bAddress, int bPort, String username) throws UnknownHostException {
        this.bAddress = bAddress;
        this.bPort = bPort;
        this.username = username;
        this.address = InetAddress.getLocalHost().getHostAddress();
        /*Assign a port number to this file node between 3000 and 1023.*/
        this.port = (int) (Math.random() * (3000 - 1023) + 1023);
        try {
            this.socket = new DatagramSocket(this.port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void Register() {
        String query = String.format("REG %1$s %2$d %2$s", this.address, this.port, this.username);
        query = String.format("%1$04d %2$s", query.length() + 5, query);
        FileNodeCommand receiveCommand = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                String message = this.receive();
                String[] tokens = message.split(" ");
                if (tokens[1].equals("REGOK") && this.getAddress().equals(FileNode.this.bAddress) && FileNode.this.bPort == this.getPort()) {
                    FileNode.this.neighbours = new ArrayList<Neighbour>();
                    for(int i = 0; i < tokens.length; i+=2) {
                        if(i > 2){
                            FileNode.this.neighbours.add(new Neighbour(tokens[i-1],Integer.parseInt(tokens[i])));
                        }
                    }
                }else {
                    this.run();
                }
            }
        };
        receiveCommand.start();
        FileNodeCommand command = new FileNodeCommand(this.socket);
        command.send(this.bAddress, this.bPort, query);
    }

    private void Unregister(){

        String quary = String.format("UNREG %1$s %2$d %2$s", this.address, this.port, this.username);

        FileNodeCommand reciveCommand = new FileNodeCommand(this.socket){
            @Override
            public void run() {
                String message = this.receive();
                String[] tokens = message.split(" ");
                if(tokens[1].equals("UNROK") && this.getAddress().equals(FileNode.this.bAddress) && FileNode.this.bPort == this.getPort()) {
                    System.out.println(message);
                }else{
                    this.run();
                }
            }
        };

        reciveCommand.start();
        FileNodeCommand command = new FileNodeCommand(this.socket);
        command.send(this.bAddress, this.bPort, quary);
        
    }

    public void JoinNetwork() {

    }

    public static void main(String[] args) throws UnknownHostException {
        FileNode fileNode = new FileNode(args[0], Integer.parseInt(args[1]), args[2]);
        fileNode.Register();
        System.out.printf("File node started at %1s:%2d", fileNode.getAddress(), fileNode.getPort());
        System.out.println("Waiting for incoming queries..");
    }
}
