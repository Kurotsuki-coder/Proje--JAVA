package com.example.server.network;

import com.example.common.model.Utilisateur;
import com.example.common.network.Payload;
import com.example.server.db.DatabaseManager;
import com.example.common.model.Message;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private DatabaseManager dbManager;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dbManager = new DatabaseManager();
    }

    //Méthode pour que le serveur puisse pousser un message vers ce client
    public void envoyerObjet(Object obj) {
        try {
            if (out != null) {
                out.writeObject(obj);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi d'un objet au client: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        String nomUtilisateur = null; //Pour savoir qui se déconnecte à la fin
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Payload request = (Payload) in.readObject();

                if ("INSCRIPTION".equals(request.getAction())) {
                    Utilisateur u = (Utilisateur) request.getData();
                    System.out.println("Demande d'inscription pour: " + u.getNom());
                    boolean success = dbManager.sauvegarderUtilisateur(u.getNom(), u.getMotsdepasse());
                    out.writeObject(success ? "SUCCESS" : "DEJA_EXISTANT");
                    out.flush();
                } else if ("CONNEXION".equals(request.getAction())) {
                    Utilisateur u = (Utilisateur) request.getData();
                    boolean isValid = dbManager.verifierConnexion(u.getNom(), u.getMotsdepasse());

                    if (isValid) {
                        if (ChatServer.clientsConnectes.containsKey(u.getNom())) {
                            out.writeObject("ALREADY_CONNECTED");
                            out.flush();
                            return; // On arrête là pour ce client
                        }

                        Utilisateur userEnBase = dbManager.getUtilisateurParNom(u.getNom());
                        if (userEnBase != null) {
                            nomUtilisateur = userEnBase.getNom();
                            ChatServer.clientsConnectes.put(nomUtilisateur, this);

                            out.writeObject(userEnBase); // Succès : on envoie l'objet
                            out.flush();

                            ChatServer.diffuserListeUtilisateurs();
                        }
                    } else {
                        out.writeObject("FAILED");
                        out.flush();
                    }
                } else if ("SEND_MESSAGE".equals(request.getAction())) {
                    Message msg = (Message) request.getData();
                    System.out.println("[CHAT] Message reçu de " + msg.getExpediteur().getNom());
                    //On demande au ChatServer de rediffuser à tout le meonde
                    ChatServer.diffuserMessage(msg);
                } else if ("SEND_PRIVATE_MESSAGE".equals(request.getAction())) {
                    Message msg = (Message) request.getData();
                    dbManager.sauvegarderMessage(msg);
                    ChatServer.envoyerMessagePrive(msg.getDestinataire().getNom(), msg);
                    System.out.println("[CHAT] Message de " + msg.getExpediteur().getNom() + " sauvegardé et transmis à " + msg.getDestinataire().getNom());
                    //On envoi uniquement au destinataire
                    //ChatServer.envoyerMessagePrive(msg.getDestinataire().getNom(), msg);
                } else if ("GET_HISTORY".equals(request.getAction())) {
                    Utilisateur cible = (Utilisateur) request.getData();
                    List<Message> historique = dbManager.recupererHistorique(nomUtilisateur, cible.getNom());
                    out.writeObject(new Payload("HISTORY_DATA", historique));
                    out.flush();
                } else if ("REQUEST_USER_LIST".equals(request.getAction())) {
                    this.envoyerObjet(new Payload("UPDATE_USER_LIST", new java.util.ArrayList<>(ChatServer.clientsConnectes.keySet())));
                }
            }
        } catch (EOFException | SocketException e) {
            System.out.println("[SERVEUR] Un client s'est déconnecté");
        } catch (Exception e) {
            System.err.println("[SERVEUR] Erreur ClientHandler: " + e.getMessage());
        } finally {
            if (nomUtilisateur != null) {
                ChatServer.clientsConnectes.remove(nomUtilisateur);
                ChatServer.diffuserListeUtilisateurs(); //Mise à jour de la liste
            }
            //ChatServer.clients.remove(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
