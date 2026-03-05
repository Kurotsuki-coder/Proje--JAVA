package com.example.common.network;

import java.io.Serializable; //Le implements Serializable est ce qui permet à Java de transformer ton objet en "bits" pour qu'ils puissent voyager dans le câble réseau

/*Cette classe sert d'enveloppe pour tous les messages circulant entre le client et le serveur*/

public class Payload implements Serializable {
    private static final long serialVersionUID = 1L; //indispensable pour envoyer des objets sur le réseau

    private String action;
    private Object data;

    public Payload(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
