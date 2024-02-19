package com.cdd.datawarrior;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Communicator {
	public static InputStreamReader getInputStreamReader(String url, String token, Properties properties, boolean isPost, boolean isJSON, boolean isZip) throws IOException, URISyntaxException {
		if (!isPost && properties!=null)
			for (String key: properties.stringPropertyNames())
				url = url.concat(url.contains("?") ? "&" : "?").concat(key).concat("=").concat(properties.getProperty(key));

		URL myURL = new URI(url).toURL();

		// curl -H X-CDD-Token:$TOKEN 'https://app.collaborativedrug.com/api/v1/vaults
		HttpURLConnection connection = (HttpURLConnection)myURL.openConnection();
		connection.setRequestMethod(isPost ? "POST" : "GET");
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");
		connection.setRequestProperty("X-CDD-Token", token);
		connection.setRequestProperty("Content-Type", isJSON ? "application/json" : "text/plain");
		if (isPost && properties != null)
			for (String key: properties.stringPropertyNames())
				connection.setRequestProperty(key, properties.getProperty(key));
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);

		InputStream is = connection.getInputStream();
		if (isZip) {
			// For saving zip file call writeToTestFile(is) insrtead of the following!!!
			is = new ZipInputStream(is);
			ZipEntry entry = ((ZipInputStream)is).getNextEntry();
			while (entry != null && !entry.getName().endsWith(".sdf"))
				entry = ((ZipInputStream)is).getNextEntry();
		}

		return new InputStreamReader(is);
	}

/*	private static void writeToTestFile(InputStream is) throws IOException {
		OutputStream os = new FileOutputStream(new File("/home/thomas/temp.zip"));
		byte[] buffer = new byte[8 * 1024];
		int bytesRead;
		while ((bytesRead = is.read(buffer)) != -1) {
			os.write(buffer, 0, bytesRead);
		}
		IOUtils.closeQuietly(is);
		IOUtils.closeQuietly(os);
	}   */

	public static JSONArray retrieveArray(String url, String token) {
		try {
			return new JSONArray(new JSONTokener(getInputStreamReader(url, token, null, false, true, false)));
		}
		catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static JSONObject retrieveObject(String url, String token, Properties properties, boolean isPost) {
		try {
			return new JSONObject(new JSONTokener(getInputStreamReader(url, token, properties, isPost, true, false)));
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

}
