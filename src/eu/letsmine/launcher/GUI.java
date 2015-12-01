package eu.letsmine.launcher;

import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.basic.BasicProgressBarUI;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import eu.letsmine.launcher.DataTypes.VType;
import eu.letsmine.launcher.DataTypes.Version;

public class GUI extends JFrame implements ActionListener, ComponentListener {

	private static final long serialVersionUID = 1L;

	private JLabel NameLabel = new JLabel("Name:");
	private JLabel VersionLabel = new JLabel("Version:");
	private JTextField Name = new JTextField("Letsmine");
	private Map<VType, List<Version>> McVersions;
	private JComboBox<VType> McType = new JComboBox<>(VType.values());
	private JComboBox<String> McVersion = new JComboBox<>();
	private JButton Play = new JButton("Spielen");
	private JTabbedPane InfoTabs = new JTabbedPane();
	private JEditorPane LMBlog = new JEditorPane();
	private JScrollPane LMBlogScroll = new JScrollPane(LMBlog);
	private JEditorPane MCBlog = new JEditorPane();
	private JScrollPane MCBlogScroll = new JScrollPane(MCBlog);
	private JProgressBar Progress = new JProgressBar();
	private File gameDir;
	private boolean debug = true;
	private boolean startoffline = false;
	private int FontSize = 16;
	private int ySize = 30;
	private int NameLabelWidth = 55;
	private int NameLabelSpace = 60;
	private int NameWidth = 200;
	private int VersionLabelWidth = 70;
	private int VersionLabelSpace = 75;
	private int McTypeWidth = 130;
	private int McTypeSpace = 135 + VersionLabelSpace;
	private int McVersionWidth = 160;
	private int PlayWidth = 100;

