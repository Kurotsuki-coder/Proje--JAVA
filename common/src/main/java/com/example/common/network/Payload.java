package com.example.common.network;

import java.io.Serializable;

/**
 * Enveloppe universelle pour tous les échanges réseau entre client et serveur.
 *
 * Exemple d'utilisation :
 *   new Payload("SEND_PRIVATE_MESSAGE", monMessage)
 *   new Payload("REQUEST_USER_LIST", null)
 *   new Payload("CONNEXION", utilisateur)
 */
public class Payload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String action; // Le type de requête ex: "SEND_PRIVATE_MESSAGE", "REQUEST_USER_LIST"
    private Object data;   // Le contenu — peut être un Utilisateur, Message, List<>... ou null

    public Payload(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    @Override
    public String toString() {
        return "Payload { action='" + action + "', data=" + data + " }";
    }
}
