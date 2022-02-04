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
    public final List<Bid> _bid_list;

    /**
     * Represents an auction
     * @param _auction_name name of the auction
     * @param _author username of the author of the auction
     * @param _description a brief description of the auction
     * @param _num_products the number of available products
     * @param _reserved_price the reserved price for the auction
     * @param _end_time the date in which the auction will end
     */
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
    
    /**
     * Places a new bid
     * @param _bid_value the value of the bid
     * @param _bid_owner the owner of the bid
     * @return null if it's all okay, otherwise a String which represents the username of the excluded user (note: this string can be also the _bid_owner)
     */
    public String placeAbid(double _bid_value, String _bid_owner) {
        // Checks if the bid_value is greater of the reserved_price
        if(_bid_value <= _reserved_price)
            return _bid_owner;
        
        // Checks if the bid can be added    
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
        
        if(!added && len < _num_products) {
            _bid_list.add(new Bid(_bid_value, _bid_owner));
            added = true;
        }

        // Checks if is necessary return a user whose bid has been excluded from the auction 
        if(_bid_list.size() > _num_products) {
            Bid last_bid = _bid_list.remove(_bid_list.size() - 1);
            _reserved_price = last_bid._bid_value;
            
            return last_bid._bid_owner;
        }

        return added ? null : _bid_owner;
    }

    
}
