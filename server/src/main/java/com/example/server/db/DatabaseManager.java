package com.example.server.db;

import com.example.common.model.Message;
import com.example.common.model.Status;
import com.example.common.model.Utilisateur;
import com.example.server.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.mindrot.jbcrypt.BCrypt;

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
        return false;
    }

    public List<Message> recupererHistorique(String nom1, String nom2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Message m WHERE " +
                    "(m.expediteur.nom = :n1 AND m.destinataire.nom = :n2) OR " +
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
            System.out.println("[DB] Message sauvegardé");
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

    // ✅ Retourne List<Utilisateur> avec statuts
    public List<Utilisateur> getAllUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Utilisateur> users = session.createQuery(
                    "FROM Utilisateur", Utilisateur.class
            ).getResultList();
            System.out.println("[DB] getAllUsers → " + users.size() + " users");
            return users;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ✅ Met à jour le statut en DB
    public void updateStatus(String nom, Status status) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.createMutationQuery(
                            "UPDATE Utilisateur SET status = :status WHERE nom = :nom"
                    )
                    .setParameter("status", status)
                    .setParameter("nom", nom)
                    .executeUpdate();
            transaction.commit();
            System.out.println("[DB] Status de " + nom + " → " + status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}