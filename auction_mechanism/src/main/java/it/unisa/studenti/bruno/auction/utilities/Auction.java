package it.unisa.studenti.bruno.auction.utilities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Auction implements Serializable {
    public final String _auction_name;
    public final String _author;
    public String _description;
    public int _num_products;
    public double _reserved_price;
    public Date _end_time;
    public State _auction_state;
    private final List<Bid> _bid_list;
    // Categoria?

    public Auction(String _auction_name, String _author, String _description, int _num_products, double _reserved_price, Date _end_time) {
        this._auction_name = _auction_name;
        this._author = _author;
        this._description = _description;
        this._num_products = _num_products;
        this._reserved_price = _reserved_price;
        this._end_time = _end_time;   
        this._auction_state = State.AVAILABLE;
        this._bid_list = new ArrayList<>();
    }

    public String addBid(double _bid_value, String _bid_owner) {
        if(_bid_value <= _reserved_price)
            return null;
        
        // Bisogna vedere se c'è già una bid dell'utente?
        boolean added = false;
        int i = 0;
        int len = _bid_list.size();
        for(; i < len; i++) {
            if(_bid_list.get(i)._bid_value < _bid_value) {
                _bid_list.add(i, new Bid(_bid_value, _bid_owner));
                added = true;
                break;
            }
        }
        
        if(!added && len < _num_products)
            _bid_list.add(new Bid(_bid_value, _bid_owner));

        // Ritorna l'user escluso dall'asta
        if(_bid_list.size() > _num_products) {
            Bid last_bid = _bid_list.remove(_bid_list.size() - 1);
            _reserved_price = last_bid._bid_value;
            
            return last_bid._bid_owner;
        }

        return null;
    }

    
}
