package lk.uom.dc;

import javafx.util.Pair;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

public class FileNode {
    private String bAddress;
    private int bPort;
    private int port;
    private String address;
    private String username;
    DatagramSocket socket;
    private List<Neighbour> neighbours;
    private List<Pair<Integer, String>> files;
    private List<Pair<String, Neighbour>> messageRoutingHistory;
    private int lTimestamp = 0;
    private ArrayList<FileReview> reviews;

    public int getLTimestamp() {
        return lTimestamp;
    }

    private void setLtimestamp(int lTimestamp) {
        if (this.lTimestamp < lTimestamp) {
            this.lTimestamp = lTimestamp + 1;
        } else {
            this.lTimestamp = this.lTimestamp + 1;
        }
    }

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public List<Pair<Integer, String>> getFiles() {
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
        this.reviews = new ArrayList<FileReview>();
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
                    System.out.println(message);
                    message = message.replace("\n", "");
                    if (message.equals("")) {
                        continue;
                    }
                    try {
                        String[] tokens = message.split(" ");
                        if (tokens[1].equals("RTTBL")) {
                            String replyMessage = "";
                            for (int i = 0; i < FileNode.this.neighbours.size(); i++) {
                                replyMessage += String.format("%1$s:%2$d\n", FileNode.this.neighbours.get(i).getIp(), FileNode.this.neighbours.get(i).getPort());
                            }
                            if (FileNode.this.neighbours.size() == 0) {
                                replyMessage = "No neighbours found!";
                            }
                            FileNode.this.SendMessage(this.getPacket().getAddress().getHostAddress(), this.getPacket().getPort(), replyMessage);
                        } else if (tokens[1].equals("FILES")) {
                            FileNode.this.SendMessage(this.getPacket().getAddress().getHostAddress(), this.getPacket().getPort(),
                                    String.join("\n", FileNode.this.getFiles().stream().map(t -> t.getKey() + "-" + t.getValue()).collect(Collectors.toList())));
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
                        } else if (tokens[1].equals("COMM")) {
                            FileNode.this.setLtimestamp(Integer.parseInt(tokens[tokens.length - 1]));
                            FileNode.this.messageRoutingHistory.add(new Pair<String, Neighbour>(tokens[8], new Neighbour(this.getAddress(), this.getPort())));
                            FileNode.this.Comment(tokens[2], Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4])
                                    , Integer.parseInt(tokens[5])
                                    , tokens[6]
                                    , Integer.parseInt(tokens[7]) - 1
                                    , tokens[8]
                                    , Integer.parseInt(tokens[9]));
                        } else if (tokens[1].equals("RANK")) {
                            FileNode.this.setLtimestamp(Integer.parseInt(tokens[tokens.length - 1]));
                            FileNode.this.messageRoutingHistory.add(new Pair<String, Neighbour>(tokens[8], new Neighbour(this.getAddress(), this.getPort())));
                            FileNode.this.Rank(tokens[2], Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4])
                                    , Integer.parseInt(tokens[5])
                                    , Integer.parseInt(tokens[6])
                                    , Integer.parseInt(tokens[7]) - 1
                                    , tokens[8]
                                    , Integer.parseInt(tokens[9]));
                        } else if (tokens[1].equals("VIEWCOMM")) {
                            FileNode.this.ViewComment(this.getAddress(), this.getPort(), Integer.parseInt(tokens[2]));
                        } else if (tokens[1].equals("SER")) {
                            List<Pair<Integer, String>> matchingFiles;
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
                                ip = this.getAddress();
                                sourcePort = this.getPort();
                                hopCount = 10;
                            }

                            boolean messagePreviouslyFound = false;
                            for (int i = 0; i < FileNode.this.messageRoutingHistory.size(); i++) {
                                if (FileNode.this.messageRoutingHistory.get(i).getKey().equals(messageId)) {
                                    messagePreviouslyFound = true;
                                }
                            }

                            if ((matchingFiles.size() > 0 && !messagePreviouslyFound) || (hopCount == 0 && matchingFiles.size() == 0) || FileNode.this.neighbours.size() == 0) {
                                FileNode.this.SearchOK(ip, sourcePort, hopCount, matchingFiles);
                            }

                            FileNode.this.messageRoutingHistory.add(new Pair<String, Neighbour>(messageId, new Neighbour(this.getPacket().getAddress().getHostAddress(), this.getPacket().getPort())));


                            if (hopCount > 0 && FileNode.this.neighbours.size() > 0) {
                                FileNode.this.Search(ip, sourcePort, fileName, hopCount - 1, messageId);
                            }
                        }
                    } catch (Exception ex) {
                        FileNode.this.Error(this.getPacket().getAddress().getHostAddress(), this.getPacket().getPort());
                    }
                }
                System.exit(0);
            }
        };
        receiveCommand.start();
    }

    public void Reg() {
        String query = String.format("REG %1$s %2$d %3$s", this.address, this.port, this.username);
        System.out.println(query);
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
                if (FileNode.this.neighbours.size() == 0) {
                    System.exit(0);
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
        String query = String.format("SER %1$s %2$d %3$s %4$d %5$s", address, port, fileName, hopCount, messageId);
        query = String.format("%1$04d %2$s", query.length() + 5, query);
        this.RandomWalk(messageId, query);
    }

    public void SearchOK(String address, int port, int hopCount, List<Pair<Integer, String>> fileNames) {
        FileNodeCommand command = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                List<String> replacedWithUnderscore = new ArrayList<String>();
                for (int i = 0; i < fileNames.size(); i++) {
                    replacedWithUnderscore.add(fileNames.get(i).getKey() + ":" + fileNames.get(i).getValue().replace(" ", "_"));
                }
                String query = String.format("SEROK %1$d %2$s %3$d %4$d %5$s", fileNames.size(), FileNode.this.address
                        , FileNode.this.port, hopCount, String.join(" ", replacedWithUnderscore));
                query = String.format("%1$04d %2$s\n", query.length() + 5, query);
                this.send(address, port, query);
            }
        };
        command.start();
    }

    public void Comment(String address, int port, int fileId, int commentId, String commentText, int hopCount, String messageId, int lTimestamp) {
        Pair<Integer, String> file = this.FindFile(fileId);
        if (file != null) {
            if (commentId > 0) {
                this.FindComment(fileId, commentId).addReply(commentText, address, port, lTimestamp);
            } else {
                if (this.FindReview(fileId) == null) {
                    FileReview review = new FileReview(file.getKey(), file.getValue());
                    review.addComment(commentText, address, port, lTimestamp);
                    this.reviews.add(review);
                } else {
                    FileReview review = FindReview(fileId);
                    review.addComment(commentText, address, port, lTimestamp);
                }
            }
        }
        String query = String.format("COMM %1$s %2$d %3$d %4$d %5$s %6$d %7$s %8$d", address, port, fileId, commentId, commentText, hopCount, messageId, lTimestamp);
        query = String.format("%1$04d %2$s", query.length() + 5, query);
        if (this.neighbours.size() > 0 && hopCount > 0) {
            this.RandomWalk(messageId, query);
        }
    }

    public void Rank(String address, int port, int fileId, int commentId, int rankValue, int hopCount, String messageId, int lTimestamp) {
        Pair<Integer, String> file = this.FindFile(fileId);
        if (file != null) {
            if (commentId > 0) {
                this.FindComment(fileId, commentId).rankComment(rankValue, address, port);
            } else {
                if (this.FindReview(fileId) == null) {
                    FileReview review = new FileReview(file.getKey(), file.getValue());
                    review.rankReview(rankValue, address, port);
                    this.reviews.add(review);
                } else {
                    FileReview review = FindReview(fileId);
                    review.rankReview(rankValue, address, port);
                }
            }
        }
        String query = String.format("RANK %1$s %2$d %3$d %4$d %5$d %6$d %7$s %8$d", address, port, fileId, commentId, rankValue, hopCount, messageId, lTimestamp);
        query = String.format("%1$04d %2$s", query.length() + 5, query);
        if (this.neighbours.size() > 0 && hopCount > 0) {
            this.RandomWalk(messageId, query);
        }
    }

    public void ViewComment(String address, int port, int fileId) {
        FileReview review = this.FindReview(fileId);
        if (review != null) {
            this.SendMessage(address, port, review.toString());
        } else {
            this.SendMessage(address, port, "Review not found!");
        }
    }

    private Pair<Integer, String> FindFile(int fileId) {
        for (Pair<Integer, String> file : this.files) {
            if (file.getKey() == fileId) {
                return file;
            }
        }
        return null;
    }

    private FileReview FindReview(int fileId) {
        for (FileReview review : this.reviews) {
            if (review.getFileId() == fileId) {
                return review;
            }
        }
        return null;
    }

    private Comment FindCommentRecursive(int commentId, ArrayList<Comment> comments) {
        Comment result = null;
        for (Comment comment : comments) {
            if (comment.getCommentId() == commentId) {
                result = comment;
                break;
            } else {
                if (comment.getReplies().size() > 0) {
                    result = this.FindCommentRecursive(commentId, comment.getReplies());
                    if (result != null) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    private Comment FindComment(int fileId, int commentId) {
        FileReview review = this.FindReview(fileId);
        if (review != null) {
            return this.FindCommentRecursive(commentId, review.getComments());
        }
        return null;

    }

    public void RandomWalk(String messageId, String query) {
        FileNodeCommand command = new FileNodeCommand(this.socket) {
            @Override
            public void run() {
                Iterator<Neighbour> neighbourIterator = FileNode.this.neighbours.iterator();
                Iterator<Pair<String, Neighbour>> routingHistoryIterator;
                List<Neighbour> notRoutedNeighbors = new ArrayList<Neighbour>();
                Neighbour currentNeighbour;
                Pair<String, Neighbour> currentRoutingHistoryRecord;
                boolean hasAlreadyRouted;
                while (neighbourIterator.hasNext()) {
                    currentNeighbour = neighbourIterator.next();
                    routingHistoryIterator = FileNode.this.messageRoutingHistory.iterator();
                    hasAlreadyRouted = false;
                    while (routingHistoryIterator.hasNext()) {
                        currentRoutingHistoryRecord = routingHistoryIterator.next();
                        if (currentNeighbour.getIp().equals(currentRoutingHistoryRecord.getValue().getIp())
                                && currentNeighbour.getPort() == currentRoutingHistoryRecord.getValue().getPort()
                                && currentRoutingHistoryRecord.getKey().equals(messageId)) {
                            hasAlreadyRouted = true;
                            break;
                        }
                    }
                    if (!hasAlreadyRouted) {
                        notRoutedNeighbors.add(currentNeighbour);
                    }
                }
                Collections.shuffle(notRoutedNeighbors);

                if (notRoutedNeighbors.size() > 0) {
                    this.send(notRoutedNeighbors.get(0).getIp(), notRoutedNeighbors.get(0).getPort(), query);
                    FileNode.this.messageRoutingHistory.add(new Pair<>(messageId, notRoutedNeighbors.get(0)));
                } else {
                    Collections.shuffle(FileNode.this.neighbours);
                    this.send(FileNode.this.neighbours.get(0).getIp(), FileNode.this.neighbours.get(0).getPort(), query);
                }
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
        List<Pair<Integer, String>> allFiles = new ArrayList<Pair<Integer, String>>(
                Arrays.asList(new Pair<Integer, String>(1, "Adventures of Tintin"),
                        new Pair<Integer, String>(2, "Jack and Jill"),
                        new Pair<Integer, String>(3, "Glee"),
                        new Pair<Integer, String>(4, "The Vampire Diarie"),
                        new Pair<Integer, String>(5, "King Arthur"),
                        new Pair<Integer, String>(6, "Windows XP"),
                        new Pair<Integer, String>(7, "Harry Potter"),
                        new Pair<Integer, String>(8, "Kung Fu Panda"),
                        new Pair<Integer, String>(9, "Lady Gaga"),
                        new Pair<Integer, String>(10, "Twilight"),
                        new Pair<Integer, String>(11, "Hacking for Dummies"),
                        new Pair<Integer, String>(12, "Windows 8"),
                        new Pair<Integer, String>(13, "Mission Impossible"),
                        new Pair<Integer, String>(14, "Turn Up The Music"),
                        new Pair<Integer, String>(15, "Super Mario"),
                        new Pair<Integer, String>(16, "American Pickers"),
                        new Pair<Integer, String>(17, "Microsoft Office 2010"),
                        new Pair<Integer, String>(18, "Happy Feet"),
                        new Pair<Integer, String>(19, "Modern Family"),
                        new Pair<Integer, String>(20, "American Idol")));
        Collections.shuffle(allFiles);
        this.files = allFiles.subList(0, 10);
    }

    private List<Pair<Integer, String>> SearchFile(String query) {
        List<Pair<Integer, String>> matchingFiles = new ArrayList<Pair<Integer, String>>();
        String[] tokens = query.split("_");
        int matchWordCount = 0;
        for (int j = 0; j < this.files.size(); j++) {
            String[] words = this.files.get(j).getValue().split(" ");
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

        commandListner(fileNode);

    }

    private static void commandListner(FileNode fileNode) {
        while (true) {
            try {
                System.out.println("Waiting for queries..");
                Scanner scan = new Scanner(System.in);
                String input = scan.nextLine();
                String[] tokens = input.split(" ");
                if (input.equals("RTTBL")) {
                    String replyMessage = "";
                    for (int i = 0; i < fileNode.neighbours.size(); i++) {
                        replyMessage += String.format("%1$s:%2$d\n", fileNode.neighbours.get(i).getIp(), fileNode.neighbours.get(i).getPort());
                    }
                    if (fileNode.neighbours.size() == 0) {
                        replyMessage = "No neighbours found!";
                    }
                    System.out.println(replyMessage);
                } else if (input.equals("FILES")) {
                    System.out.println(String.join("\n", fileNode.getFiles().stream().map(t -> t.getKey() + "-" + t.getValue()).collect(Collectors.toList())));
                } else if (input.equals("THIS")) {
                    System.out.println(String.format("%1$s:%2$s", fileNode.getAddress(), fileNode.getPort()));
                } else if (input.equals("UNREG")) {
                    input = String.format("%1$04d %2$s", input.length() + 5, input);
                    fileNode.SendMessage(fileNode.getAddress(), fileNode.getPort(), input);
                } else if (input.length() > 3 && input.substring(0, 3).equals("SER")) {
                    input = String.format("%1$04d %2$s", input.length() + 5, input);
                    fileNode.SendMessage(fileNode.getAddress(), fileNode.getPort(), input);
                } else if (tokens[0].equals("COMM")) {
                    String messageId = UUID.randomUUID().toString();
                    input = String.format("COMM %1$s %2$d %3$d %4$d %5$s %6$d %7$s %8$d", fileNode.getAddress(), fileNode.getPort()
                            , Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2])
                            , tokens[3], 10, messageId, fileNode.getLTimestamp());
                    input = String.format("%1$04d %2$s", input.length() + 5, input);
                    fileNode.SendMessage(fileNode.getAddress(), fileNode.getPort(), input);
                } else if (tokens[0].equals("RANK")) {
                    String messageId = UUID.randomUUID().toString();
                    input = String.format("RANK %1$s %2$d %3$d %4$d %5$d %6$d %7$s %8$d", fileNode.getAddress(), fileNode.getPort()
                            , Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2])
                            , Integer.parseInt(tokens[3]), 10, messageId, fileNode.getLTimestamp());
                    input = String.format("%1$04d %2$s", input.length() + 5, input);
                    fileNode.SendMessage(fileNode.getAddress(), fileNode.getPort(), input);
                } else if (tokens[0].equals("VIEWCOMM")) {
                    input = String.format("VIEWCOMM %1$d", Integer.parseInt(tokens[1]));
                    input = String.format("%1$04d %2$s", input.length() + 5, input);
                    fileNode.SendMessage(tokens[2], Integer.parseInt(tokens[3]), input);
                } else if (input.equalsIgnoreCase("exit")) {
                    //send unreg message
                    fileNode.Unreg();
                    break;
                } else {
                    System.out.println("Invalid command!");
                }


            } catch (Exception ex) {
                System.out.println("Invalid command!");
            }
        }
        System.exit(0);
    }
}
