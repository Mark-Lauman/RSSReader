package ca.marklauman.tools;

public abstract class Tools {
	
	public static String arrToStr(long[] arr) {
		if(arr == null) return "" + null;
		if(arr.length == 0) return "[]";
		String res = "";
		for(long val : arr) {
			res += ", " + val;
		}
		return "[" + res.substring(2) + "]";
	}
	
	public static String arrToStr(String[] arr) {
		if(arr == null) return "" + null;
		if(arr.length == 0) return "[]";
		String res = "";
		for(String s : arr) {
			res += "\", \"" + s;
		}
		return "[" + res.substring(3) + "\"]";
	}
	
	
	public static <T> String arrToStr(T[] arr) {
		if(arr == null) return "" + null;
		if(arr.length == 0) return "[]";
		String res = "";
		for(T o : arr) res += ", " + o;
		return "[" + res.substring(2) + "]";
	}
}
