package it.unisa.studenti.bruno.auction;

import org.junit.jupiter.api.*;

import it.unisa.studenti.bruno.auction.utilities.Auction;
import it.unisa.studenti.bruno.auction.utilities.Pair;
import it.unisa.studenti.bruno.auction.utilities.State;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.List;


public class AuctionMechanismImplTest  {
    static private AuctionMechanismImpl peer_1, peer_2, peer_3, peer_4;
    static private String MASTER = "127.0.0.1";

    static class MessageListenerImpl implements MessageListener {
        @Override
        public Object parseMessage(Object request) {
            return "success";
        }
        
    }

    @BeforeAll
    static void setup() throws Exception {
        // Peer initialization
        peer_1 = new AuctionMechanismImpl(0, MASTER);
        peer_1.setMessageListener(new MessageListenerImpl());

        peer_2 = new AuctionMechanismImpl(1, MASTER);
        peer_2.setMessageListener(new MessageListenerImpl());

        peer_3 = new AuctionMechanismImpl(2, MASTER);
        peer_3.setMessageListener(new MessageListenerImpl());
        
        peer_4 = new AuctionMechanismImpl(3, MASTER);
        peer_4.setMessageListener(new MessageListenerImpl());
    
        peer_1.register("Peer_1", "Peer_1");
        peer_2.register("Peer_2", "Peer_2");
        peer_3.register("Peer_3", "Peer_3");
        peer_4.register("Peer_4", "Peer_4");
    }

    @Test
    void testRegister() {
        assertTrue(peer_1.register("Test", "Test"), "The registration of Peer_1 didn't go well");
        
        // Cannot create two user with the same username
        assertFalse(peer_1.register("Test", "Password"), "New user created"); 
    }

    @Test
    void testLogin() {
        assertTrue(peer_1.login("Peer_1", "Peer_1"), "The login of Peer_1 didn't go well");
        assertNotNull(peer_1.user, "The user of peer_1 is null");
        
        // Wrong password
        assertFalse(peer_2.login("Peer_2", "Per_2"), "The login of Peer_2 went well");
        assertNull(peer_2.user, "The user of peer_2 is not null");

        // User doesn't exist
        assertFalse(peer_3.login("Peer_5", "Peer_5"), "The login of Peer_5 went well");
        assertNull(peer_3.user, "The user of peer_3 is not null");
    }

    @Test
    void testLogout() {
        assertTrue(peer_1.login("Peer_1", "Peer_1"), "The login of Peer_1 didn't go well");
        assertNotNull(peer_1.user, "The user of peer_1 is null");

        assertTrue(peer_1.logout(), "The logout of Peer_1 didn't go well");
        assertNull(peer_1.user, "The user of peer_1 is not null");
    }


    @Nested
    class PostLoginTest {
        @BeforeEach
        void login() {
            peer_1.login("Peer_1", "Peer_1");
            peer_2.login("Peer_2", "Peer_2");
            peer_3.login("Peer_3", "Peer_3");
            peer_4.login("Peer_4", "Peer_4");
        }

        @AfterEach
        void logout() {
            if(peer_1.user != null) peer_1.logout();
            if(peer_2.user != null) peer_2.logout();
            if(peer_3.user != null) peer_3.logout();
            if(peer_4.user != null) peer_4.logout();
        }

        @Test
        void testCreateAuction() {
            assertTrue(peer_1.createAuction("Auction_1", new Date(), 4.2, 10, "Description"), "The auction creation didn't go well");
            assertEquals("Auction_1", peer_1.my_auctions_list.get(0), "The peer_1 didn't update its auctions list");

            // Cannot create two auctions with same name
            assertFalse(peer_2.createAuction("Auction_1", new Date(), 4.5, 10, "Description"), "The auction creation went well");
            assertEquals(0, peer_2.my_auctions_list.size(), "The peer_1 updated its auctions list");

            // Not logged peers cannot create auctions
            assertTrue(peer_3.logout(), "The logout of the peer_3 didn't go well");
            assertFalse(peer_3.createAuction("Auction_3", new Date(), 4.5, 1, "Description"), "The auction creation went well");
        }

        @Test
        void testCheckAuction() {
            assertTrue(peer_1.createAuction("Auction_to_check", new Date((new Date()).getTime() + 1000), 10, 2, "Description"), "The auction creation didn't go well");

            Auction a = peer_2.checkAuction("Auction_to_check");
            assertNotNull(a, "The auction's research didn't go well");
            assertEquals("Auction_to_check", a._auction_name, "The auction retrieved is the wrong one");

            // Searches an auction which does not exist
            assertNull(peer_2.checkAuction("Auction_42"), "The auction retrieving went well");

            // Checks if the state of an auction is updated properly
            assertTrue(peer_1.createAuction("Auction_expired", new Date((new Date()).getTime() - 1000), 10, 2, "Description"), "The auction creation didn't go well");
            a = peer_2.checkAuction("Auction_expired");
            assertNotNull(a, "The auction's research didn't go well");
            assertEquals("Auction_expired", a._auction_name, "The auction retrieved is the wrong one");
            assertEquals(State.CLOSED, a._auction_state, "The state of the auction is wrong");
        }

