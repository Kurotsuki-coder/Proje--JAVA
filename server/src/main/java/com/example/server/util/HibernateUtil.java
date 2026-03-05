package com.example.server.util; // Ton package serveur

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import com.example.common.model.Utilisateur; // Import de ton module common
import com.example.common.model.Message;     // Import de ton module common

public class HibernateUtil {

    private static final SessionFactory sessionFactory;

    static {
        try {
            // On charge la config ET on déclare explicitement nos classes du module common
            sessionFactory = new Configuration()
                    .configure()
                    .addAnnotatedClass(Utilisateur.class)
                    .addAnnotatedClass(Message.class)
                    .buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Erreur de création de la SessionFactory : " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}