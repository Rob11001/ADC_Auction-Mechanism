package it.unisa.studenti.bruno.auction;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;


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
    class WorkflowTest {
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


    }


    @AfterAll
    static void shutdown() {
        assertTrue(peer_1.leaveNetwork(), "peer_1 didn't leave the network properly");
        assertTrue(peer_2.leaveNetwork(), "peer_2 didn't leave the network properly");
        assertTrue(peer_3.leaveNetwork(), "peer_3 didn't leave the network properly");
        assertTrue(peer_4.leaveNetwork(), "peer_4 didn't leave the network properly");
    }
}
