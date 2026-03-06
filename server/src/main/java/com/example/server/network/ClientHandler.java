package com.example.server.network;

import com.example.common.model.*;
import com.example.common.network.Payload;
import com.example.server.db.DatabaseManager;

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
        this.dbManager = ChatServer.getDbManager();
    }

    public void envoyerObjet(Object obj) {
        try {
            if (out != null) {
                out.writeObject(obj);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Erreur d'envoi client: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        String nomUtilisateur = null;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Payload request = (Payload) in.readObject();

                if ("INSCRIPTION".equals(request.getAction())) {
                    Utilisateur u = (Utilisateur) request.getData();
                    boolean success = dbManager.sauvegarderUtilisateur(u.getNom(), u.getMotsdepasse());
                    out.writeObject(success ? "SUCCESS" : "DEJA_EXISTANT");
                    out.flush();

                } else if ("CONNEXION".equals(request.getAction())) {
                    Utilisateur u = (Utilisateur) request.getData();

                    if (ChatServer.clientsConnectes.containsKey(u.getNom())) {
                        out.writeObject("ALREADY_CONNECTED");
                        out.flush();
                    } else {
                        boolean isValid = dbManager.verifierConnexion(u.getNom(), u.getMotsdepasse());
                        Utilisateur userEnBase = dbManager.getUtilisateurParNom(u.getNom());

                        if (isValid && userEnBase != null) {
                            nomUtilisateur = userEnBase.getNom();
                            ChatServer.clientsConnectes.put(nomUtilisateur, this);

                            out.writeObject(userEnBase);
                            out.flush();

                            // Diffusion générale pour mettre à jour les statuts chez tout le monde
                            ChatServer.diffuserListeUtilisateurs();
                        } else {
                            out.writeObject("FAILED");
                            out.flush();
                        }
                    }

                } else if ("SEND_PRIVATE_MESSAGE".equals(request.getAction())) {
                    Message msg = (Message) request.getData();
                    dbManager.sauvegarderMessage(msg);
                    ChatServer.envoyerMessagePrive(msg.getDestinataire().getNom(), msg);

                } else if ("GET_HISTORY".equals(request.getAction())) {
                    Utilisateur cible = (Utilisateur) request.getData();
                    List<Message> historique = dbManager.recupererHistorique(nomUtilisateur, cible.getNom());
                    out.writeObject(new Payload("HISTORY_DATA", historique));
                    out.flush();

                } else if ("REQUEST_USER_LIST".equals(request.getAction())) {
                    // Envoi de la liste enrichie (avec statuts)
                    this.envoyerObjet(new Payload("UPDATE_USER_LIST", ChatServer.getListeUtilisateursEnrichie()));
                }
            }
        } catch (EOFException | SocketException e) {
            System.out.println("[SERVEUR] Déconnexion détectée.");
        } catch (Exception e) {
            System.err.println("[SERVEUR] Erreur Handler: " + e.getMessage());
        } finally {
            if (nomUtilisateur != null) {
                ChatServer.clientsConnectes.remove(nomUtilisateur);
                ChatServer.diffuserListeUtilisateurs(); // Informe les autres de la déconnexion
            }
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }
}