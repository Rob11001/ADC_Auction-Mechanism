package it.unisa.studenti.bruno.auction;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private final String GLOBAL_AUCTIONS_LIST = "";
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
                    
                    // Gets user's auctions saved as a list of pairs (auction_name, author_name)
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
                // Auction creation
                _dht.put(Number160.createHash(_auction_name + user._username)).data(new Data(auction)).start().awaitUninterruptibly();
                
                // Updates user's auctions list and global auctions list
                
                // Global auctions list update
                Number160 global_list_key = Number160.createHash(GLOBAL_AUCTIONS_LIST + Character.toLowerCase(_auction_name.charAt(0)));
                futureGet =_dht.get(global_list_key).start();
                futureGet.awaitUninterruptibly();
                if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                    // Saves the new auctions in its list in order
                    List<Pair<String, String>> auctions_list =  (List<Pair<String, String>>) futureGet.data().object();
                    int pos = binarySearch(auctions_list, _auction_name);
                    auctions_list.add(pos, new Pair<String,String>(_auction_name, user._username));
                    _dht.put(global_list_key).data(new Data(auctions_list)).start().awaitUninterruptibly();
                }

                // user's auctions list update
                Number160 my_list_key = Number160.createHash(user._username + Number160.createHash(user._username));
                futureGet = _dht.get(my_list_key).start();
                futureGet.awaitUninterruptibly();
                List<Pair<String, String>> list;
                if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                    list = (List<Pair<String, String>>) futureGet.data().object();
                } else {
                    list = new ArrayList<>();
                }
                list.add(new Pair<String,String>(_auction_name, user._username));
                _dht.put(my_list_key).data(new Data(list)).start().awaitUninterruptibly();
                my_auctions_list.add(_auction_name);

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

    private int binarySearch(List<Pair<String, String>> list, String key) {
        int len = list.size();
        if(len == 0) return 0;

        int i = 0, j = len;
        while(j - i > 1) {
            int tmp = (j + i) / 2;
            Pair<String, String> tmp_pair = list.get(tmp);
            int cmp = tmp_pair.element0().compareTo(key);
            if(cmp < 0) {
                i = tmp;
            } else if(cmp > 0) {
                j = tmp;
            } else {
                return tmp;
            }
        }
        if(list.get(i).element0().compareTo(key) <= 0 && list.get(j).element0().compareTo(key) >= 0) {
            return j;
        } else if(list.get(i).element0().compareTo(key) < 0) {
            return i;
        } else {
            return j + 1;
        }   
    }


}
