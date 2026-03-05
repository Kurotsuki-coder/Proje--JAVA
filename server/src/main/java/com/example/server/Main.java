package com.example.server;

import com.example.server.db.DatabaseManager;
import com.example.common.model.Utilisateur;

public class Main {
    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();

        System.out.println("Test de connexion de la base de données");

        boolean success = dbManager.sauvegarderUtilisateur("user", "passer123");

        if (success) {
            System.out.println("L'utilisateur a été crée dans la base de données");
        } else {
            System.out.println("Erreur dans la création de l'utilisateur");
        }
    }
}
