package it.unisa.studenti.bruno.auction.ui;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.TextBox.Style;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.menu.Menu;
import com.googlecode.lanterna.gui2.menu.MenuBar;
import com.googlecode.lanterna.gui2.menu.MenuItem;

import it.unisa.studenti.bruno.auction.AuctionMechanismImpl;
import it.unisa.studenti.bruno.auction.MessageListenerImpl;
import it.unisa.studenti.bruno.auction.utilities.Auction;
import it.unisa.studenti.bruno.auction.utilities.Bid;
import it.unisa.studenti.bruno.auction.utilities.Pair;
import it.unisa.studenti.bruno.auction.utilities.State;

public class MainPage extends BasicWindow {
    public Panel body;
    public Panel notify_panel;
    static final int PAGE_SIZE = 5;

    public MainPage(AuctionMechanismImpl auction_mechanism) {
        super();
        
        // Menubar
        MenuBar menubar = new MenuBar();
        // "User" menu
        Menu _menu_user = new Menu("User");
        menubar.add(_menu_user);
        _menu_user.add(new MenuItem("My list", () -> {
            if(auction_mechanism.my_auctions_list.isEmpty()) {
                body.removeAllComponents();
                body.addComponent(new Label("You didn't create any auction"));
                
                return;
            }

            body.removeAllComponents();
            body.setLayoutManager(new GridLayout(2));
            
            body.addComponent(createTitleLabel("Auction name"));
            body.addComponent(new Label(" "));
            for (String auction_name : auction_mechanism.my_auctions_list) {
                body.addComponent(new Label(auction_name));

                Button check_button = new Button("Check");
                check_button.addListener((Button button) -> {
                    checkListener(auction_name, auction_mechanism);
                });
                body.addComponent(check_button);          
            }
        }));

        _menu_user.add(new MenuItem("My bids", () -> {
            if(auction_mechanism.my_bidder_list.isEmpty()) {
                body.removeAllComponents();
                body.addComponent(new Label("You didn't place any bids"));
                
                return;
            }

            body.removeAllComponents();
            body.setLayoutManager(new GridLayout(3));
            body.addComponent(createTitleLabel("Auction name"));
            body.addComponent(createTitleLabel("Auction's author"));
            body.addComponent(new Label(" "));
            for (Pair<String, String> pair : auction_mechanism.my_bidder_list) {
                body.addComponent(new Label(pair.element0()));
                body.addComponent(new Label(pair.element1()));

                Button check_button = new Button("Check");
                check_button.addListener((Button button) -> {
                    checkListener(pair.element0(), auction_mechanism);
                });
                body.addComponent(check_button);  
            }

            // Cancel button
            Button cancel_button = new Button("Cancel");
            body.addComponent(cancel_button);
            cancel_button.addListener((Button button) -> {
                body.removeAllComponents();
            });

        }));

        _menu_user.add(new MenuItem("Logout", () -> {
            try {
                if(auction_mechanism.logout()) {
                    WindowBasedTextGUI wbtGUI = this.getTextGUI().removeWindow(this);
                    this.close();
                    IndexPage idx_page = new IndexPage(auction_mechanism);
                    MultiWindowTextGUI gui = new MultiWindowTextGUI(wbtGUI.getScreen(), // I need to create a new one otherwise the older page remains
                        new DefaultWindowManager(),
                        new EmptySpace(TextColor.ANSI.BLUE));
                    gui.addWindowAndWait(idx_page);
                    auction_mechanism.setMessageListener((Object request) -> null);
                    
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Error message
            MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "There has been a problem with logout process");
        }));
        
        // "Auctions" menu
        Menu _menu_auctions = new Menu("Auctions");
        menubar.add(_menu_auctions);

        _menu_auctions.add(new MenuItem("Create", () -> {
            body.removeAllComponents();
            body.setLayoutManager(new GridLayout(2));
            
            // Auction info
            body.addComponent(new Label("Auction name: "));
            TextBox auction_name_box = new TextBox().setValidationPattern(Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,29}"));
            body.addComponent(auction_name_box);

            body.addComponent(new Label("Description: "));
            TextBox description_box = new TextBox("", Style.MULTI_LINE);
            body.addComponent(description_box);

            body.addComponent(new Label("Number of products: "));
            TextBox num_products_box = new TextBox(new TerminalSize(4, 1)).setValidationPattern(Pattern.compile("[1-9][0-9]*"));
            body.addComponent(num_products_box);

            body.addComponent(new Label("Reserved price: "));
            TextBox reserved_price_box = new TextBox(new TerminalSize(6, 1)).setValidationPattern(Pattern.compile("[1-9][0-9]*"));
            body.addComponent(reserved_price_box);

            Panel end_time_panel = new Panel(new GridLayout(9));
            body.addComponent(new Label("End time (yyyy-mm-dd hh:mm): "));
            TextBox year_box = new TextBox(new TerminalSize(5, 1))
                .setValidationPattern(Pattern.compile("[0-9]*"));
            end_time_panel.addComponent(year_box);
            end_time_panel.addComponent(new Label("-"));

            TextBox mounth_box = new TextBox(new TerminalSize(3, 1))
                .setValidationPattern(Pattern.compile("[0-9]*"));
            end_time_panel.addComponent(mounth_box);
            end_time_panel.addComponent(new Label("-"));

            TextBox day_box = new TextBox(new TerminalSize(3, 1))
                .setValidationPattern(Pattern.compile("[0-9]*"));
            end_time_panel.addComponent(day_box);
            end_time_panel.addComponent(new Label(" "));
            
            TextBox hour_box = new TextBox(new TerminalSize(3, 1))
                .setValidationPattern(Pattern.compile("[0-9]*"));
            end_time_panel.addComponent(hour_box);
            end_time_panel.addComponent(new Label(":"));

            TextBox min_box = new TextBox(new TerminalSize(3, 1))
                .setValidationPattern(Pattern.compile("[0-9]*"));
            end_time_panel.addComponent(min_box);

            body.addComponent(end_time_panel);

            // Create button
            Button create_button = new Button("Create");
            body.addComponent(create_button);
            create_button.addListener((Button button) -> {
                String auction_name = auction_name_box.getText();
                String description = description_box.getText();

                String num_products = num_products_box.getText();
                String reserved_price = reserved_price_box.getText();
                String year = year_box.getText();
                String month = mounth_box.getText();
                String dd = day_box.getText();
                String hh = hour_box.getText();
                String mm = min_box.getText();
                
                if(!isAvalidDate(year, month, dd, hh, mm)) {
                    // Error message
                    MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Invalid date format");
                    
                    return;
                }

                if(auction_name.length() > 0 && num_products.length() > 0 && reserved_price.length() > 0) {
                    try {
                        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
                        calendar.set(Calendar.YEAR, Integer.parseInt(year));
                        calendar.set(Calendar.MONTH, Integer.parseInt(month) - 1);
                        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dd));
                        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hh));
                        calendar.set(Calendar.MINUTE, Integer.parseInt(mm));
                        
                        Date end_time = calendar.getTime();
                        
                        if(auction_mechanism.createAuction(auction_name, end_time, Double.parseDouble(reserved_price), Integer.parseInt(num_products), description)) {
                            MessageDialog.showMessageDialog(this.getTextGUI(), "Success", "Auction has been successfully created");        
                            menubar.setVisible(true);
                            body.removeAllComponents();

                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Error message
                    MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Auction cannot be created");
                } else {
                    // Error message
                    MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Auction name\\number of products\\reserved price cannot be empty");                    
                }
        
            });

            // Cancel button
            Button cancel_button = new Button("Cancel");
            body.addComponent(cancel_button);
            cancel_button.addListener((Button button) -> {
                menubar.setVisible(true);
                body.removeAllComponents();
            });

            menubar.setVisible(false);
            body.setSize(getPreferredSize());      
        }));

