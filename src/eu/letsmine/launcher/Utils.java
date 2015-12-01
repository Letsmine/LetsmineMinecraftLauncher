package eu.letsmine.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.letsmine.launcher.DataTypes.VType;
import eu.letsmine.launcher.DataTypes.Version;

public class Utils {

	public static String getOS(){
		String OS = System.getProperty("os.name");
		if(OS.toLowerCase().contains("win")) return "windows";
		if(OS.toLowerCase().contains("mac")) return "osx";
		if(OS.toLowerCase().contains("nix") || OS.toLowerCase().contains("nux") || OS.toLowerCase().contains("aix")) return "linux";
		return null;
	}

	public static String getArch(){
		return System.getProperty("sun.arch.data.model");
	}

	public static Map<VType, List<Version>> createMapFromVersions(JSONArray varray){
		Map<VType, List<Version>> result = new HashMap<VType, List<Version>>();
		for (int i = 0; i < varray.length(); i++) {
			JSONObject v = varray.getJSONObject(i);
			VType vt = VType.valueOf(v.getString("type").toUpperCase());
			Version nv = new Version(v.getString("id"), v.getString("time"), vt);
			List<Version> vl = result.get(vt);
			if (vl == null) vl = new ArrayList<Version>();
			vl.add(nv);
			result.put(vt, vl);
		}
		return Collections.unmodifiableMap(result);
	}

	public static void StartMC(String StartString, File gameDir, Version version, String Name){
		try{
			//Start MC
			Process Minecraft = Runtime.getRuntime().exec(StartString);
			//wait 2s and check if MC has been started successfully
			Thread.sleep(2000);
			if(Minecraft.isAlive()){
				saveStartString(gameDir, version, StartString);
				saveLastUse(gameDir, version, Name);
				System.exit(0);
			}
			//otherwise show Termination Error
			JOptionPane.showMessageDialog(new JFrame(), "Hi, Leider ist folgender Fehler beim Starten von Minecraft aufgetreten:\n\nFehler  " + Minecraft.exitValue(), "Fehlermeldung", JOptionPane.ERROR_MESSAGE);
		}
		catch(Exception e){
			e.printStackTrace();
		}		
	}

	private static void saveLastUse(File gameDir, Version version, String name) {
		//save Last Use to regain Profile on next Startup
		try {
			PrintWriter pwriter = new PrintWriter(gameDir.getAbsolutePath() + "/last.use");
			pwriter.println(name + "\t" + version.getType() + "\t" + version.getId());
			pwriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void saveStartString(File gameDir, Version version, String StartString){
		try{
			//Write StartString to File for offline Starts
			PrintWriter pwriter = new PrintWriter(gameDir.getAbsolutePath() + "/versions/" + version.getId() + "/start.str");
			pwriter.println(StartString);
			pwriter.close();
		}
		catch(Exception e){
			e.printStackTrace();
		} 
	}
}


