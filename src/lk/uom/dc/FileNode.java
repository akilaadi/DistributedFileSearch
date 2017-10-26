package lk.uom.dc;

import javafx.util.Pair;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

public class FileNode {
    private String bAddress;
    private int bPort;
    private int port;
    private String address;
    private String username;
    DatagramSocket socket;
    private List<Neighbour> neighbours;
    private List<String> files;
    private List<Pair<String, Neighbour>> messageRoutingHistory;

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public List<String> getFiles() {
        return this.files;
    }

    public List<Neighbour> getNeighbours() {
        return neighbours;
    }

    public FileNode(String bAddress, int bPort, String username) throws UnknownHostException {
        this.bAddress = bAddress;
        this.bPort = bPort;
        this.username = username;
        this.address = InetAddress.getLocalHost().getHostAddress();
        this.messageRoutingHistory = new ArrayList<Pair<String, Neighbour>>();
        this.LoadFiles();
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
                    message = message.replace("\n", "");
                    System.out.println(message);
                    try {
                        String[] tokens = message.split(" ");
                        if (message.equals("RTTBL")) {
                            String replyMessage = "\n";
                            for (int i = 0; i < FileNode.this.neighbours.size(); i++) {
                                replyMessage += String.format("%1$s:%2$d\n", FileNode.this.neighbours.get(i).getIp(), FileNode.this.neighbours.get(i).getPort());
                            }
                            FileNode.this.SendMessage(this.getPacket().getAddress().getHostAddress(), this.getPacket().getPort(), replyMessage);
                        } else if (message.equals("FILES")) {
                            FileNode.this.SendMessage(this.getPacket().getAddress().getHostAddress(), this.getPacket().getPort(), String.join("\n", FileNode.this.getFiles()));
                        } else if (tokens[1].equals("REGOK") && this.getAddress().equals(FileNode.this.bAddress) && FileNode.this.bPort == this.getPort()) {
                            FileNode.this.neighbours = new ArrayList<Neighbour>();
                            for (int i = 0; i < tokens.length; i += 2) {
                                if (i > 2) {
                                    FileNode.this.Join(tokens[i - 1], Integer.parseInt(tokens[i]));
                                }
                            }
                        } else if (tokens[1].equals("UNREG")) {
                            FileNode.this.Unreg();
                        } else if (tokens[1].equals("UNROK") && this.getAddress().equals(FileNode.this.bAddress) && FileNode.this.bPort == this.getPort()) {
                            FileNode.this.Leave();
                        } else if (tokens[1].equals("JOIN")) {
                            FileNode.this.neighbours.add(
                                    new Neighbour(
                                            tokens[2],
                                            Integer.parseInt(tokens[3])));
                            FileNode.this.JoinOk(tokens[2], Integer.parseInt(tokens[3]));

                        } else if (tokens[1].equals("JOINOK")) {
                            FileNode.this.neighbours.add(
                                    new Neighbour(
                                            this.getPacket().getAddress().getHostAddress(),
                                            this.getPacket().getPort()));
                        } else if (tokens[1].equals("LEAVE")) {
                            Iterator<Neighbour> iterator = FileNode.this.neighbours.iterator();
                            while (iterator.hasNext()) {
                                Neighbour current = iterator.next();
                                if (current.getIp().equals(tokens[2])
                                        && current.getPort() == Integer.parseInt(tokens[3])) {
                                    iterator.remove();
                                    break;
                                }
                            }
                            FileNode.this.LeaveOk(tokens[2], Integer.parseInt(tokens[3]));
                        } else if (tokens[1].equals("LEAVEOK")) {
                            Iterator<Neighbour> iterator = FileNode.this.neighbours.iterator();
                            while (iterator.hasNext()) {
                                Neighbour current = iterator.next();
                                if (current.getIp().equals(this.getPacket().getAddress().getHostAddress())
                                        && current.getPort() == this.getPacket().getPort()) {
                                    iterator.remove();
                                    break;
                                }
                            }
                            if (FileNode.this.neighbours.size() == 0) {
                                break;
                            }
                        } else if (tokens[1].equals("SER")) {
                            List<String> matchingFiles;
                            String ip, fileName;
                            int sourcePort;
                            int hopCount;
                            String messageId;
                            if (tokens.length == 7) {
                                messageId = tokens[6];
                            } else {
                                messageId = UUID.randomUUID().toString();
                            }
                            if (tokens.length > 3) {
                                fileName = tokens[4];
                                matchingFiles = FileNode.this.SearchFile(fileName);
                                ip = tokens[2];
                                sourcePort = Integer.parseInt(tokens[3]);
                                hopCount = Integer.parseInt(tokens[5]);
                            } else {
                                fileName = tokens[2];
                                matchingFiles = FileNode.this.SearchFile(fileName);
                                ip = this.getPacket().getAddress().getHostAddress();
                                sourcePort = this.getPort();
                                hopCount = 10;
                            }

                            FileNode.this.messageRoutingHistory.add(new Pair<String, Neighbour>(messageId, new Neighbour(this.getPacket().getAddress().getHostAddress(), this.getPacket().getPort())));

                            if (matchingFiles.size() > 0 || hopCount == 1) {
                                FileNode.this.SearchOK(ip, sourcePort, hopCount - 1, matchingFiles);
                            }

                            if (hopCount > 1) {
                                FileNode.this.Search(ip, sourcePort, fileName, hopCount - 1, messageId);
                            }
                        }

                    } catch (Exception ex) {
                        FileNode.this.Error(this.getPacket().getAddress().getHostAddress(), this.getPacket().getPort());
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

    public void Unreg() {
        FileNodeCommand command = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                String query = String.format("UNREG %1$s %2$d %3$s", FileNode.this.address, FileNode.this.port, FileNode.this.username);
                query = String.format("%1$04d %2$s", query.length() + 5, query);
                this.send(FileNode.this.bAddress, FileNode.this.bPort, query);
            }
        };
        command.start();
    }

