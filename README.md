<!-- Title -->

# Auction Mechanism
|**Auction Mechanism**|**Bruno Roberto**| 
|---|---|
<br>

<!-- TABLE OF CONTENTS -->

<details open="open">
  <summary><h1 style="display: inline-block">Table of Contents</h1></summary>
  <ol>
    <li><a href="#">Introduction</a></li>
    <li><a href="#license">License</a></li>
  </ol>
</details>

<br>

<!-- Introduction -->
# **Introduction**
## **Problem**
Design and develop an auction mechanism based on P2P Network. Each peer can sell and buy goods using second-price Auctions (eBay). Second-price auction is a non-truthful auction mechanism for multiple items. Each bidder places a bid. The highest bidder gets the first slot, the second-highest, the second slot, and so on, but the highest bidder pays the price bid by the second-highest bidder, the second-highest pays the price bid by the third-highest, and so on. The system allows the users to create new auction (with an ending time, a reserved selling price, and a description), check the status of an auction, and eventually place a new bid for an auction.

## **Tools**
The project has been developed in Java using the [TomP2P](https://tomp2p.net) library to create and manage a Distributed HashTable (DHT).

To handle the dependencies and installation has been used Maven, a project management tool.

In addition to provide a simple text-based GUIs has been used the [Lanterna](https://github.com/mabe02/lanterna) library. It's a simple Java library which allows to write easy semi-graphical user interfaces in a text-only environment, very similar to the C library curses. 

# **Solution's Overview**

## **Project's Structure**
The project's structure is the following:

<!-- Project structure image -->
![](/readme_images/structure.png)

We have the common Maven project layout.

The **auction** package contains the main class *App*, the interface *AuctionMechanism* and its implementation *AuctionMechanismImpl*, and also the interface *Message Listener* and its implementation *MessageListenerImpl*. 

Then it contains two sub-package too:
- **ui**
- **utilities**

The first one contains the two classes implemented for create the GUI, the second one a set of utility classes, like *Auction*, *Bid*, *Message*, *User* and others, used to describe the concept of an auction, an user and their features.

All the project's dependencies are expressed in the *pom.xml*:
<!-- Project's dependencies -->
![](/readme_images/dependencies.png)

## **AuctionMechanism**
The interface implemented for the project is the following:
```java
public interface AuctionMechanism {
	
	/**
	 * Allows user to register his information into the system
	 * @param username a String describing username
	 * @param password a String describing user's password
	 * @return true if the user's information are valid, false otherwise
	 */
	public boolean register(String username, String password);

	/**
	 * Allows user to login in the system
	 * @param username a String describing the username
	 * @param password a String describing the user's password
	 * @return true if the user's information match, false otherwise
	 */
	public boolean login(String username, String password);

	/**
	 * Allows user to logout from the system
	 * @return true if the operation goes well, false otherwise
	 */
	public boolean logout();
	
	/**
	 * Creates a new auction for a good.
	 * @param _auction_name a String, the name identify the auction.
	 * @param _end_time a Date that is the end time of an auction.
	 * @param _reserved_price a double value that is the reserve minimum pricing selling.
	 * @param _num_products a int value that is the number of products. 
	 * @param _description a String describing the selling goods in the auction.
	 * @return true if the auction is correctly created, false otherwise.
	 */
	public boolean createAuction(String _auction_name, Date _end_time, double _reserved_price, int _num_products, String _description);
	
	/**
	 * Checks the status of the auction.
	 * @param _auction_name a String, the name of the auction.
	 * @return the auction with the passed _auction_name or null if it doesn't exist.
	 */
	public Auction checkAuction(String _auction_name);
	
	/**
	 * Places a bid for an auction if it is not already ended.
	 * @param _auction_namea a String, the name of the auction.
	 * @param _bid_amount a double value, the bid for an auction.
	 * @return the updated auction.
	 */
	public Auction placeAbid(String _auction_name, double _bid_amount);
	
	/**
	 * Gets the list of open auctions which begins with index character
	 * @param index a Character value which represents the first character of the auctions returned
	 * @return a list of pair<auction name, author name> in which all auction names start with index
	 */
	public List<Pair<String, String>> getListOfAuctions(Character index);	
}
```

It has all the features of the initial interface [AuctionMechanism](https://github.com/spagnuolocarmine/distributedsystems-unisa/blob/master/homework/AuctionMechanism.java) with some additions:
- a peer can now create an account with which login in the system
- it's possible to retrieve not only a single auction, but also a list of auctions 

## **AuctionMechanismImpl**
The core class that handles all P2P interactions is **AuctionMechanismImpl**.

Using the [Publisher/Subscriber Example](https://github.com/spagnuolocarmine/p2ppublishsubscribe) as reference the constructor of the class is very similiar.

```java
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
				return request;
			}
            
		});
    }
```
In fact, it initiates the peer and does the boostrap using a "_master_peer" (a peer who is already in the network).

We can see also that the message listener it's not set in the constructor, but it can be set later with an appropriate setter to give more freedom to the developer.

```java
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
```

Every instance of *AuctionMechanismImpl*, in addition to peer instance, keep:
- an *User* object which represents the current peer (it's null if the peer is not logged in the system)
- two list of auctions (kept as string) which represents the auctions created by the user and the auctions for which the user has placed a bid

All the needed information are retrieved during the login phase as can see in the snippet below:

```java
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
```

As we can see various objects are kept in the DHT:
- the *User* object with its peer_address which can be used to notify the peer (note: this field can be null when the user is not logged in the system, and therefore the notifications sent during this time are simply lost)
- a *personal list* for each user. This is a list of *Pair<String, String>* (auction_name, author_name) that kept the information of the auctions that have been created by the user or for which the user has placed a bid
- the *Auction* object representing an auction with all its information
- a "Global auction list". This is a list of string representing the name of the auctions still "valid" (for which can be placed a bid).
Actually this isn't kept as a single object, but is divided in various sublists according to the first character of the auction name. In fact, the *getListOfAuctions* method allows to retrieve all the auctions (auction's names) who begin with the passed character. 
This choice was made to not have a single large list and try to balance it in many smaller sublists.

The objects described before are put on the DHT using as keys the **SHA-1** hash of different strings. More precisely for an auction as key is used the hash of its name, for an user the hash of its username, for a personal list the hash of the string generated by the concatenation of the user's username and the hash of the username. And for a Global auction list containing all the auctions with the name beginning with a character "x", the hash of the string generated by the concatenation of a constant string defined in the class and the character "x". 

To try to make everything clearer, let's see the **createAuction** method:

```java
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
```
Here we can see how an auction is retrieved and saved, and how the global list and the user's list are updated, all using the keys described before.

Another important aspect to discuss about is the way in which the state of an auction is updated. It was decided to use a "lazy" approach. In fact, the state of an auction is updated only when it is retrieved and it's the same peer who needed it to update the auction. Let's see the private method used to check and update the state if necessary:

```java
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
```
As we can see, when the state of an auction is updated we need to remove the auction from the global list because the time of the auction is over and cannot be placed any other bids, and also we need to notify the end of the auction (sending a message as we'll see later) to the author of the auction and all the user who had placed a bid for it.

## **MessageListener**
This class implements the *parseMessage* method, which is invoked whenever the peer receives a new message from another peer.

```java
public Object parseMessage(Object request) {
        Message msg = (Message) request;
        if(_notify_panel.getChildCount() > 5) {
            _notify_panel.removeAllComponents();
            _notify_panel.addComponent(new Label("Updates: "));
        }

        String str_msg = "";
        // The type of notification to generate depending on message type
        switch (msg._type) {
            case REJECTED:
                str_msg = String.format("- Your bid for the auction \"%s\" has been replaced", msg._auction_name);
                // Updates my_bidder_list
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
```
From the previous snippet we can see that there are three types of message that a peer can receive:
- REJECTED: a message used to notify a peer when its bid is replaced by a new one
- END_BIDDER: a message used to notify a bidder when an auction is over 
- END_OWNER: a message used to notify the owner of the auction when it's over

All the three types of message are shown in a notify_panel (a simple *Lantern Panel*).

<!--GUI-->
## **App**
The main class (*App.java*) handles the startup of the application. In fact, in *App* are instantiated the peer (*AuctionMechanismImpl*) and the *IndexPage*, which is a simple class that extends *Lanterna*'s *Window* class representing the login page.

![](/readme_images/index_page.png)

After login, we switch from *IndexPage* to *MainPage*. This like the previous one is a class that extends *Lanterna*'s *Window* class and represents the main page of the application. In fact, here thanks to a menu bar, the user is able to do all the possible operations like:
- see my personal list and logout
![](/readme_images/menu_item_1.png)
- create, search or get all auctions
![](/readme_images/menu_item_2.png)
- clean the notification panel
![](/readme_images/menu_item_3.png)

Here are some example of application screens: 

![](/readme_images/List_of_auctions.png)

![](/readme_images/auction.png)


<!-- Testing -->
# **Testing : JUnit**
The testing has been done through the usage of **JUnit** and the Maven plugin **surefire**.

The Test Case class is "*AuctionMechanismImplTest*", in which is tested every method of the implemented interface. For the testing have been instantiated four peer in a @BeforeAll method. This JUnit annotation allows that the method will be invoked only one before all the test methods (the methods annotated with @Test).

```java
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
```

Then at the end of the testing all the peer leave the network using another method annotated with @AfterAll.

```java
 @AfterAll
    static void shutdown() {
        assertTrue(peer_1.leaveNetwork(), "peer_1 didn't leave the network properly");
        assertTrue(peer_2.leaveNetwork(), "peer_2 didn't leave the network properly");
        assertTrue(peer_3.leaveNetwork(), "peer_3 didn't leave the network properly");
        assertTrue(peer_4.leaveNetwork(), "peer_4 didn't leave the network properly");
    }
```

Another important annotation used is **@Nested**. This JUnit annotation allows to group multiple test methods inside another class and in this way is possible to define other @BeforeAll, @AfterAll, @BeforeEach and @AfterEach methods that are applied only at the nested methods in the class. In fact, this annotation has been used to create a inner class "*PostLoginTest*" to test all the methods which required the login of the peers.

Here below a part of the "*PostLoginTest*" class with its @BeforeEach and @AfterEach methods.
```java
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
            // Correct auction creation
            assertTrue(peer_1.createAuction("Auction_1", new Date(), 4.2, 10, "Description"), "The auction creation didn't go well");
            assertEquals("Auction_1", peer_1.my_auctions_list.get(0), "The peer_1 didn't update its auctions list");

            // Cannot create two auctions with same name
            assertFalse(peer_2.createAuction("Auction_1", new Date(), 4.5, 10, "Description"), "The auction creation went well");
            assertEquals(0, peer_2.my_auctions_list.size(), "The peer_1 updated its auctions list");

            // Not logged peers cannot create auctions
            assertTrue(peer_3.logout(), "The logout of the peer_3 didn't go well");
            assertFalse(peer_3.createAuction("Auction_3", new Date(), 4.5, 1, "Description"), "The auction creation went well");
        }
```

<!-- Docker -->

# **Execution in a Docker Container**
The application can be easily executed in a Docker container.
The project has a *Dockerfile* in it, which can be used to launch the container.

Here is the *Dockerfile* content:

```dockerfile
FROM maven:3.5-jdk-8-alpine
WORKDIR /app
COPY ./auction_mechanism /app
RUN mvn package

FROM openjdk:8-jre-alpine
WORKDIR /app
ENV MASTERIP=127.0.0.1
ENV ID=0
COPY --from=0 /app/target/auction_mechanism-1.0-jar-with-dependencies.jar /app

CMD /usr/bin/java -jar auction_mechanism-1.0-jar-with-dependencies.jar -m $MASTERIP -id $ID
```

The steps to execute the application in a Docker container are the following:

**1.** Download/clone the current repository 

**2.** Open a terminal and move to the directory containing the Dockerfile

**3.** **Build the Dockerfile** using the following command:
```
docker build --no-cache -t p2p-auction-mechanism .
```
**4.** **Create a simple docker network** using the following command:

```
docker network create auction-mechanism-net 
```
We use a simple docker network because this user-defined bridge, unlike the default bridge, provides an automatic DNS resolution between containers. In fact, on a user-defined bridge network, containers can resolve each other by name.

**5.** **Run the Master Peer**:
```
docker run -it --name MASTER-PEER -e MASTERIP="127.0.0.1" -e ID=0 --network auction-mechanism-net p2p-auction-mechanism
```
The parameters in the *run* command are:
- **--name**: Name of the container
- **-e**: Environment variables
- **--network**: Specify the network in which run the container


**6.** **Run a generic Peer**: using the automatic DNS resolution we don't need to check the Master Peer IP and we can simply run the following command:

```
docker run -it --name PEER -e MASTERIP="MASTER-PEER" -e ID=1 --network auction-mechanism-net p2p-auction-mechanism
```
Note: Remember to use the same network and the correct Master Peer container's name. In addition you need to use unique identifiers for peers' ID.



<!-- Conclusioni -->

<!-- LICENSE -->

# **License**

Distributed under the MIT License. See `LICENSE` for more information.


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/github_username/repo.svg?style=for-the-badge
[contributors-url]: https://github.com/github_username/repo/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/github_username/repo.svg?style=for-the-badge
[forks-url]: https://github.com/github_username/repo/network/members
[stars-shield]: https://img.shields.io/github/stars/github_username/repo.svg?style=for-the-badge
[stars-url]: https://github.com/github_username/repo/stargazers
[issues-shield]: https://img.shields.io/github/issues/github_username/repo.svg?style=for-the-badge
[issues-url]: https://github.com/github_username/repo/issues
[license-shield]: https://img.shields.io/github/license/github_username/repo.svg?style=for-the-badge
[license-url]: https://github.com/github_username/repo/blob/master/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/github_username
