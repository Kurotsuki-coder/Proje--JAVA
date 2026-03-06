package com.example.server.network;

import com.example.common.network.Payload;
import com.example.server.db.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ChatServer {
    private static final int PORT = 1234;
    public static java.util.Map<String, ClientHandler> clientsConnectes = new java.util.concurrent.ConcurrentHashMap<>();

    // ✅ Une seule instance de DatabaseManager partagée
    private static final DatabaseManager dbManager = new DatabaseManager();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVEUR] démarré sur le port " + PORT);
            System.out.println("En attente de connexions clients...");

            while (true) {
                Socket clienSocket = serverSocket.accept();
                System.out.println("[SERVEUR] Nouveau Client connecté: " + clienSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clienSocket);
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

    // ✅ Corrigé — envoie TOUS les users de la DB, pas juste les connectés
    public static void diffuserListeUtilisateurs() {
        List<String> tousLesUsers = dbManager.getAllUsers();
        System.out.println("[SERVEUR] Diffusion liste complète: " + tousLesUsers);
        Payload payload = new Payload("UPDATE_USER_LIST", tousLesUsers);
        for (ClientHandler client : clientsConnectes.values()) {
            client.envoyerObjet(payload);
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
}