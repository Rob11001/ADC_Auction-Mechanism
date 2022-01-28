package it.unisa.studenti.bruno.auction.utilities;

import java.io.Serializable;

import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;

public class User implements Serializable {
    public PeerAddress _peer_address;
    public final String _username;
    private Number160 _password;

    public User(PeerAddress _peer_address, String _username, Number160 _password) {
        this._peer_address = _peer_address;
        this._username = _username;
        this._password = _password;
    }

    public boolean checkPassword(Number160 _password) {
        return this._password.equals(_password);
    }

    public void resetPassword() {
        this._password = null;
    }
    
}
