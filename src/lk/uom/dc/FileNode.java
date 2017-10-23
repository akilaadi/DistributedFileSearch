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

    public void Receive() {
        FileNodeCommand receiveCommand = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                while (true) {
                    String message = this.receive();
                    System.out.println(message);
                    String[] tokens = message.split(" ");
                    if (tokens[1].equals("REGOK") && this.getAddress().equals(FileNode.this.bAddress) && FileNode.this.bPort == this.getPort()) {
                        FileNode.this.neighbours = new ArrayList<Neighbour>();
                        for (int i = 0; i < tokens.length; i += 2) {
                            if (i > 2) {
                                FileNode.this.Join(tokens[i - 1], Integer.parseInt(tokens[i]));
                            }
                        }
                    } else if (tokens[1].equals("JOIN")) {
                        FileNode.this.JoinOk(tokens[2], Integer.parseInt(tokens[3]));

                    } else if(tokens[1].equals("JOINOK")){
                        FileNode.this.neighbours.add(
                                new Neighbour(
                                        this.getPacket().getAddress().getHostAddress(),
                                        this.getPacket().getPort()));
                    }
                }
            }
        };
        receiveCommand.start();
    }

    public void Reg() {
        String query = String.format("REG %1$s %2$d %3$s", this.address, this.port, this.username);
        query = String.format("%1$04d %2$s", query.length() + 5, query);
        FileNodeCommand command = new FileNodeCommand(this.socket);
        command.send(this.bAddress, this.bPort, query);
    }

    public void Join(String address, int port) {
        FileNodeCommand command = new FileNodeCommand(this.socket){
            @Override
            public void run() {
                String query = String.format("JOIN %1$s %2$d", FileNode.this.address, FileNode.this.port);
                query = String.format("%1$04d %2$s", query.length() + 5, query);
                this.send(address, port, query);
            }
        };
        command.start();
    }

    public void JoinOk(String address, int port) {
        FileNodeCommand command = new FileNodeCommand(this.socket){
            @Override
            public void run() {
                String query = String.format("JOINOK 0");
                query = String.format("%1$04d %2$s", query.length() + 5, query);
                this.send(address, port, query);
            }
        };
        command.start();
    }

    public static void main(String[] args) throws UnknownHostException {
        FileNode fileNode = new FileNode(args[0], Integer.parseInt(args[1]), args[2]);
        fileNode.Receive();
        fileNode.Reg();
        System.out.println(String.format("File node started at %1s:%2d", fileNode.getAddress(), fileNode.getPort()));
        System.out.println("Waiting for incoming queries..");
    }
}
