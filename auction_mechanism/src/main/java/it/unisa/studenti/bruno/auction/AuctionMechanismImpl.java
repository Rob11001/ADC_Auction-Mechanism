package it.unisa.studenti.bruno.auction;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.unisa.studenti.bruno.auction.utilities.Auction;
import it.unisa.studenti.bruno.auction.utilities.User;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;
import net.tomp2p.utils.Pair;

public class AuctionMechanismImpl implements AuctionMechanism {
    private final Peer peer;
	private final PeerDHT _dht;
	private final int DEFAULT_MASTER_PORT = 4000;
    private User user;

    private final List<String> my_auctions_list = new ArrayList<>();
    private final List<Pair<String, String>> my_bidder_list = new ArrayList<>();

    public AuctionMechanismImpl(int _id, String _master_peer, final MessageListener _listener) throws Exception {
        peer = new PeerBuilder(Number160.createHash(_id)).ports(DEFAULT_MASTER_PORT + _id).start();
        _dht = new PeerBuilderDHT(peer).start();
        
        FutureBootstrap fb = peer.bootstrap().inetAddress(InetAddress.getByName(_master_peer)).ports(DEFAULT_MASTER_PORT).start();
		fb.awaitUninterruptibly();
		
        if(fb.isSuccess()) {
		    peer.discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
        } else { 
		    throw new Exception("Error in master peer bootstrap.");
        }

        peer.objectDataReply(new ObjectDataReply() {
			
			public Object reply(PeerAddress sender, Object request) throws Exception {
				return _listener.parseMessage(request);
			}
            
		});
    }

    
    @Override
    public boolean register(String _username, String _password) {
        try {
            FutureGet futureGet = _dht.get(Number160.createHash(_username)).start();
            futureGet.awaitUninterruptibly();
            // If doesn't exist any user with that username, we put it
            if(futureGet.isSuccess() && futureGet.isEmpty()) {
                _dht.put(Number160.createHash(_username)).data(new Data(new User(peer.peerAddress(), _username, Number160.createHash(_password)))).start().awaitUninterruptibly();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean login(String _username, String _password) {
        try {
            FutureGet futureGet = _dht.get(Number160.createHash(_username)).start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                User tmp = (User) futureGet.data().object();
                if(tmp.checkPassword(Number160.createHash(_password))) {
                    user = tmp;
                    user._peer_address = peer.peerAddress();
                    // Updates User info with his new peerAddress
                    _dht.put(Number160.createHash(_username)).data(new Data(user)).start().awaitUninterruptibly();
                    
                    // Gets user's auctions saved as a list of pair (auction_name, author_name)
                    futureGet = _dht.get(Number160.createHash(_username + Number160.createHash(_username))).start();
                    futureGet.awaitUninterruptibly();
                    if(futureGet.isSuccess()) {
                        if(futureGet.isEmpty()) return true;
                        List<Pair<String, String>> auctions = (List<Pair<String, String>>) futureGet.data().object();
                        for(Pair<String, String> auction : auctions) {
                            if(auction.element1().equals(_username))
                                my_auctions_list.add(auction.element0());
                            else
                                my_bidder_list.add(auction);
                        }
                    }

                    return true;
                }
            }
        } catch (Exception e) {
            user = null;
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean createAuction(String _auction_name, Date _end_time, double _reserved_price, int _num_products, String _description) {
        if(user == null) return false;
        try {
        Auction auction = new Auction(_auction_name, user._username, _description, _num_products, _reserved_price, _end_time);
        FutureGet futureGet = _dht.get(Number160.createHash(_auction_name + user._username)).start();
        futureGet.awaitUninterruptibly();
        // Checks if the auction doesn't exist
        if(futureGet.isSuccess() && futureGet.isEmpty()) {
            _dht.put(Number160.createHash(_auction_name + user._username)).data(new Data(auction)).start().awaitUninterruptibly();
            
            // Updates user's auctions list and global auctions list
            my_auctions_list.add(_auction_name);
            // TODO

            
            return true;
        }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    @Override
    public String checkAuction(String _auction_name) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String placeAbid(String _auction_name, double _bid_amount) {
        // TODO Auto-generated method stub
        return null;
    }


}
