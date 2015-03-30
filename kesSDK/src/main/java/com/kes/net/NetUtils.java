package com.kes.net;


import java.util.HashMap;
import java.util.Map;

import android.content.Context;

public class NetUtils {
	public static Context context;
	public static int appVersion;
	public static int apiVersion;
	public static String versionString = null;
	

	public static Map<String, String> buildParameters(String... params) {
		return buildParameters(new HashMap<String, String>(), params);
	}

	public static Map<String, String> buildParameters(Map<String, String> result, String... params) {
		for (int i = 0; i < params.length / 2; i++) {
			result.put(params[i * 2], params[i * 2 + 1]);
		}
		return result;
	}

	public static Map<String, String> defaultJsonHeader() {
		return NetUtils.buildParameters("Accept", versionString, "Content-type", "application/json");
	}
}