	public static void main(String[] args) {	
		new GUI();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
			}
		});
	}

	private GUI(){
		//Default Fenster + Rahmen erstellen
		this.setTitle("Let's Mine Launcher");
		try {
			Image logo = ImageIO.read(this.getClass().getResourceAsStream("logo.png"));
			this.setIconImage(logo);
		} catch (IOException e1) {	}
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(900, 600);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (d.width - getSize().width) / 2;
		int y = (d.height - getSize().height) / 2;
		this.setLocation(x, y);
		this.setMinimumSize(getSize());
		this.addComponentListener(this);
		Container Inhalt = getContentPane();
		Inhalt.setLayout(null);
		Inhalt.setBackground(Color.DARK_GRAY);

		if(debug) gameDir = new File("C:\\test");
		else gameDir = new File(System.getenv("APPDATA"), ".minecraft");

		this.McVersions = Downloader.getVersions();
		CheckMcVersions();

		//Komponenten einfügen

		Font standard = new Font("Helvetia", Font.BOLD, FontSize);

		this.NameLabel.setFont(standard);
		this.NameLabel.setForeground(Color.WHITE);
		Inhalt.add(this.NameLabel);

		this.Name.setFont(standard);
		//Block invalid Keys with event (copied and customized Code)
		Name.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (!(((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) || ((c >= '0') && (c <= '9')) || (c == KeyEvent.VK_BACK_SPACE) || (c == '_')) ||(Name.getText().length() >= 16)) {
					e.consume();  // ignore event
				}
			}
		});
		Inhalt.add(this.Name);

		this.VersionLabel.setFont(standard);
		this.VersionLabel.setForeground(Color.WHITE);
		Inhalt.add(this.VersionLabel);

		this.McType.setFont(standard);
		this.McType.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) {
				UpdateEntries();
			}
		});
		this.McType.setSelectedIndex(0);
		Inhalt.add(this.McType);

		this.McVersion.setFont(standard);
		this.add(this.McVersion);

		this.Play.setFont(standard);
		this.Play.addActionListener(this);
		this.Play.setActionCommand("spielen");
		this.add(this.Play);

		this.Progress.setBackground(Color.LIGHT_GRAY);
		this.Progress.setForeground(Color.BLUE);
		this.Progress.setUI(new BasicProgressBarUI() {protected Color getSelectionBackground() { return Color.black; } protected Color getSelectionForeground() { return Color.white; }});
		this.Progress.setStringPainted(true);
		this.Progress.setString("");
		this.add(this.Progress);

		AddHyperlinkListeners();

		MCBlog.setEditable(false);
		LMBlog.setEditable(false);
		InfoTabs.addTab("Minecraft News", MCBlogScroll);
		InfoTabs.addTab("Letsmine News", LMBlogScroll);
		Inhalt.add(InfoTabs);
		
		tryGetLastUse();

		//Anzeigen lassen
		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try{
			if(e.getActionCommand() == "spielen"){
				if(Name.getText().equalsIgnoreCase("")){
					JOptionPane.showMessageDialog(new JFrame(), "Bitte gib einen gültigen Namen ein!", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				if(startoffline){
					Version version = this.McVersions.get(McType.getSelectedItem()).get(McVersion.getSelectedIndex());
					try{
						InputStream source = new FileInputStream(gameDir.getAbsolutePath() + "/versions/" + version + "/start.str");
						String StartString = IOUtils.toString(source, "UTF-8");
						StartString = StartString.replaceAll("--username [A-z.0-9._]*", "--username " + Name.getText());
						StartString = StartString.replaceAll("--uuid [A-z.0-9.-]*", "--uuid " + UUID.nameUUIDFromBytes( ( "OfflinePlayer:" + Name.getText() ).getBytes( Charsets.UTF_8 ) ).toString());
						Utils.StartMC(StartString, gameDir, version, Name.getText());
					}
					catch(Exception x){
						JOptionPane.showMessageDialog(new JFrame(), "Fehler: Die Datei 'start.str' konnte nicht geladen werden!\n\nBitte starte den Launcher mindestens einmal mit\n\tbestehender Internetverbindung damit die nötigen Daten\n\theruntergeladen werden können.", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
				else{
					this.Play.setEnabled(false);
					Map<VType, List<Version>> VersionList = Downloader.getVersions();
					Version version = VersionList.get(McType.getSelectedItem()).get(McVersion.getSelectedIndex());
					Launcher launcher = new Launcher(version, gameDir, Name.getText(), this.Progress, this.Play);
					launcher.execute();
				}
			}
		}
		catch(Exception except){
			except.printStackTrace();
		}
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}
	@Override
	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}
	@Override
	public void componentShown(ComponentEvent e) {

	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		//Dynamic Gui
		int yOffset = getContentPane().getHeight() - 45;
		this.NameLabel.setBounds(20, yOffset, NameLabelWidth, ySize);
		this.Name.setBounds(20 + NameLabelSpace, yOffset, NameWidth, ySize);
		int xOffset = getContentPane().getWidth() / 2 - 120;
		this.VersionLabel.setBounds(xOffset, yOffset, VersionLabelWidth, ySize);
		this.McType.setBounds(xOffset + VersionLabelSpace, yOffset, McTypeWidth, ySize);
		this.McVersion.setBounds(xOffset + McTypeSpace, yOffset, McVersionWidth, ySize);
		xOffset = getContentPane().getWidth() - 120;
		this.Play.setBounds(xOffset, yOffset, PlayWidth, ySize);
		this.InfoTabs.setBounds(5, 5, xOffset + 110, yOffset - 35);
		this.Progress.setBounds(0, yOffset - 30, getContentPane().getWidth(), 15);
	}

	private void UpdateEntries(){
		//Populate Version JComboBox with correct Version-List on Typechange (e.g. Release -> Old-Beta)
		this.McVersion.removeAllItems();
		for(Version v : McVersions.get(McType.getSelectedItem())){
			this.McVersion.addItem(v.getId().toString());
		}
	}

	private void AddHyperlinkListeners(){
		try{
			LMBlog.setPage("http://media.letsmine.eu"); 
			//handle Link clicks with event (copied Code)
			LMBlog.addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(HyperlinkEvent hle) {
					if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
						System.out.println(hle.getURL());
						Desktop desktop = Desktop.getDesktop();
						try {
							desktop.browse(hle.getURL().toURI());
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			});
		}
		catch(IOException e){
			e.printStackTrace();
			LMBlog.setContentType("text/html");
			LMBlog.setText("<html>Beim Laden des Blogs ist leider ein Fehler aufgetreten</html>");
		}

		try{
			MCBlog.setPage("http://mcupdate.tumblr.com/"); 
			//handle Link clicks with event (copied Code)
			MCBlog.addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(HyperlinkEvent hle) {
					if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
						System.out.println(hle.getURL());
						Desktop desktop = Desktop.getDesktop();
						try {
							desktop.browse(hle.getURL().toURI());
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			});
		}
		catch(IOException e){
			e.printStackTrace();
			MCBlog.setContentType("text/html");
			MCBlog.setText("<html>Beim Laden des Blogs ist leider ein Fehler aufgetreten</html>");
		}
	}
	
	private void CheckMcVersions(){
		if(this.McVersions == null){		
			try {
				URL vjson = new URL("file:///" + gameDir.getAbsolutePath() + "/versions.json");
				String sjson = IOUtils.toString(vjson.openStream());
				this.McVersions = Utils.createMapFromVersions(new JSONObject(sjson).optJSONArray("versions"));
				startoffline = true;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(new JFrame(), "Fehler: Die Datei 'versions.json' konnte nicht geladen werden!\n\nBitte starte den Launcher mindestens einmal mit\nbestehender Internetverbindung damit die nötigen Daten\n\theruntergeladen werden können.\n\n\tDer Launcher wird nun geschlossen...", "Error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
	private void tryGetLastUse(){
		try {
			URL uLastUse = new URL("file:///" + gameDir.getAbsolutePath() + "/last.use");
			String[] LastUse = IOUtils.toString(uLastUse.openStream()).split("\t");
			Name.setText(LastUse[0]);
			for(int i = 0; i < McType.getItemCount(); i++){
				if(McType.getItemAt(i).toString().equalsIgnoreCase(LastUse[1])) McType.setSelectedIndex(i);
			}
			for(int i = 0; i < McVersion.getItemCount(); i++){
				if(McVersion.getItemAt(i).equalsIgnoreCase(LastUse[2].trim())) McVersion.setSelectedIndex(i);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}





