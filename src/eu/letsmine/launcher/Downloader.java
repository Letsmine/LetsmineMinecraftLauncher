package eu.letsmine.launcher;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.letsmine.launcher.DataTypes.VType;
import eu.letsmine.launcher.DataTypes.Version;

import org.apache.commons.io.IOUtils;

public class Downloader {
    
	public static boolean downloadVersion(File gameDir, Version version) {
		boolean result = false;
		try {
			File versions = new File(gameDir, "versions");
			File vd = new File(versions, version.getId());
			File jar = new File(vd, version.getId() + ".jar");
			File json = new File(vd, version.getId() + ".json");
			if (!versions.exists()) versions.mkdirs();
			if (!vd.exists()) vd.mkdirs();
			
			if (!jar.exists()) {
				FileUtils.copyURLToFile(new URL("http://s3.amazonaws.com/Minecraft.Download/versions/" + version.getId() + "/" + version.getId() + ".jar"), jar);
			}
			if (!json.exists())
				FileUtils.copyURLToFile(new URL("http://s3.amazonaws.com/Minecraft.Download/versions/" + version.getId() + "/" + version.getId() + ".json"), json);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static Map<VType, List<Version>> getVersions() {
		try {
			URL version = new URL("http://s3.amazonaws.com/Minecraft.Download/versions/versions.json");
			HttpURLConnection connection = (HttpURLConnection) version.openConnection();
			if(connection != null){
				if(connection.getResponseCode() == 200){
					String json = IOUtils.toString(version.openStream());
					JSONObject obj = new JSONObject(json);
					JSONArray array = obj.getJSONArray("versions");
					return Utils.createMapFromVersions(array);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static JSONObject getStartJSON(Version version){
		try {
			URL Params = new URL("http://s3.amazonaws.com/Minecraft.Download/versions/" + version.getId() + "/" + version.getId() + ".json");
			String json = IOUtils.toString(Params.openStream());
			return new JSONObject(json);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Boolean getLibrary(URL source, File dest){
		if(!dest.exists()){
			try {
				FileUtils.copyURLToFile(source, dest);
				return dest.exists();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}
