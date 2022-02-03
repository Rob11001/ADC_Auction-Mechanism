package it.unisa.studenti.bruno.auction;

import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

import it.unisa.studenti.bruno.auction.utilities.Message;

public class MessageListenerImpl implements MessageListener {
    private Panel _notify_panel;
    private AuctionMechanismImpl auction_mech;

    public MessageListenerImpl(AuctionMechanismImpl auction_mech, Panel _notify_panel) {
        this._notify_panel = _notify_panel;
        this.auction_mech = auction_mech;
    }

    @Override
    public Object parseMessage(Object request) {
        Message msg = (Message) request;
        if(_notify_panel.getChildCount() > 5) {
            _notify_panel.removeAllComponents();
            _notify_panel.addComponent(new Label("Updates: "));
        }
        String str_msg = "";
        switch (msg._type) {
            case REJECTED:
                str_msg = String.format("- Your bid for the auction \"%s\" has been replaced", msg._auction_name);
                for(int i = 0; i < auction_mech.my_bidder_list.size(); i++) {
                    if(auction_mech.my_bidder_list.get(i).element0().equals(msg._auction_name)) {
                        auction_mech.my_bidder_list.remove(i);
                        break;
                    }
                }
                break;
            case END_BIDDER:
                str_msg = String.format("- The auction \"%s\" is over, and you won with a bid of %.2f", msg._auction_name, msg._bid_amount);
                break;
            case END_OWNER:
                str_msg = String.format("- Your auction \"%s\" is over with %d bids", msg._auction_name, msg._num_of_bids);
                break;
            default:
                break;
        }
        _notify_panel.addComponent(new Label(str_msg));

        return "success";
    }
    
}