    public void Join(String address, int port) {
        FileNodeCommand command = new FileNodeCommand(this.socket) {
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
        FileNodeCommand command = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                String query = String.format("JOINOK 0");
                query = String.format("%1$04d %2$s", query.length() + 5, query);
                this.send(address, port, query);
            }
        };
        command.start();
    }

    public void Leave() {
        FileNodeCommand command = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                String query = String.format("LEAVE %1$s %2$d", FileNode.this.address, FileNode.this.port);
                query = String.format("%1$04d %2$s", query.length() + 5, query);
                for (int i = 0; i < FileNode.this.neighbours.size(); i++) {
                    this.send(FileNode.this.neighbours.get(i).getIp(), FileNode.this.neighbours.get(i).getPort(), query);
                }
            }
        };
        command.start();
    }

    public void LeaveOk(String address, int port) {
        FileNodeCommand command = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                String query = String.format("LEAVEOK  0");
                query = String.format("%1$04d %2$s", query.length() + 5, query);
                this.send(address, port, query);
            }
        };
        command.start();
    }

    public void Search(String address, int port, String fileName, int hopCount, String messageId) {
        FileNodeCommand command = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                String query = String.format("SER %1$s %2$d %3$s %4$d %5$s", address, port, fileName, hopCount, messageId);
                query = String.format("%1$04d %2$s", query.length() + 5, query);

                Iterator<Neighbour> neighbourIterator = FileNode.this.neighbours.iterator();
                Iterator<Pair<String, Neighbour>> routingHistoryIterator;
                Neighbour currentNeighbour = FileNode.this.neighbours.get(0);
                Pair<String, Neighbour> currentRoutingHistory;
                boolean hasAlreadyRouted = false;
                int iteratedNeighbors = 0;
                while (neighbourIterator.hasNext()) {
                    currentNeighbour = neighbourIterator.next();
                    routingHistoryIterator = FileNode.this.messageRoutingHistory.iterator();
                    hasAlreadyRouted = false;
                    while (routingHistoryIterator.hasNext()) {
                        currentRoutingHistory = routingHistoryIterator.next();
                        if (currentNeighbour.getIp().equals(currentRoutingHistory.getValue().getIp())
                                && currentNeighbour.getPort() == currentRoutingHistory.getValue().getPort()
                                && currentRoutingHistory.getKey().equals(messageId)) {
                            hasAlreadyRouted = true;
                        }
                    }
                    if (!hasAlreadyRouted) {
                        this.send(currentNeighbour.getIp(), currentNeighbour.getPort(), query);
                        FileNode.this.messageRoutingHistory.add(new Pair<>(messageId, currentNeighbour));
                        break;
                    }
                    iteratedNeighbors++;
                }
                if (iteratedNeighbors == FileNode.this.neighbours.size()) {
                    FileNode.this.SearchOK(address, port, hopCount, new ArrayList<String>());
                }
            }
        };
        command.start();
    }

    public void SearchOK(String address, int port, int hopCount, List<String> fileNames) {
        FileNodeCommand command = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                String query = String.format("SEROK %1$d %2$s %3$d %4$d %5$s", fileNames.size(), FileNode.this.address, FileNode.this.port, hopCount, String.join(" ", fileNames));
                query = String.format("%1$04d %2$s\n", query.length() + 5, query);
                this.send(address, port, query);
            }
        };
        command.start();
    }

    public void Error(String address, int port) {
        FileNodeCommand command = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                String query = String.format("ERROR\n");
                query = String.format("%1$04d %2$s", query.length() + 5, query);
                this.send(address, port, query);
            }
        };
        command.start();
    }

    public void SendMessage(String address, int port, String message) {
        FileNodeCommand command = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                String query = String.format("%1$s\n", message);
                this.send(address, port, query);
            }
        };
        command.start();
    }

    private void LoadFiles() {
        List<String> allFiles = new ArrayList<String>(
                Arrays.asList("Adventures of Tintin",
                        "Jack and Jill",
                        "Glee",
                        "The Vampire Diarie",
                        "King Arthur",
                        "Windows XP",
                        "Harry Potter",
                        "Kung Fu Panda",
                        "Lady Gaga",
                        "Twilight",
                        "Windows 8",
                        "Mission Impossible",
                        "Turn Up The Music",
                        "Super Mario",
                        "American Pickers",
                        "Microsoft Office 2010",
                        "Happy Feet",
                        "Modern Family",
                        "American Idol",
                        "Hacking for Dummies")
        );
        Collections.shuffle(allFiles);
        this.files = allFiles.subList(0, 10);
    }

    private List<String> SearchFile(String query) {
        List<String> matchingFiles = new ArrayList<String>();
        String[] tokens = query.split(" ");
        int matchWordCount = 0;
        for (int j = 0; j < this.files.size(); j++) {
            String[] words = this.files.get(j).split(" ");
            matchWordCount = 0;
            for (int i = 0; i < tokens.length; i++) {
                for (int z = 0; z < words.length; z++) {
                    if (tokens[i].toLowerCase().equals(words[z].toLowerCase())) {
                        matchWordCount++;
                    }
                }
            }
            if (matchWordCount == tokens.length) {
                matchingFiles.add(this.files.get(j));
            }
        }
        return matchingFiles;
    }

    public static void main(String[] args) throws UnknownHostException {
        FileNode fileNode = new FileNode(args[0], Integer.parseInt(args[1]), args[2]);
        fileNode.Receive();
        fileNode.Reg();
        System.out.println(String.format("File node started at %1s:%2d", fileNode.getAddress(), fileNode.getPort()));
        System.out.println("Waiting for incoming queries..");
    }
}
