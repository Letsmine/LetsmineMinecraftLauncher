package eu.letsmine.launcher;

public class DataTypes {
	
	public static enum VType {
		RELEASE,
		SNAPSHOT,
		OLD_BETA,
		OLD_ALPHA
	}
	
	public static class Version {
		private String id;
		private String time;
		private VType type;
		
		public Version(String id, String time, VType type) {
			this.id = id;
			this.time = time;
			this.type = type;
		}
		
		public String getId() {
			return id;
		}
		
		public String getTime() {
			return time;
		}
		
		public VType getType() {
			return type;
		}
	}
}
