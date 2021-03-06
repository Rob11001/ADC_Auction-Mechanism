package it.unisa.studenti.bruno.auction;
import java.util.Date;
import java.util.List;

import it.unisa.studenti.bruno.auction.utilities.Auction;
import it.unisa.studenti.bruno.auction.utilities.Pair;

/**
Copyright 2017 Universita' degli Studi di Salerno
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
An auction mechanism API based on P2P . 
Each peer can sell and buy goods using a Second-Price Auctions (EBay). 
second-price auction is a non-truthful auction mechanism for multiple items.
Each bidder places a bid. The highest bidder gets the first slot,
the second-highest, the second slot and so on, but the highest 
bidder pays the price bid by the second-highest bidder, 
the second-highest pays the price bid by the third-highest, and so on. 
*/

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