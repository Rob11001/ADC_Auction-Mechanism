package it.unisa.studenti.bruno.auction.ui;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

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
import net.tomp2p.utils.Pair;

public class MainPage extends BasicWindow {
    public Panel body;
    public Panel notify_panel;

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
            body.setLayoutManager(new GridLayout(1));
            body.addComponent(new Label("Auction name"));
            for (String auction_name : auction_mechanism.my_auctions_list) {
                body.addComponent(new Label(auction_name));
            }

        }));

        _menu_user.add(new MenuItem("My bids", () -> {
            if(auction_mechanism.my_bidder_list.isEmpty()) {
                body.removeAllComponents();
                body.addComponent(new Label("You didn't place any bids"));
                
                return;
            }

            body.removeAllComponents();
            body.setLayoutManager(new GridLayout(2));
            body.addComponent(new Label("Auction name"));
            body.addComponent(new Label("Auction's author"));
            for (Pair<String, String> pair : auction_mechanism.my_bidder_list) {
                body.addComponent(new Label(pair.element0()));
                body.addComponent(new Label(pair.element1()));
            }

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
            TextBox num_products_box = new TextBox().setValidationPattern(Pattern.compile("[1-9][0-9]*"));
            body.addComponent(num_products_box);

            body.addComponent(new Label("Reserved price: "));
            TextBox reserved_price_box = new TextBox().setValidationPattern(Pattern.compile("[1-9][0-9]*"));
            body.addComponent(reserved_price_box);

            // TODO: Vedere di risolvere la larghezza delle box e la creazione mediante la data (validare la data)
            Panel end_time_panel = new Panel(new GridLayout(9));
            body.addComponent(new Label("End time (yyyy-mm-dd hh:mm): "));
            TextBox year_box = new TextBox()
                .setValidationPattern(Pattern.compile("[0-9]"));
            end_time_panel.addComponent(year_box);
            end_time_panel.addComponent(new Label("-"));

            TextBox mounth_box = new TextBox()
                .setValidationPattern(Pattern.compile("[0-9]"));
            end_time_panel.addComponent(mounth_box);
            end_time_panel.addComponent(new Label("-"));

            TextBox day_box = new TextBox()
                .setValidationPattern(Pattern.compile("[1-9][0-9]*"));
            end_time_panel.addComponent(day_box);
            end_time_panel.addComponent(new Label(" "));
            
            TextBox hour_box = new TextBox()
                .setValidationPattern(Pattern.compile("[0-9]"));
            end_time_panel.addComponent(hour_box);
            end_time_panel.addComponent(new Label(":"));

            TextBox min_box = new TextBox()
                .setValidationPattern(Pattern.compile("[0-9]"));
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
                String mounth = mounth_box.getText();
                String dd = day_box.getText();
                String hh = hour_box.getText();
                String mm = min_box.getText();
                
                if( year.length() != 4
                    || mounth.length() != 2  
                    || dd.length() != 2 
                    || hh.length() != 2 
                    || mm.length() != 2) {
                    // Error message
                    MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Invalid date format");
                    
                    return;
                }

                if(auction_name.length() > 0 && num_products.length() > 0 && reserved_price.length() > 0) {
                    try {
                        Date end_time = new Date(Integer.parseInt(year), Integer.parseInt(mounth) - 1, Integer.parseInt(dd), Integer.parseInt(hh), Integer.parseInt(mm)); 
                        
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
                    MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Auction name cannot be empty");                    
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

        }));

        _menu_auctions.add(new MenuItem("Place a bid", () -> {

        }));

        _menu_auctions.add(new MenuItem("All auctions", () -> {

        }));

        this.setMenuBar(menubar);

        // Body component
        body = new Panel();
        body.addComponent(new Label(String.format("Welcome %s", auction_mechanism.user._username)));
        notify_panel = new Panel(new GridLayout(1));
        notify_panel.addComponent(new Label("Updates: "));
        
        this.setComponent(new Panel(new GridLayout(2))
            .addComponent(body)
            .addComponent(notify_panel)
        );

    }
    
}
