package com.example.server.db;

import com.example.common.model.Message;
import com.example.common.model.Utilisateur;
import com.example.server.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    public boolean sauvegarderUtilisateur(String nom, String motDePasseClair) {
        String passwordHache = BCrypt.hashpw(motDePasseClair, BCrypt.gensalt(12));
        Utilisateur user = new Utilisateur(nom, passwordHache);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean verifierConnexion(String nom, String motDePasseClair) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Utilisateur> query = session.createQuery(
                    "FROM Utilisateur WHERE nom = :nom", Utilisateur.class
            );
            query.setParameter("nom", nom);
            Utilisateur user = query.uniqueResult();

            if (user != null) {
                return BCrypt.checkpw(motDePasseClair, user.getMotsdepasse());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Si le user n'existe pas ou si le mdp est faux
    }

    public List<Message> recupererHistorique(String nom1, String nom2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Message m WHERE " +
                    "(m.expediteur.nom = :n1 AND m.destinataire.nom = :n2) OR " + // ← "=" ajouté
                    "(m.expediteur.nom = :n2 AND m.destinataire.nom = :n1) " +
                    "ORDER BY m.dateEnvoi ASC";

            Query<Message> query = session.createQuery(hql, Message.class);
            query.setParameter("n1", nom1);
            query.setParameter("n2", nom2);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void sauvegarderMessage(Message msg) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            Utilisateur exp = session.createQuery("FROM Utilisateur WHERE nom = :nom", Utilisateur.class)
                    .setParameter("nom", msg.getExpediteur().getNom())
                    .uniqueResult();
            Utilisateur dest = session.createQuery("FROM Utilisateur WHERE nom = :nom", Utilisateur.class)
                    .setParameter("nom", msg.getDestinataire().getNom())
                    .uniqueResult();

            if (exp == null || dest == null) {
                System.err.println("[DB] Expéditeur ou destinataire introuvable en base");
                return;
            }

            msg.setExpediteur(exp);
            msg.setDestinataire(dest);

            session.persist(msg);
            transaction.commit();
            System.out.println("[DB] Message sauvegardé dans la base de données");
        } catch (Exception e) {
            System.err.println("[DB] Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    public Utilisateur getUtilisateurParNom(String nom) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Utilisateur WHERE nom = :nom", Utilisateur.class)
                    .setParameter("nom", nom)
                    .uniqueResult();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Récupère la liste complète des objets Utilisateur.
     * Cette version est compatible avec le ChatServer et le ChatController de ton binôme.
     */
    public List<Utilisateur> getAllUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // On récupère les objets complets
            return session.createQuery("FROM Utilisateur", Utilisateur.class).getResultList();
        } catch (Exception e) {
            System.err.println("[DB] Erreur lors de la récupération des utilisateurs: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}