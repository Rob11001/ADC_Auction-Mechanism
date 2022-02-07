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
The interface implemented is the following:
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
