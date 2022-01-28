package it.unisa.studenti.bruno.auction.utilities;

import java.io.Serializable;

public class Bid implements Serializable {
    public final double _bid_value;
    public final String _bid_owner;

    public Bid(double _bid_value, String _bid_owner) {
        this._bid_value = _bid_value;
        this._bid_owner = _bid_owner;
    }

}
