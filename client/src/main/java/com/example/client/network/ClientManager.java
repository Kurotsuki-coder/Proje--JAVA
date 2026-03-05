package com.example.client.network;

import com.example.common.network.Payload;
import com.example.common.model.Utilisateur;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ClientManager {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;
    private static Thread listenerThread;
    private static Consumer<Object> onMessageReceived;

    private static final BlockingQueue<Object> syncResponseQueue = new LinkedBlockingQueue<>();
    private static volatile boolean waitingForSyncResponse = false;

    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    public static void setOnMessageReceived(Consumer<Object> callback) {
        onMessageReceived = callback;
    }

    private static void startListeningIfNeeded() {
        if (listenerThread != null && listenerThread.isAlive()) return;

        listenerThread = new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();

                    // Réponse synchrone attendue (connexion / inscription)
                    if (waitingForSyncResponse) {
                        syncResponseQueue.put(obj);
                    } else {
                        // Message async → callback UI
                        if (onMessageReceived != null) {
                            final Object finalObj = obj;
                            onMessageReceived.accept(finalObj);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Déconnexion du flux d'écoute: " + e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public static void connecter() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        }
        startListeningIfNeeded();
    }

    // Pour connexion et inscription uniquement
    public static Object envoyerRequete(String action, Object data) {
        try {
            connecter(); // Ouvre le socket ET démarre le listener
            syncResponseQueue.clear();
            waitingForSyncResponse = true;

            out.writeObject(new Payload(action, data));
            out.flush();

            // Attendre la réponse max 5 secondes via la BlockingQueue
            Object response = syncResponseQueue.poll(5, TimeUnit.SECONDS);
            waitingForSyncResponse = false;

            if (response == null) {
                System.err.println("Timeout : pas de réponse du serveur");
            }
            return response;
        } catch (Exception e) {
            waitingForSyncResponse = false;
            System.err.println("Erreur requête: " + e.getMessage());
            return null;
        }
    }

    public static void envoyerMessage(String action, Object data) {
        try {
            connecter();
            out.writeObject(new Payload(action, data));
            out.flush();
        } catch (Exception e) {
            System.err.println("Erreur d'envoi: " + e.getMessage());
        }
    }

    public static void startListening() {
        startListeningIfNeeded();
    }

    public static ObjectInputStream getIn() {
        return in;
    }

    public static void deconnecter() throws IOException {
        if (socket != null) {
            socket.close();
            socket = null;
            listenerThread = null;
        }
    }
}
