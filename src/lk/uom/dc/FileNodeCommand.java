package lk.uom.dc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.function.Function;

/**
 * Created by AkilaA on 10/21/2017.
 */
public class FileNodeCommand extends Thread {
    DatagramSocket socket;
    private DatagramPacket packet;

    public FileNodeCommand(DatagramSocket socket) {
        this.socket = socket;
    }

    public String getAddress() {
        if (packet != null) {
            return packet.getAddress().getHostAddress();
        } else {
            return null;
        }
    }

    public int getPort() {
        if (packet != null) {
            return packet.getPort();
        } else {
            return 0;
        }
    }

    public void send(String address, int port, String message) {
        try {
            byte[] buf = message.getBytes();
            InetAddress receiver = InetAddress.getByName(address);
            this.packet = new DatagramPacket(buf, buf.length, receiver, port);
            this.socket.send(this.packet);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String receive() {
        byte[] buf = new byte[65536];
        this.packet = new DatagramPacket(buf, buf.length);
        try {
            this.socket.receive(this.packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String received = new String(this.packet.getData(), 0, this.packet.getLength());
        return received;
    }
}
