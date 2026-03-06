package com.example.server;

import com.example.server.db.DatabaseManager;
import com.example.common.model.Utilisateur;
import com.example.server.network.ChatServer;

public class Main {
    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();

        ChatServer.logger.info("Test de connexion de la base de données");

        boolean success = dbManager.sauvegarderUtilisateur("user", "passer123");

        if (success) {
            ChatServer.logger.info("L'utilisateur a été crée dans la base de données");
        } else {
            ChatServer.logger.severe("Erreur dans la création de l'utilisateur");
        }
    }
}