        _menu_auctions.add(new MenuItem("Search", () -> {
            body.removeAllComponents();
            body.setLayoutManager(new GridLayout(2));

            body.addComponent(new Label("Auction name: "));
            TextBox search_box = new TextBox().setValidationPattern(Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,29}"));
            body.addComponent(search_box);
            
            // Cancel button
            Button cancel_button = new Button("Cancel");
            body.addComponent(cancel_button);
            cancel_button.addListener((Button button) -> {
                body.removeAllComponents();
            });
            body.addComponent(cancel_button);

            // Search button
            Button search_button = new Button("Search");
            search_button.addListener((Button s_button) -> {
                if(search_box.getText().length() == 0) {
                    // Error message
                    MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Invalid auction name");
                    
                    return;
                }

                checkListener(search_box.getText(), auction_mechanism);
            });
            body.addComponent(search_button);

            
        }));

        _menu_auctions.add(new MenuItem("All auctions", () -> {
            body.removeAllComponents();
            char index = 'a';
            List<Pair<String, String>> list = auction_mechanism.getListOfAuctions(index);
            createSearchPage(list, 0, auction_mechanism, index);
        }));

         // "Notifications" menu
        Menu _menu_notify = new Menu("Notifications");
        menubar.add(_menu_notify);

        _menu_notify.add(new MenuItem("Clean", () -> {
            notify_panel.removeAllComponents();
            notify_panel.addComponent(new Label("Updates: "));
        }));
 
