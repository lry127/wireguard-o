package com.wireguard;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class UdpBidirectionalForwarder {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: java UdpBidirectionalForwarder <xorKeySeed> <localPort> <forwardHost> <forwardPort>");
            System.exit(1);
        }

        String xorKeySeed = args[0];
        int localListeningPort = Integer.parseInt(args[1]);
        String forwardsToHost = args[2];
        int forwardsToPort = Integer.parseInt(args[3]);

        byte[] xorKey = md5(xorKeySeed);

        DatagramSocket listeningSocket = new DatagramSocket(localListeningPort);
        DatagramSocket forwardingSocket = new DatagramSocket(0);

        AtomicReference<InetAddress> lastClientAddress = new AtomicReference<>();
        AtomicInteger lastClientPort = new AtomicInteger(-1);

        Thread clientToRemote = new Thread(() -> {
            byte[] buf = new byte[4096];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    listeningSocket.receive(packet);

                    synchronized (UdpBidirectionalForwarder.class) {
                        lastClientAddress.set(packet.getAddress());
                        lastClientPort.set(packet.getPort());
                    }

                    byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                    byte[] xored = xorWithKey(data, xorKey);

                    DatagramPacket forwardPacket = new DatagramPacket(
                            xored, xored.length, InetAddress.getByName(forwardsToHost), forwardsToPort
                    );

                    forwardingSocket.send(forwardPacket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Thread remoteToClient = new Thread(() -> {
            byte[] buf = new byte[4096];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    forwardingSocket.receive(packet);

                    byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                    byte[] xored = xorWithKey(data, xorKey);

                    InetAddress clientAddr;
                    int clientPort;

                    synchronized (UdpBidirectionalForwarder.class) {
                        clientAddr = lastClientAddress.get();
                        clientPort = lastClientPort.get();
                    }

                    if (clientAddr != null && clientPort != -1) {
                        DatagramPacket reply = new DatagramPacket(xored, xored.length, clientAddr, clientPort);
                        listeningSocket.send(reply);
                    } else {
                        System.out.println("No client known to send response to.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        clientToRemote.start();
        remoteToClient.start();

        clientToRemote.join();
        remoteToClient.join();
    }

    private static byte[] xorWithKey(byte[] data, byte[] key) {
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return out;
    }

    private static byte[] md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }
}
