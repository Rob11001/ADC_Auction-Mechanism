package it.unisa.studenti.bruno.auction;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import it.unisa.studenti.bruno.auction.ui.IndexPage;

public class App {
    @Option(name="-m", aliases="--masterip", usage="the master peer ip address", required=true)
	private static String master;

	@Option(name="-id", aliases="--identifierpeer", usage="the unique identifier for this peer", required=true)
	private static int id;

    public static void main(String[] args) {
        App app = new App();
        final CmdLineParser parser = new CmdLineParser(app); 
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory(); 
        MultiWindowTextGUI gui = null;

        try {
            parser.parseArgument(args);
            // Peer creation
            AuctionMechanismImpl auction_mechanism = new AuctionMechanismImpl(id, master, new MessageListenerImpl());
            // UI initialization
            Terminal terminal = defaultTerminalFactory.createTerminal();
            Screen screen = new TerminalScreen(terminal);
            screen.startScreen();
            // Index page creation
            IndexPage idx_page = new IndexPage(auction_mechanism);
            gui = new MultiWindowTextGUI(screen,
                    new DefaultWindowManager(),
                    new EmptySpace(TextColor.ANSI.BLUE));
            gui.addWindowAndWait(idx_page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