        this.setMenuBar(menubar);

        // Body component
        body = new Panel();
        body.addComponent(new Label(String.format("Welcome %s", auction_mechanism.user._username)));
        
        // Notify component
        notify_panel = new Panel(new GridLayout(1));
        notify_panel.addComponent(new Label("Updates: "));
        
        this.setComponent(new Panel(new GridLayout(2))
            .addComponent(body)
            .addComponent(notify_panel)
        );

        // Message Listener
        auction_mechanism.setMessageListener(new MessageListenerImpl(auction_mechanism, notify_panel));
    }


    private boolean isAvalidDate(String year, String mounth, String day, String hh, String mm) {
        if(year.length() != 4 || Integer.parseInt(year) < 1900) 
            return false;
        if(mounth.length() != 2 || Integer.parseInt(mounth) < 1 || Integer.parseInt(mounth) > 12)
            return false;
        if(day.length() != 2 || Integer.parseInt(day) < 1 || Integer.parseInt(day) > 31)
            return false;
        if(hh.length() < 1 || hh.length() > 2 || Integer.parseInt(hh) < 0 || Integer.parseInt(hh) > 23)
            return false;
        if(mm.length() != 2 || Integer.parseInt(mm) < 0 || Integer.parseInt(mm) > 59)
            return false;
        
        return true;
    }

    private void checkListener(String auction_name, AuctionMechanismImpl auction_mechanism) {
        Auction auction;
        try {
            auction = auction_mechanism.checkAuction(auction_name);
            if(auction == null) {
                // Error message
                MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Problem in auction retrieved");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Problem in auction retrieved");
            return;
        }

        body.removeAllComponents();
        body.setLayoutManager(new GridLayout(1));
                    
        Panel auction_info_panel = new Panel(new GridLayout(2));
        body.addComponent(auction_info_panel);
                    
        Panel bids_panel = new Panel(new GridLayout(2));
        body.addComponent(bids_panel);

        auction_info_panel.addComponent(new Label("Auction name: "));
        auction_info_panel.addComponent(new Label(auction._auction_name));

        auction_info_panel.addComponent(new Label("Author: "));
        auction_info_panel.addComponent(new Label(auction._author));
                    
        auction_info_panel.addComponent(new Label("Description: "));
        auction_info_panel.addComponent(new Label(auction._description));

        auction_info_panel.addComponent(new Label("Number of products: "));
        auction_info_panel.addComponent(new Label("" + auction._num_products));
                    
        auction_info_panel.addComponent(new Label("Reserved price: "));
        auction_info_panel.addComponent(new Label("" + auction._reserved_price));

        auction_info_panel.addComponent(new Label("End time: "));
        Date date = auction._end_time;
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        calendar.setTime(date);
        auction_info_panel.addComponent(new Label(String.format("%d-%02d-%02d %02d:%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))));

        auction_info_panel.addComponent(new Label("Auction status: "));
        auction_info_panel.addComponent(new Label(auction._auction_state == State.AVAILABLE ? "Open" : "Closed"));
        
        if(!auction._bid_list.isEmpty()) {
            bids_panel.addComponent(new Label("Bids:"));
            Panel list = new Panel(new GridLayout(2));
            for (Bid bid : auction._bid_list) {
                list.addComponent(new Label(bid._bid_owner));
                list.addComponent(new Label(String.format("%.2f", bid._bid_value)));
            }
            bids_panel.addComponent(list);
        }
                    
        // "Place a bid" button
        if(auction._auction_state == State.AVAILABLE && !auction._author.equals(auction_mechanism.user._username)) {
            Panel place_a_bid_panel = new Panel(new GridLayout(2));
            TextBox bid_box = new TextBox().setValidationPattern(Pattern.compile("[0-9]*"));
            place_a_bid_panel.addComponent(bid_box);
            Button place_a_bid_button = new Button("Place a bid");
            place_a_bid_panel.addComponent(place_a_bid_button);
            // Listener for "place a bid" action
            place_a_bid_button.addListener((Button b) -> {
                String bid_text = bid_box.getText();
                if(bid_text.length() == 0) {
                    MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Invalid bid value");
                    return;
                }
                double bid_value = Double.parseDouble(bid_text);
                try {
                    Auction v = auction_mechanism.placeAbid(auction._auction_name, bid_value);
                    if(v != null) {
                        MessageDialog.showMessageDialog(this.getTextGUI(), "Success", "Bid placed");
                        bids_panel.removeAllComponents();
                        
                        bids_panel.addComponent(new Label("Bids:"));
                        Panel list = new Panel(new GridLayout(2));
                        for (Bid bid : v._bid_list) {
                            list.addComponent(new Label(bid._bid_owner));
                            list.addComponent(new Label(String.format("%.2f", bid._bid_value)));
                        }
                        bids_panel.addComponent(list);
                        
                        return;
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                // Error Message
                MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "You cannot place a bid");
            });
            body.addComponent(place_a_bid_panel);
        }
    }

    private void createSearchPage(List<Pair<String, String>> list, int offset, AuctionMechanismImpl auction_mechanism, char index) {
        body.removeAllComponents();
        body.setLayoutManager(new GridLayout(3));
        body.addComponent(createTitleLabel("Auction name"));
        body.addComponent(createTitleLabel("Author"));
        body.addComponent(new Label(" "));

        while(list.size() < offset + MainPage.PAGE_SIZE && index != 'z') {
            index = (char) (index + 1); 
            list.addAll(auction_mechanism.getListOfAuctions(index));
        }
        final char idx = index;

        for (int i = offset; i < list.size() && i < offset + MainPage.PAGE_SIZE; i++) {
            Pair<String, String> item = list.get(i);
            body.addComponent(new Label(item.element0()));
            body.addComponent(new Label(item.element1()));
            Button button = new Button("Check");
            body.addComponent(button);
            button.addListener((Button b) -> {
                checkListener(item.element0(), auction_mechanism);
            });
        }
        
        if(offset > 0) {
            Button prev_button = new Button("Prev");
            body.addComponent(prev_button);
        
            prev_button.addListener((Button b) -> {
                createSearchPage(list, offset - MainPage.PAGE_SIZE, auction_mechanism, idx);
            });
        } else {
            body.addComponent(new Label(" "));
        }

        body.addComponent(new Label(" "));

        if(index == 'z' && offset + MainPage.PAGE_SIZE > list.size()) {
            body.addComponent(new Label(" "));    
        } else {
            Button next_button = new Button("Next");
            body.addComponent(next_button);
        
            next_button.addListener((Button b) -> {
                createSearchPage(list, offset + MainPage.PAGE_SIZE, auction_mechanism, idx);
            });
        }       
    }

    private Label createTitleLabel(String text) {
        return (new Label(text))
            .setForegroundColor(TextColor.ANSI.BLACK_BRIGHT)
            .addStyle(SGR.BOLD);
    }
}