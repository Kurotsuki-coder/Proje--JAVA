package com.example.server.network;

import com.example.common.network.Payload;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ChatServer {
    private static final int PORT = 1234;
    //public static java.util.List<ClientHandler> clients = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static java.util.Map<String, ClientHandler> clientsConnectes = new java.util.concurrent.ConcurrentHashMap<>();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVEUR] démarré sur le port " + PORT);
            System.out.println("En attente de connexions clients...");

            while (true) {
                //Le programme s'arrete ici jusqu'à ce qu'un client se connecte
                Socket clienSocket = serverSocket.accept();
                System.out.println("[SERVEUR] Nouveau Client connecté: " + clienSocket.getInetAddress());

                //on laisse le travail au ClientHandler dans un nouveau Thread
                ClientHandler handler = new ClientHandler(clienSocket);
                //clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[SERVEUR] erreur: " + e.getMessage());
        }
    }

    public static void diffuserMessage(Object message) {
        for (ClientHandler client : clientsConnectes.values()) {
            client.envoyerObjet(message);
        }
    }

    public static void envoyerMessagePrive(String destinataire, Object message) {
        ClientHandler handler = clientsConnectes.get(destinataire);
        if (handler != null) {
            handler.envoyerObjet(message);
        }
    }

    public static void diffuserListeUtilisateurs() {
        List<String> noms = new java.util.ArrayList<>(clientsConnectes.keySet());
        Payload payload = new Payload("UPDATE_USER_LIST", noms);
        for (ClientHandler client : clientsConnectes.values()) {
            client.envoyerObjet(payload);
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
}
