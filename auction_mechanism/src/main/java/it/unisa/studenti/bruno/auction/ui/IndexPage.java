package it.unisa.studenti.bruno.auction.ui;

import java.util.regex.Pattern;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.bundle.LanternaThemes;
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
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;

import it.unisa.studenti.bruno.auction.AuctionMechanismImpl;

public class IndexPage extends BasicWindow {
    public TextBox _username_box;
    public TextBox _password_box;
    private Button _login_button, _register_button;
    private String _username_regex = "[A-Za-z][A-Za-z0-9_]{0,29}";
    private String _password_regex = "[A-Za-z][A-Za-z0-9_]{0,29}";

    public IndexPage(AuctionMechanismImpl auction_mechanism) {
        super();
    
        // Login panel creation
        Panel login_panel = new Panel(new GridLayout(1));
        
        // Username
        login_panel.addComponent(new Label("Username: "));
        _username_box = new TextBox()
            .setValidationPattern(Pattern.compile(_username_regex))
            .addTo(login_panel);
        
        // Password
        login_panel.addComponent(new Label("Password: "));
        _password_box = new TextBox()
            .setValidationPattern(Pattern.compile(_password_regex))
            .addTo(login_panel);
        
        // Buttons
        Panel button_panel = new Panel(new GridLayout(2));
        _login_button = new Button("Login").addTo(button_panel);
        _register_button = new Button("Register").addTo(button_panel);
        login_panel.addComponent(button_panel);
        
        // Login panel added to index page
        this.setComponent(login_panel);
        this.setTitle("Auction Mech");
        this.setTheme(LanternaThemes.getDefaultTheme());
            
        // Sets the listeners
        _login_button.addListener((button) -> {
            // Login listener
            String username = this._username_box.getText();
            String password = this._password_box.getText();
            
            try {
                if(username.length() > 0 && password.length() > 0 && auction_mechanism.login(username, password)) {
                    WindowBasedTextGUI wbtGUI = this.getTextGUI().removeWindow(this);
                    this.close();
                    MainPage main_page = new MainPage(auction_mechanism);
                    MultiWindowTextGUI gui = new MultiWindowTextGUI(wbtGUI.getScreen(),     // I need to create a new one otherwise the older page remains
                        new DefaultWindowManager(),
                        new EmptySpace(TextColor.ANSI.BLUE));
                    gui.addWindowAndWait(main_page);

                    return;   
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            // Error message
            MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Wrong username or password");
        });

        _register_button.addListener((button) -> {
            // Register listener
            String username = this._username_box.getText();
            String password = this._password_box.getText();
            
            try {
                if(username.length() > 0 && password.length() > 0 && auction_mechanism.register(username, password)) {
                    MessageDialog.showMessageDialog(this.getTextGUI(), "Success", "Your account has been successfully created");
                    this._username_box.setText("");
                    this._password_box.setText("");
                    
                    return;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            // Error Message
            MessageDialog.showMessageDialog(this.getTextGUI(), "Error", "Invalid username");
        });
    }
}
