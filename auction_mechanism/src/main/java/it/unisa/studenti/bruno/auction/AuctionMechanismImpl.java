package it.unisa.studenti.bruno.auction;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import it.unisa.studenti.bruno.auction.utilities.Auction;
import it.unisa.studenti.bruno.auction.utilities.Bid;
import it.unisa.studenti.bruno.auction.utilities.Message;
import it.unisa.studenti.bruno.auction.utilities.Pair;
import it.unisa.studenti.bruno.auction.utilities.State;
import it.unisa.studenti.bruno.auction.utilities.Type;
import it.unisa.studenti.bruno.auction.utilities.User;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class AuctionMechanismImpl implements AuctionMechanism {
    private final Peer peer;
	private final PeerDHT _dht;
	private final int DEFAULT_MASTER_PORT = 4000;
    private final String GLOBAL_AUCTIONS_LIST = "GLOBAL_AUCTION_MECHANISM" + Number160.createHash("GLOBAL_AUCTION_MECHANISM");
    public User user;

    public final List<String> my_auctions_list = new ArrayList<>();
    public final List<Pair<String, String>> my_bidder_list = new ArrayList<>();

    public AuctionMechanismImpl(int _id, String _master_peer) throws Exception {
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
				return null;
			}
            
		});
    }

    /**
     * Sets a new MessageListener
     * @param _listener the MessageLister to set
     */
    public void setMessageListener(final MessageListener _listener) {
        peer.objectDataReply(new ObjectDataReply() {

            @Override
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
    @SuppressWarnings("unchecked")
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
                    futureGet = _dht.get(generateUserKey(_username)).start();
                    futureGet.awaitUninterruptibly();
                    if(futureGet.isSuccess()) {
                        if(futureGet.isEmpty()) return true;
                        
                        // Updates my_bidder_list and my_auctions_list
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
    @SuppressWarnings("unchecked")
    public boolean createAuction(String _auction_name, Date _end_time, double _reserved_price, int _num_products, String _description) {
        if(user == null) return false;
        try {
            FutureGet futureGet = _dht.get(Number160.createHash(_auction_name)).start();
            futureGet.awaitUninterruptibly();
            // Checks if the auction doesn't exist
            if(futureGet.isSuccess() && futureGet.isEmpty()) {
                // Auction creation
                Auction auction = new Auction(_auction_name, user._username, _description, _num_products, _reserved_price, _end_time);
                _dht.put(Number160.createHash(_auction_name)).data(new Data(auction)).start().awaitUninterruptibly();
                
                // Updates user's auctions list and global auctions list
                
                // Global auctions list update
                Number160 global_list_key = Number160.createHash(GLOBAL_AUCTIONS_LIST + Character.toLowerCase(_auction_name.charAt(0)));
                futureGet =_dht.get(global_list_key).start();
                futureGet.awaitUninterruptibly();
                if(futureGet.isSuccess()) {
                    List<Pair<String, String>> auctions_list;
                    if(futureGet.isEmpty()) {
                        auctions_list = new ArrayList<>();
                    } else {
                        // Saves the new auctions in its list in order
                        auctions_list =  (List<Pair<String, String>>) futureGet.data().object();
                    }

                    // Finds the position in which needs to be placed the new auction
                    int pos = binarySearch(auctions_list, _auction_name);
                    if(pos == - 1) pos = 0;
                    auctions_list.add(pos, new Pair<>(_auction_name, user._username));
                    _dht.put(global_list_key).data(new Data(auctions_list)).start().awaitUninterruptibly();
                }

                // user's auctions list update
                Number160 my_list_key = generateUserKey(user._username);
                futureGet = _dht.get(my_list_key).start();
                futureGet.awaitUninterruptibly();
                List<Pair<String, String>> list;
                if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                    list = (List<Pair<String, String>>) futureGet.data().object();
                } else {
                    list = new ArrayList<>();
                }
                list.add(new Pair<>(_auction_name, user._username));
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
    public Auction checkAuction(String _auction_name) {
        try {
            // Gets auction
            FutureGet futureGet = _dht.get(Number160.createHash(_auction_name)).start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                Auction auction = (Auction) futureGet.data().object();
                checkAndUpdateState(auction); // Updates auction's state if it's necessary
                
                return auction;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    
    @Override
    @SuppressWarnings("unchecked")
    public Auction placeAbid(String _auction_name, double _bid_amount) {
        // Need to control that it's not in my_auction_list/bidder_list
        for (Pair<String, String> pair : my_bidder_list)
            if(pair.element0().equals(_auction_name))
                return null;
        
        for(String auction : my_auctions_list) 
            if(auction.equals(_auction_name))
                return null;
            
        try {
            // Gets auction
            FutureGet futureGet = _dht.get(Number160.createHash(_auction_name)).start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                Auction auction = (Auction) futureGet.data().object();
                
                if(!checkAndUpdateState(auction)) return null; // Returns null if the state of the auction becomes CLOSED
                
                String deleted_user = auction.placeAbid(_bid_amount, user._username);
                if(deleted_user != null) {
                    if(deleted_user.equals(user._username)) return null;    // The current user cannot place the bid

                    // An user has been excluded 
                    futureGet = _dht.get(Number160.createHash(deleted_user)).start();
                    futureGet.awaitUninterruptibly();
                    if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                        User removed_user = (User) futureGet.data().object();
                        if(removed_user._peer_address != null) {
                            // Sends a notification if the peer_address is set
                            FutureDirect futureDirect = _dht.peer().sendDirect(removed_user._peer_address).object(new Message(Type.REJECTED, _auction_name)).start();
						    futureDirect.awaitUninterruptibly();
                        }

                        // Updates bidder list of removed_user
                        Number160 user_key = generateUserKey(removed_user._username);
                        futureGet = _dht.get(user_key).start();
                        futureGet.awaitUninterruptibly();
                        if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                            List<Pair<String, String>> list = (List<Pair<String, String>>) futureGet.data().object();
                            for(int i = 0; i < list.size(); i++)
                                if(list.get(i).element0().equals(_auction_name)) {
                                    list.remove(i);
                                    break;
                                }

                            _dht.put(user_key).data(new Data(list)).start().awaitUninterruptibly();
                        }
                    }
                }
                
                // Updates the auction with a new bid
                _dht.put(Number160.createHash(_auction_name)).data(new Data(auction)).start().awaitUninterruptibly();                
                
                // Updates the bidder_list of the user both locally and in DHT
                Number160 my_list_key = generateUserKey(user._username);
                futureGet = _dht.get(my_list_key).start();
                futureGet.awaitUninterruptibly();
                List<Pair<String, String>> list;
                if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                    list = (List<Pair<String, String>>) futureGet.data().object();
                } else {
                    list = new ArrayList<>();
                }
                list.add(new Pair<>(_auction_name, auction._author));
                _dht.put(my_list_key).data(new Data(list)).start().awaitUninterruptibly();
                my_bidder_list.add(new Pair<>(_auction_name, auction._author));

                return auction;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Pair<String, String>> getListOfAuctions(Character index) {
        try {
            FutureGet futureGet = _dht.get(Number160.createHash(GLOBAL_AUCTIONS_LIST + Character.toLowerCase(index))).start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                return ((List<Pair<String, String>>) futureGet.data().object());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return new ArrayList<>();
    }

    @Override
    public boolean logout() {
        try {
            user._peer_address = null;
            _dht.put(Number160.createHash(user._username)).data(new Data(user)).start().awaitUninterruptibly();
            user = null;
            my_auctions_list.clear();
            my_bidder_list.clear();
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Allows the peer to leave the network
     * @return a boolean value which represents the result of the operation
     */
    public boolean leaveNetwork() {
        try {
            if(user != null) {
                user._peer_address = null;
                _dht.put(Number160.createHash(user._username)).data(new Data(user)).start().awaitUninterruptibly();
            }
            user = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        _dht.peer().announceShutdown().start().awaitUninterruptibly();
        
        return true;
    }

    /**
     * Checks if the date of the auction is still valid, and if it's not, it updates the auction status
     * @param auction the Auction to check
     * @return true if the auction date is valid, false otherwise
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private boolean checkAndUpdateState(Auction auction) throws Exception {
        if(!isAValidDate(auction._end_time) && auction._auction_state == State.AVAILABLE) {
            auction._auction_state = State.CLOSED;
            _dht.put(Number160.createHash(auction._auction_name)).data(new Data(auction)).start().awaitUninterruptibly();
            
            // Need to remove auction from global auctions list
            Number160 global_list_key = Number160.createHash(GLOBAL_AUCTIONS_LIST + Character.toLowerCase(auction._auction_name.charAt(0)));
            FutureGet futureGet =_dht.get(global_list_key).start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess()) {
                // Removes the auction from the global list
                List<Pair<String, String>> auctions_list;
                if(futureGet.isEmpty()) {
                    auctions_list = new ArrayList<>();
                } else {
                    auctions_list = (List<Pair<String, String>>) futureGet.data().object();
                }
                
                int pos = binarySearch(auctions_list, auction._auction_name);
                if(pos != -1 && auctions_list.get(pos).element0().equals(auction._auction_name))    // To be sure to remove the right auction 
                    auctions_list.remove(pos);
                _dht.put(global_list_key).data(new Data(auctions_list)).start().awaitUninterruptibly();

                // Notification to the owner
                String author = auction._author;
                futureGet = _dht.get(Number160.createHash(author)).start();
                futureGet.awaitUninterruptibly();
                if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                    User owner = (User) futureGet.data().object();
                    if(owner._peer_address != null) {
                        // Sends a notification if the peer_address is set
                        FutureDirect futureDirect = _dht.peer().sendDirect(owner._peer_address).object(new Message(Type.END_OWNER, auction._auction_name, auction._bid_list.size())).start();
                        futureDirect.awaitUninterruptibly();
                    }
                }

                // Notification to the bidders
                for(int i = 0; i < auction._bid_list.size(); i++) {
                    Bid bid = auction._bid_list.get(i);
                    futureGet = _dht.get(Number160.createHash(bid._bid_owner)).start();
                    futureGet.awaitUninterruptibly();
                    if(futureGet.isSuccess() && !futureGet.isEmpty()) {
                        User bidder = (User) futureGet.data().object();
                        if(bidder._peer_address != null) {
                            // Sends a notification if the peer_address is set
                            // Calculates the price to pay for each bidder
                            double bidder_value_to_pay = (i == auction._bid_list.size() - 1) ? auction._reserved_price : auction._bid_list.get(i + 1)._bid_value;
                            FutureDirect futureDirect = _dht.peer().sendDirect(bidder._peer_address).object(new Message(Type.END_BIDDER, auction._auction_name, bidder_value_to_pay)).start();
                            futureDirect.awaitUninterruptibly();
                        }
                    }
                }

            }

            return false;
        }

        return true;
    }

    /**
     * Searches the specified key in the list of pair
     * @param list a list of pair <String, String>
     * @param key a String which represents the key
     * @return -1 if the list is empy, the pos of the key in the list if the key is in the list or if it is not in the list
     *          the pos representing the position that the key should occupy in the list (in the sorted list)
     */
    private int binarySearch(List<Pair<String, String>> list, String key) {
        int len = list.size();
        if(len == 0) return -1;

        int i = 0, j = len - 1;
        while (j - i > 1) {
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

        if (list.get(i).element0().compareTo(key) <= 0 && list.get(j).element0().compareTo(key) >= 0) {
            return j;
        } else if(list.get(i).element0().compareTo(key) < 0) {
            return i;
        } else {
            return j + 1;
        }   
    }

    /**
     * Checks if the date passed is after the current date
     * @param date a Date object
     * @return true if the date passed is after the current date, false otherwise
     */
    private boolean isAValidDate(Date date) {
        Calendar current = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        Calendar c_date = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        c_date.setTime(date);
        return c_date.after(current);
    }

    /**
     * Generates the hash for the user's auctions list
     * @param username a String representing the user's username
     * @return the Number160 representing the hash for the user's auctions list
     */
    private Number160 generateUserKey(String username) {
        return Number160.createHash(username + Number160.createHash(username));
    }

}