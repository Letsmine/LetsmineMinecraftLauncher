package eu.letsmine.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.letsmine.launcher.DataTypes.Version;

public class Launcher extends SwingWorker<String, Integer> {

	private Version version;
	private File gameDir;
	private String Name;
	private int FileCount;
	private JProgressBar Progress;
	private JButton Play;
	private static int VersionPercent = 25;
	private boolean CopyServer = true;

	public Launcher(Version version, File gameDir, String Name, JProgressBar Progress, JButton Play){
		this.version = version;
		this.gameDir = gameDir;
		this.Name = Name;
		this.Progress = Progress;
		this.Play = Play;
	}

	protected String doInBackground() throws Exception{
		//Check if servers.dat exists
		if(new File(gameDir, "servers.dat").exists()) CopyServer = false;
		
		//download version.json
		this.UpdatePorgress(0, String.format("Download %s.json", version.getId()));
		JSONObject StartParams = Downloader.getStartJSON(this.version);
		
		//set Progressbar properties
		FileCount = (int) (StartParams.getJSONArray("libraries").length() * (1 + (VersionPercent / 100f))) + 4; //+ 4 is Load version.json, load version.jar, unpack natives and start minecraft.jar
		this.Progress.setMaximum(FileCount);
		this.UpdatePorgress(1, String.format("Download %s.jar", version.getId()));
		
		//download version.jar
		Downloader.downloadVersion(this.gameDir, this.version);
		
		//Get Libraries JSON and check them
		String LibsString = this.CheckLibraries(StartParams, this.gameDir);
		String StartString = this.getStartString(this.version.getId(), StartParams, LibsString, this.Name, this.gameDir.getAbsolutePath());
		
		//unpack native Libraries
		this.UpdatePorgress(FileCount - 1, "Entpacke Libraries...");
		NativeExtractor.unpack(gameDir, StartParams, LibsString);
		
		//Check necessary Files for MC
		this.UpdatePorgress(FileCount, "Starte Minecraft...");
		if(CopyServer) CopyServerDat(gameDir);
		
		//Start MC
		Utils.StartMC(StartString, gameDir, version, Name);
		
		//Reset Progress
		this.UpdatePorgress(0, "Ein Fehler ist aufgetreten, sorry...");
		return StartString;
	}

	protected void done(){
		this.Play.setEnabled(true);
	}

	private String CheckLibraries(JSONObject StartParams, File gameDir){
		try{
			StringBuilder LibString = new StringBuilder("\"");
			JSONArray Libraries = StartParams.getJSONArray("libraries");
			File Libs = new File(gameDir, "libraries");
			File Package;
			File Name;
			File Version;
			File LibJar;
			File LibSha;
			for(int i = 0; i < Libraries.length(); i++){
				String[] LibFileSys = Libraries.getJSONObject(i).get("name").toString().split(":");
				this.UpdatePorgress(FileCount * VersionPercent / 100f + i, String.format("Download %s.jar",LibFileSys[1] + "-" + LibFileSys[2]));
				LibFileSys[0] = LibFileSys[0].replace('.', '/');
				Package = new File(Libs, LibFileSys[0]);
				Name = new File(Package, LibFileSys[1]);
				Version = new File(Name, LibFileSys[2]);
				if(!Version.exists()) Version.mkdirs();				
				if(Libraries.getJSONObject(i).has("natives")){
					String natives =  Libraries.getJSONObject(i).getJSONObject("natives").getString(Utils.getOS());
					if(natives.contains("${arch}")) natives = natives.replace("${arch}", Utils.getArch());
					LibJar = new File(Version, LibFileSys[1] + "-" + LibFileSys[2] + "-" + natives + ".jar");
					LibSha = new File(Version, LibFileSys[1] + "-" + LibFileSys[2] + "-" + natives + ".jar.sha");
					Downloader.getLibrary(new URL("https://libraries.minecraft.net/" + LibFileSys[0] + "/" + LibFileSys[1] + "/" + LibFileSys[2] + "/" + LibFileSys[1] + "-" + LibFileSys[2] + "-" + natives + ".jar") , LibJar);
					Downloader.getLibrary(new URL("https://libraries.minecraft.net/" + LibFileSys[0] + "/" + LibFileSys[1] + "/" + LibFileSys[2] + "/" + LibFileSys[1] + "-" + LibFileSys[2] + "-" + natives + ".jar.sha1") , LibSha);
				}
				else{
					LibJar = new File(Version, LibFileSys[1] + "-" + LibFileSys[2] + ".jar");
					LibSha = new File(Version, LibFileSys[1] + "-" + LibFileSys[2] + ".jar.sha");
					Downloader.getLibrary(new URL("https://libraries.minecraft.net/" + LibFileSys[0] + "/" + LibFileSys[1] + "/" + LibFileSys[2] + "/" + LibFileSys[1] + "-" + LibFileSys[2] + ".jar") , LibJar);
					Downloader.getLibrary(new URL("https://libraries.minecraft.net/" + LibFileSys[0] + "/" + LibFileSys[1] + "/" + LibFileSys[2] + "/" + LibFileSys[1] + "-" + LibFileSys[2] + ".jar.sha1") , LibSha);

				}
				LibString.append(LibJar.getAbsolutePath() + ';');
				publish(FileCount * (VersionPercent / 100) + i);
			}
			LibString.append(gameDir.getAbsolutePath() + "/versions/" + version.getId() + "/" + version.getId() + ".jar\"");
			return LibString.substring(0, LibString.length() - 1);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getStartString(String version, JSONObject StartParams, String LibsString, String Username, String gameDir){
		String Djava = gameDir + "/versions/" + version + "/natives";
		String MainClass = StartParams.getString("mainClass");
		String mcArgs = StartParams.getString("minecraftArguments");
		mcArgs = mcArgs.replace("${auth_player_name}"	, Username);
		mcArgs = mcArgs.replace("${version_name}"		, version);
		mcArgs = mcArgs.replace("${game_directory}"		, gameDir);
		mcArgs = mcArgs.replace("${assets_root}"		, ".");
		mcArgs = mcArgs.replace("${assets_index_name}"	, ".");
		mcArgs = mcArgs.replace("${auth_uuid}"			, UUID.nameUUIDFromBytes( ( "OfflinePlayer:" + Username ).getBytes( Charsets.UTF_8 ) ).toString());
		mcArgs = mcArgs.replace("${auth_access_token}"	, ".");
		mcArgs = mcArgs.replace("${user_properties}"	, ".");
		mcArgs = mcArgs.replace("${user_type}"			, "demo");
		return ("Java -Xmx1G -Xmn128M -Djava.library.path=" + Djava + " -cp " + LibsString + " " + MainClass + " " + mcArgs).replace("\"", "");
	}

	private void UpdatePorgress(double Progress, String Info){
		this.Progress.setValue((int) Progress);
		this.Progress.setString(Info);
	}

	private static void CopyServerDat(File GameDir) throws IOException {
		File target = new File(GameDir, "servers.dat");
		InputStream source = GUI.class.getResourceAsStream("servers.dat");
		FileUtils.copyInputStreamToFile(source, target);
	}
}