        @Test
        void testPlaceAbid() {
            assertTrue(peer_4.createAuction("Auction_Bid", new Date((new Date()).getTime() + 1000), 42, 1, "Description"), "The auction creation didn't go well");

            // Correct case
            assertNotNull(peer_2.placeAbid("Auction_Bid", 43), "Peer_2 wasn't able to place a bid");
            assertEquals("Auction_Bid", peer_2.my_bidder_list.get(0).element0(), "peer_2 bid list has not been updated");

            // Bid's value too low to place the bid
            assertNull(peer_1.placeAbid("Auction_Bid", 0), "Peer_1 was able to place a bid");
            assertEquals(0, peer_1.my_bidder_list.size(), "Peer_1 bid list has been updated incorrectly");

            // Cannot place a bid for an auction which does not exist
            assertNull(peer_1.placeAbid("Auctionnnnn", 42.0), "Peer_1 was able to place a bid");
            assertEquals(0, peer_1.my_bidder_list.size(), "Peer_1 bid list has been updated incorrectly");

            // An user cannot place a bid for his auctions
            assertNull(peer_4.placeAbid("Auction_Bid", 44), "Peer_4 was able to place a bid");
            assertEquals(0, peer_4.my_bidder_list.size(), "Peer_4 bid list has been updated incorrectly");

            // An user cannot place another bid for an auction for which his last bid is still "active"
            assertNull(peer_2.placeAbid("Auction_Bid", 44), "Peer_2 was able to place another bid");
            assertEquals(1, peer_2.my_bidder_list.size(), "peer_2 bid list has been updated incorrectly");   

            // A bid can be replace by a new one with a greater value 
            Auction updated_auction = peer_3.placeAbid("Auction_Bid", 100);
            assertNotNull(updated_auction, "Peer_3 wasn't able to place a bid");
            assertEquals("Peer_3", updated_auction._bid_list.get(0)._bid_owner, "The bid list of the auction has not been correclty updated");

            // Cannot place a bid for an expired auction
            assertTrue(peer_4.createAuction("Expired_auction", new Date((new Date()).getTime() - 1000), 10, 1, "Description"), "Peer_4 could not create a new auction");
            assertNull(peer_1.placeAbid("Expired_auction", 42), "Peer_1 was able to place a bid");
            assertEquals(0, peer_1.my_bidder_list.size(), "Peer_1 bid list has been updated incorrectly");
            
            // Not logged peers cannot place a bid
            assertTrue(peer_1.logout(), "Peer_1 wasn't able to logout");
            assertNull(peer_1.placeAbid("Auction_Bid", 101), "peer_1 was able to place a bid incorrectly");
        }

        @Test
        void testGetListOfAuctions() {
            // List of auctions found
            assertTrue(peer_1.createAuction("Z_0", new Date(), 10, 1, "Description"), "Peer_1 wasn't able to create an auction");
            assertTrue(peer_2.createAuction("Z_1", new Date(), 10, 1, "Description"), "Peer_2 wasn't able to create an auction");
            assertTrue(peer_3.createAuction("Z_2", new Date(), 10, 1, "Description"), "Peer_3 wasn't able to create an auction");
            assertTrue(peer_4.createAuction("Z_3", new Date(), 10, 1, "Description"), "Peer_4 wasn't able to create an auction");

            List<Pair<String, String>> list = peer_1.getListOfAuctions('z');
            assertEquals(4, list.size(), "List size is incorrect");
            
            for(int i = 0; i < 4; i++)
                assertEquals(String.format("Z_%d", i), list.get(i).element0(), "The list is not properly sorted");

            // No list of auctions found
            assertEquals(0, peer_4.getListOfAuctions('x').size(), "The list has a size different from zero");
        }
    }

    @AfterAll
    static void shutdown() {
        assertTrue(peer_1.leaveNetwork(), "peer_1 didn't leave the network properly");
        assertTrue(peer_2.leaveNetwork(), "peer_2 didn't leave the network properly");
        assertTrue(peer_3.leaveNetwork(), "peer_3 didn't leave the network properly");
        assertTrue(peer_4.leaveNetwork(), "peer_4 didn't leave the network properly");
    }
}