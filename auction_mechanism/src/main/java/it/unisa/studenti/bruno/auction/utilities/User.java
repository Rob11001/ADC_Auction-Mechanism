package it.unisa.studenti.bruno.auction.utilities;

import java.io.Serializable;

import net.tomp2p.peers.PeerAddress;

public class User implements Serializable {
    public PeerAddress _peer_address;
    public final String _username;
    private String _password;

    public User(PeerAddress _peer_address, String _username, String _password) {
        this._peer_address = _peer_address;
        this._username = _username;
        this._password = _password;
    }

    public boolean checkPassword(String _password) {
        return this._password.equals(_password);
    }
    
}
