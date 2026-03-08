package com.example.server.network;

import com.example.common.model.Status;
import com.example.common.model.Utilisateur;
import com.example.common.network.Payload;
import com.example.server.db.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

public class ChatServer {
    public static final Logger logger = Logger.getLogger(ChatServer.class.getName());

    static {
        try {
            // Crée un fichier "server.log" qui conserve les traces
            FileHandler fh = new FileHandler("server.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
        } catch (IOException e) {
            logger.severe("Erreur d'initialisation du fichier de log : " + e.getMessage());
        }
    }

    private static final int PORT = 1234;
    // Map des clients actuellement connectés
    public static final Map<String, ClientHandler> clientsConnectes = new ConcurrentHashMap<>();

    // Instance unique du manager de base de données
    private static final DatabaseManager dbManager = new DatabaseManager();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("[SERVEUR] démarré sur le port " + PORT);

            // lancer Hibernate au démarrage du serveur pour que la première connexion client soit rapide
            logger.info("[SERVEUR] Initialisation de la base de données...");
            dbManager.getAllUsers();
            logger.info("[SERVEUR] Base de données prête !");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("[SERVEUR] Connexion entrante : " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            logger.severe("[SERVEUR] Erreur critique : " + e.getMessage());
        }
    }

    // Diffuse à tout le monde
    public static void diffuserMessage(Object message) {
        clientsConnectes.values().forEach(client -> client.envoyerObjet(message));
    }

    // Envoi privé
    public static void envoyerMessagePrive(String destinataire, Object message) {
        ClientHandler handler = clientsConnectes.get(destinataire);
        if (handler != null) {
            handler.envoyerObjet(message);
        }
    }

    // Diffusion de la liste complète (incluant hors-ligne via DB)
    public static void diffuserListeUtilisateurs() {
        // 1. Récupère tous les utilisateurs depuis la DB
        List<Utilisateur> tousLesUsers = dbManager.getAllUsers();

        // 2. Enrichit la liste avec la réalité du serveur (en mémoire)
        for (Utilisateur u : tousLesUsers) {
            if (clientsConnectes.containsKey(u.getNom())) {
                u.setStatus(Status.ONLINE);
            } else {
                u.setStatus(Status.OFFLINE);
            }
        }

        // 3. Diffuse cette liste enrichie
        Payload payload = new Payload("UPDATE_USER_LIST", tousLesUsers);
        for (ClientHandler client : clientsConnectes.values()) {
            client.envoyerObjet(payload);
        }
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }

    // Getter statique utile pour les handlers
    public static DatabaseManager getDbManager() {
        return dbManager;
    }

    public static List<Utilisateur> getUtilisateursAvecStatut() {
        List<Utilisateur> tousLesUsers = dbManager.getAllUsers();
        for (Utilisateur u : tousLesUsers) {
            // La vérité est dans la Map clientsConnectes, pas dans la DB
            if (clientsConnectes.containsKey(u.getNom())) {
                u.setStatus(Status.ONLINE);
            } else {
                u.setStatus(Status.OFFLINE);
            }
        }
        return tousLesUsers;
    }

    // Dans ChatServer.java
    public static List<Utilisateur> getListeUtilisateursEnrichie() {
        List<Utilisateur> tous = dbManager.getAllUsers();
        for (Utilisateur u : tous) {
            // Si le nom est dans la Map des clients connectés, il est ONLINE
            if (clientsConnectes.containsKey(u.getNom())) {
                u.setStatus(Status.ONLINE);
            } else {
                u.setStatus(Status.OFFLINE);
            }
        }
        return tous;
    }
}
