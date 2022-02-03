package it.unisa.studenti.bruno.auction.utilities;

import java.io.Serializable;

public class Message implements Serializable {
    public Type _type;
    public String _auction_name;
    public double _bid_amount;
    public int _num_of_bids;

    public Message(Type _type, String _auction_name) {
        this._type = _type;
        this._auction_name = _auction_name;
    }

    public Message(Type _type, String _auction_name, double _bid_amount) {
        this._type = _type;
        this._auction_name = _auction_name;
        this._bid_amount = _bid_amount;
    }

    public Message(Type _type, String _auction_name, int _num_of_bids) {
        this._type = _type;
        this._auction_name = _auction_name;
        this._num_of_bids = _num_of_bids;
    }

}
