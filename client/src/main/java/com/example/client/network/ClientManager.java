package com.example.client.network;

import com.example.common.network.Payload;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ClientManager {

    //==================================================
    // CONFIGURATION RÉSEAU
    //==================================================
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;

    //==================================================
    // CONNEXION SOCKET
    //==================================================
    private static Socket socket;
    private static ObjectOutputStream out; // Flux d'envoi vers le serveur
    private static ObjectInputStream in;   // Flux de réception depuis le serveur

    //==================================================
    // THREAD D'ÉCOUTE
    //==================================================
    private static Thread listenerThread; // Thread qui écoute en permanence le serveur

    //==================================================
    // CALLBACKS UI
    //==================================================
    private static Consumer<Object> onMessageReceived; // Appelé quand un message async arrive
    private static Runnable onDeconnexion;             // Appelé quand la connexion est perdue

    //==================================================
    // FILE D'ATTENTE POUR RÉPONSES SYNCHRONES
    // (utilisée uniquement pour LOGIN et INSCRIPTION)
    //==================================================
    private static final BlockingQueue<Object> syncResponseQueue = new LinkedBlockingQueue<>();
    private static volatile boolean waitingForSyncResponse = false;

    //==================================================
    // SETTERS DES CALLBACKS
    //==================================================

    /**
     * Définit le callback appelé à chaque message async reçu du serveur.
     * (messages, liste users, historique...)
     */
    public static void setOnMessageReceived(Consumer<Object> callback) {
        onMessageReceived = callback;
    }

    /**
     * Définit le callback appelé quand la connexion au serveur est perdue.
     */
    public static void setOnDeconnexion(Runnable callback) {
        onDeconnexion = callback;
    }

    //==================================================
    // CONNEXION
    //==================================================

    /**
     * Ouvre la connexion socket avec le serveur si elle n'est pas déjà ouverte.
     * Démarre aussi le thread d'écoute si nécessaire.
     */
    public static void connecter() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        }
        startListeningIfNeeded();
    }

    /**
     * Ferme la connexion socket et arrête le thread d'écoute.
     */
    public static void deconnecter() throws IOException {
        if (socket != null) {
            socket.close();
            socket = null;
            listenerThread = null;
        }
    }

    //==================================================
    // THREAD D'ÉCOUTE
    //==================================================

    /**
     * Démarre le thread d'écoute s'il n'est pas déjà actif.
     * Ce thread tourne en permanence et dispatch les objets reçus :
     * - Vers syncResponseQueue si on attend une réponse synchrone (login/inscription)
     * - Vers onMessageReceived sinon (messages, liste users, historique...)
     */

    private static void startListeningIfNeeded() {
        if (listenerThread != null && listenerThread.isAlive()) return;

        listenerThread = new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();

                    if (waitingForSyncResponse) {
                        // Seuls String et Utilisateur sont des réponses auth
                        // On ignore les Payload (UPDATE_USER_LIST etc.) pendant l'auth
                        if (obj instanceof String || obj instanceof com.example.common.model.Utilisateur) {
                            syncResponseQueue.put(obj);
                        }
                        // Les Payload reçus pendant l'auth sont ignorés ils seront redemandés via REQUEST_USER_LIST après connexion
                    } else {
                        if (onMessageReceived != null) {
                            final Object finalObj = obj;
                            onMessageReceived.accept(finalObj);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[CLIENT] Déconnexion: " + e.getMessage());
                if (onDeconnexion != null) {
                    Platform.runLater(onDeconnexion);
                }
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public static Object envoyerRequete(String action, Object data) {
        try {
            // Arrêter le listener AVANT de fermer le socket
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
                listenerThread = null;
            }

            // Fermer le socket proprement
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            socket = null;
            out = null;
            in = null;

            // Désactiver temporairement le callback de déconnexion
            // pour éviter l'alerte pendant la reconnexion
            Runnable savedDeconnexion = onDeconnexion;
            onDeconnexion = null;

            syncResponseQueue.clear();
            waitingForSyncResponse = true;

            connecter();

            // Remettre le callback après connexion réussie
            onDeconnexion = savedDeconnexion;

            out.writeObject(new Payload(action, data));
            out.flush();

            Object response = syncResponseQueue.poll(15, TimeUnit.SECONDS);
            waitingForSyncResponse = false;

            if (response == null) {
                System.err.println("[CLIENT] Timeout : pas de réponse du serveur");
            }
            return response;

        } catch (Exception e) {
            waitingForSyncResponse = false;
            System.err.println("[CLIENT] Erreur requête: " + e.getMessage());
            return null;
        }
    }

    //==================================================
    // ENVOI DE MESSAGES
    //==================================================

    /**
     * Envoi SYNCHRONE — attend la réponse du serveur (max 5 secondes).
     * Utilisé uniquement pour LOGIN et INSCRIPTION.
     */

    /**
     * Envoi ASYNCHRONE — envoie et n'attend pas de réponse.
     * Utilisé pour tout le reste (messages, demande liste, historique...)
     */
    public static void envoyerMessage(String action, Object data) {
        try {
            connecter();
            out.writeObject(new Payload(action, data));
            out.flush();
        } catch (Exception e) {
            System.err.println("[CLIENT] Erreur d'envoi: " + e.getMessage());
            if (onDeconnexion != null) {
                Platform.runLater(onDeconnexion);
            }
        }
    }

    //==================================================
    // UTILITAIRES
    //==================================================

    public static void startListening() {
        startListeningIfNeeded();
    }

    public static ObjectInputStream getIn() {
        return in;
    }
}
