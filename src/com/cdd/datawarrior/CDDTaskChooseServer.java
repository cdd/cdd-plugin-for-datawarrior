package com.cdd.datawarrior;

import info.clearthought.layout.TableLayout;
import org.openmolecules.datawarrior.plugin.IPluginHelper;
import org.openmolecules.datawarrior.plugin.IPluginTask;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import javax.swing.*;
import java.util.Properties;
import java.util.prefs.Preferences;

public class CDDTaskChooseServer implements IPluginTask {
	protected static final String PREFERENCES_SERVER_CODE = "servercode";
 	private static final String PROPERTY_SERVER = "server";

	private static final String[] SERVER_URL = { "https://app.collaborativedrug.com/api/v1/vaults/", "https://eu.collaborativedrug.com/api/v1/vaults/" };
	private static final String[] SERVER_NAME = { "US-Server", "EU-Server" };
	private static final String[] SERVER_CODE = { "us01", "eu01" };
	private static final int DEFAULT_SERVER = 0;
	private static int sCurrentServer = -1;

	private JComboBox<String> mComboBoxServer;

	@Override
	public String getTaskName() {
		return "Choose CDD Vault® Server";
	}

	@Override public String getTaskCode() {
		return "CDDVaultTask003";
	}

	protected static String getServerURL() {
		return SERVER_URL[getCurrentServer()];
	}

	private static int getCurrentServer() {
		if (sCurrentServer == -1) {
			sCurrentServer = DEFAULT_SERVER;
			Preferences prefs = Preferences.userRoot().node(Token.PREFERENCES_ROOT);
			String serverCode = prefs.get(PREFERENCES_SERVER_CODE, SERVER_CODE[DEFAULT_SERVER]);
			for (int i=0; i<SERVER_CODE.length; i++) {
				if (SERVER_CODE[i].equals(serverCode)) {
					sCurrentServer = i;
					break;
				}
			}
		}
		return sCurrentServer;
	}

	/**
	 * This method expects a JPanel with all UI-elements for defining a database query.
	 * These may include elements to define a structure search and/or alphanumerical
	 * search criteria. 'Cancel' and 'OK' buttons are provided outside of this panel.
	 * @param dialogHelper gives access to a chemistry panel to let the user draw a chemical (sub-)structure
	 * @return
	 */
	@Override public JComponent createDialogContent(IUserInterfaceHelper dialogHelper) {
		float f = (dialogHelper == null) ? 1.5f : dialogHelper.getUIScaleFactor();
		int gap = (int)(f * 8);

		mComboBoxServer = new JComboBox<>(SERVER_NAME);
		mComboBoxServer.setSelectedIndex(getCurrentServer());

		final double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap }, {gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Choose CDD-Vault Server:"), "1,1");
		content.add(mComboBoxServer, "3,1");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_SERVER, SERVER_CODE[mComboBoxServer.getSelectedIndex()]);
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String serverCode = configuration.getProperty(PROPERTY_SERVER, SERVER_CODE[DEFAULT_SERVER]);
		for (int i=0; i<SERVER_CODE.length; i++) {
			if (SERVER_CODE[i].equals(serverCode)) {
				mComboBoxServer.setSelectedItem(SERVER_NAME[i]);
				break;
			}
		}
	}

	@Override
	public String checkConfiguration(Properties configuration) {
		return null;
	}

	@Override
	public void run(Properties configuration, IPluginHelper dwInterface) {
		String serverCode = configuration.getProperty(PROPERTY_SERVER);
		if (!serverCode.equals(SERVER_CODE[getCurrentServer()])) {
			for (int i=0; i<SERVER_CODE.length; i++) {
				if (serverCode.equals(SERVER_CODE[i])) {
					Preferences prefs = Preferences.userRoot().node(Token.PREFERENCES_ROOT);
					prefs.put(PREFERENCES_SERVER_CODE, serverCode);
					sCurrentServer = i;
				}
			}
		}
	}
}
