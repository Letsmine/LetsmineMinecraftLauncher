package eu.letsmine.launcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.json.JSONObject;

public class NativeExtractor {
	
	static String ExcludeDir = "META-INF/";

	public static void unpack(File gameDir, JSONObject StartParams, String LibsString) {
		try{
			File NativeDir = new File(new File(new File(gameDir, "versions"), StartParams.getString("id")), "natives");
			if(!NativeDir.exists()) NativeDir.mkdirs();
			String[] LibStrings = LibsString.substring(1, LibsString.length() - 1).split(";", 0);
			for(int i = 0; i < LibStrings.length - 1; i++){ //- 1 to exclude <minecraftversion>.jar

				ZipFile zip = new ZipFile(LibStrings[i]);
				Enumeration<? extends ZipEntry> zipEntries = zip.entries();

				while(zipEntries.hasMoreElements()){
					ZipEntry zipEntry = zipEntries.nextElement();
					
					if((!zipEntry.getName().startsWith(ExcludeDir)) && (!zipEntry.getName().contains("/"))){
						if(zipEntry.getName().endsWith(".dll") || zipEntry.getName().endsWith(".so") || zipEntry.getName().endsWith(".jnilib") || zipEntry.getName().endsWith(".dylib")){

							BufferedInputStream InStream = new BufferedInputStream(zip.getInputStream(zipEntry));
							byte[] buffer = new byte['?'];
							FileOutputStream OutStream = new FileOutputStream(new File(NativeDir, zipEntry.getName()));
							BufferedOutputStream BOutStream = new BufferedOutputStream(OutStream);

							int length;
							while ((length = InStream.read(buffer, 0, buffer.length)) != -1) {
								BOutStream.write(buffer, 0, length);
							}

							//close Streams
							tryClose(BOutStream);
							tryClose(OutStream);
							tryClose(InStream);
						}
					}
				}
				zip.close();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	private static void tryClose(Closeable toClose){
		try{
			toClose.close();
		}
		catch(Exception e){	} //ignore
	}
}
