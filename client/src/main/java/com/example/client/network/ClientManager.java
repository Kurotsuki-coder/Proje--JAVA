package com.example.client.network;

import com.example.common.network.Payload;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientManager {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;

    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private static Consumer<Object> onMessageReceived; // Le callback

    public static void connecter() throws IOException {
        if (socket != null && !socket.isClosed()) {
            try { socket.close(); } catch (Exception ignored) {}
        }
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public static Object envoyerRequete(String action, Object data) {
        try {
            connecter();
            Payload requete = new Payload(action, data);
            out.writeObject(requete);
            out.flush();
            return in.readObject();
        } catch (Exception e) {
            System.err.println("Erreur réseau: " + e.getMessage());
            return null;
        }
    }

    public static void envoyerMessage(String action, Object data) {
        try {
            if (socket == null || socket.isClosed()) {
                connecter();
            }
            Payload requete = new Payload(action, data);
            out.writeObject(requete);
            out.flush();
        } catch (Exception e) {
            System.err.println("Erreur d'envoi: " + e.getMessage());
        }
    }

    // On enregistre le callback ET on lance le thread d'écoute
    public static void setOnMessageReceived(Consumer<Object> callback) {
        onMessageReceived = callback;
        new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(obj);
                    }
                }
            } catch (Exception e) {
                System.err.println("Déconnexion du flux d'écoute: " + e.getMessage());
            }
        }).start();
    }

    public static ObjectInputStream getIn() {
        return in;
    }

    public static void deconnecter() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }
}
