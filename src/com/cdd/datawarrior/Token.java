package com.cdd.datawarrior;

import java.util.prefs.Preferences;

public class Token {
	protected static final String PREFERENCES_ROOT = "com.collaborativedrug";
	private static final String PREFERENCES_KEY_TOKEN = "token";

	private static String sToken;

	public static String get() {
		if (sToken == null) {
			final Preferences prefs = Preferences.userRoot().node(PREFERENCES_ROOT);
			sToken = prefs.get(PREFERENCES_KEY_TOKEN, null);
		}
		return sToken;
	}

	public static void set(String token, boolean persist) {
		sToken = token;
		if (persist) {
			final Preferences prefs = Preferences.userRoot().node(PREFERENCES_ROOT);
			if (token == null || token.isEmpty()) {
				prefs.remove(PREFERENCES_KEY_TOKEN);
			} else {
				prefs.put(PREFERENCES_KEY_TOKEN, token);
			}
		}
	}
}
