import info.clearthought.layout.TableLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openmolecules.datawarrior.plugin.IPluginHelper;
import org.openmolecules.datawarrior.plugin.IPluginTask;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.swing.*;

/**
 * PluginTask to retrieve data from the CDD-Vault.
 */
public class CDDVaultTask implements IPluginTask {
	private static final String PREFERENCES_ROOT = "com.collaborativedrug";
	private static final String PREFERENCES_KEY_TOKEN = "token";

	private final String VAULT_URL = "https://app.collaborativedrug.com/api/v1/vaults";
	private final String DATASETS_URL = "https://app.collaborativedrug.com/api/v1/vaults/VAULT/data_sets";
	private final String PROJECTS_URL = "https://app.collaborativedrug.com/api/v1/vaults/VAULT/projects";
	private final String SEARCHES_URL = "https://app.collaborativedrug.com/api/v1/vaults/VAULT/searches";
	private final String EXPORT_URL = "https://app.collaborativedrug.com/api/v1/vaults/VAULT/exports?format=csv&search=SEARCH&projects=PROJECTS";
	private final String PROGRESS_URL = "https://app.collaborativedrug.com/api/v1/vaults/VAULT/export_progress/EXPORT_ID";
	private final String RESULT_URL = "https://app.collaborativedrug.com/api/v1/vaults/VAULT/exports/EXPORT_ID";

	private static final String[] DEFAULT_KEYS = { "name", "id"};
	private static final int DEFAULT_NAME_INDEX = 0;
	private static final int DEFAULT_ID_INDEX = 1;

	private static final String EMPTY_TOKEN = "<no token>";

	private final String CONFIGURATION_SEARCH_NAME = "searchName";
	private final String CONFIGURATION_VAULT = "vault";
	private final String CONFIGURATION_SEARCH = "search";
	private final String CONFIGURATION_PROJECTS = "projects";
	private final String CONFIGURATION_DATASETS = "datasets";

	private static Properties sTestConfiguration;	// this is the dialog configuration cache for main()

	private JLabel mLabelToken;
	private JComboBox<String> mComboBoxVault,mComboBoxSearch;
	private JList<String> mListProjects,mListDatasets;
	private String[] mVaultID,mSearchID,mProjectID,mDatasetID;

	@Override public String getTaskCode() {
		return "CDDVaultTask001";
	}

	@Override public String getTaskName() {
		return "Retrieve CDD-Vault Search Result";
	}

	/**
	 * This method expects a JPanel with all UI-elements for defining a database query.
	 * These may include elements to define a structure search and/or alphanumerical
	 * search criteria. 'Cancel' and 'OK' buttons are provided outside of this panel.
	 * @param dialogHelper gives access to a chemistry panel to let the user draw a chemical (sub-)structure
	 * @return
	 */
	@Override public JComponent createDialogContent(IUserInterfaceHelper dialogHelper) {
		final Preferences prefs = Preferences.userRoot().node(PREFERENCES_ROOT);
		final String token = prefs.get(PREFERENCES_KEY_TOKEN, "");

		float f = (dialogHelper == null) ? 1.5f : dialogHelper.getUIScaleFactor();
		int gap = (int)(f * 8);
		int listHeight = (int)(f * 80);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, 2*gap,
						TableLayout.PREFERRED, listHeight, gap, TableLayout.PREFERRED, listHeight, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mLabelToken = new JLabel(token.length() == 0 ? EMPTY_TOKEN : "Token: "+token.substring(0, 12)+"...");
		JButton buttonSetToken = new JButton("Set Token");
		buttonSetToken.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				String t = JOptionPane.showInputDialog(null, "Please provide your CDD-access token.", "CDD-Token", JOptionPane.QUESTION_MESSAGE);
				if (t != null && t.length() != 0) {
					prefs.put(PREFERENCES_KEY_TOKEN, t);
					mLabelToken.setText("Token: " + token.substring(0, 12) + "...");
					tokenUpdated(t);
				}
			}
		});

		content.add(buttonSetToken, "1,1");
		content.add(mLabelToken, "3,1");

		mComboBoxVault = new JComboBox<String>();
		mComboBoxVault.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				vaultUpdated(mVaultID[mComboBoxVault.getSelectedIndex()], prefs.get(PREFERENCES_KEY_TOKEN, ""));
			}
		});
		content.add(new JLabel("CDD Vault:"), "1,3");
		content.add(mComboBoxVault, "3,3");

		mComboBoxSearch = new JComboBox<String>();
		content.add(new JLabel("Search:"), "1,5");
		content.add(mComboBoxSearch, "3,5");

		mListProjects = new JList<String>();
		mListProjects.setModel(new DefaultListModel<String>());
		JScrollPane scrollPane1 = new JScrollPane(mListProjects);
		content.add(new JLabel("My Projects:"), "1,7");
		content.add(scrollPane1, "1,8,3,8");

		mListDatasets = new JList<String>();
		mListDatasets.setModel(new DefaultListModel<String>());
		JScrollPane scrollPane2 = new JScrollPane(mListDatasets);
		content.add(new JLabel("Public DataSets:"), "1,10");
		content.add(scrollPane2, "1,11,3,11");

		if (token.length() != 0)
			tokenUpdated(token);

		return content;
	}

	private void tokenUpdated(String token) {
		String[][] vaults = createTable(DEFAULT_KEYS, retrieveArray(VAULT_URL, token));
		if (vaults == null) {
			JOptionPane.showMessageDialog(null, "Retrieving vaults didn't answer.");
			return;
		}
		mVaultID = vaults[DEFAULT_ID_INDEX];
		mComboBoxVault.removeAllItems();
		for (String vaultName:vaults[DEFAULT_NAME_INDEX])
			mComboBoxVault.addItem(vaultName);
		mComboBoxVault.setSelectedIndex(0);
		vaultUpdated(mVaultID[0], token);
	}

	private void vaultUpdated(String vaultID, String token) {
		String[][] searches = createTable(DEFAULT_KEYS, retrieveArray(SEARCHES_URL.replace("VAULT", vaultID), token));
		if (searches == null) {
			JOptionPane.showMessageDialog(null, "Retrieving searches didn't answer.");
			return;
		}
		mSearchID = searches[DEFAULT_ID_INDEX];
		mComboBoxSearch.removeAllItems();
		for (String searchName:searches[DEFAULT_NAME_INDEX])
			mComboBoxSearch.addItem(searchName);
		mComboBoxSearch.setSelectedIndex(0);

		String[][] projects = createTable(DEFAULT_KEYS, retrieveArray(PROJECTS_URL.replace("VAULT", vaultID), token));
		if (projects == null) {
			JOptionPane.showMessageDialog(null, "Retrieving projects didn't answer.");
			return;
		}
		mProjectID = projects[DEFAULT_ID_INDEX];
		((DefaultListModel)mListProjects.getModel()).clear();
		for (String projectName:projects[DEFAULT_NAME_INDEX])
			((DefaultListModel<String>)mListProjects.getModel()).addElement(projectName);
		mListProjects.getSelectionModel().addSelectionInterval(0, mProjectID.length-1);

		String[][] datasets = createTable(DEFAULT_KEYS, retrieveArray(DATASETS_URL.replace("VAULT", vaultID), token));
		if (datasets == null) {
			JOptionPane.showMessageDialog(null, "Retrieving datasets didn't answer.");
			return;
		}
		mDatasetID = datasets[DEFAULT_ID_INDEX];
		((DefaultListModel)mListDatasets.getModel()).clear();
		for (String datasetName:datasets[DEFAULT_NAME_INDEX])
			((DefaultListModel<String>)mListDatasets.getModel()).addElement(datasetName);
	}

	/**
	 * This method is called after the users presses the dialog's 'OK' button.
	 * At this time the dialog is still shown. This method expects a Properties
	 * object containing all UI-elements' states converted into key-value pairs
	 * describing the user defined database query. This query configuration is
	 * used later for two purposes:<br>
	 * - to run the query independent from the actual dialog<br>
	 * - to populate a dialog with a query that has been performed earlier<br>
	 * @return query configuration
	 */
	@Override public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		if (mComboBoxVault.getSelectedItem() != null)
			configuration.setProperty(CONFIGURATION_VAULT, mVaultID[mComboBoxVault.getSelectedIndex()]);

		if (mComboBoxSearch.getSelectedItem() != null)
			configuration.setProperty(CONFIGURATION_SEARCH, mSearchID[mComboBoxSearch.getSelectedIndex()]);

		if (mComboBoxSearch.getSelectedItem() != null)
			configuration.setProperty(CONFIGURATION_SEARCH_NAME, (String)mComboBoxSearch.getSelectedItem());

		String projects = getSelectedColumnsFromList(mListProjects, mProjectID);
		if (projects != null && projects.length() != 0)
			configuration.setProperty(CONFIGURATION_PROJECTS, projects);

		String datasets = getSelectedColumnsFromList(mListDatasets, mDatasetID);
		if (datasets != null && datasets.length() != 0)
			configuration.setProperty(CONFIGURATION_DATASETS, datasets);

		return configuration;
	}

	/**
	 * This method populates an empty database query dialog with a previously configured database query.
	 * @param configuration
	 */
	@Override public void setDialogConfiguration(Properties configuration) {
		String vault = configuration.getProperty(CONFIGURATION_VAULT, "");
		mComboBoxVault.setSelectedIndex(getListIndex(vault, mVaultID));

		String search = configuration.getProperty(CONFIGURATION_SEARCH, "");
		mComboBoxSearch.setSelectedItem(getListIndex(search, mSearchID));

		String projects = configuration.getProperty(CONFIGURATION_PROJECTS, "");
		selectColumnsInList(mListProjects, mProjectID, projects);

		String datasets = configuration.getProperty(CONFIGURATION_DATASETS, "");
		selectColumnsInList(mListDatasets, mDatasetID, datasets);
	}

	private int getListIndex(String entry, String[] list) {
		for (int i=0; i<list.length; i++)
			if (entry.equals(list[i]))
				return i;
		return 0;
	}

	/**
	 * Creates a comma delimited String containing ids of the names selected in the list.
	 * @param list
	 * @param keys
	 * @return null or comma delimited column name list
	 */
	public String getSelectedColumnsFromList(JList list, String[] keys) {
		StringBuilder sb = null;
		for (int index:list.getSelectedIndices()) {
			if (sb == null) {
				sb = new StringBuilder(keys[index]);
			}
			else {
				sb.append(',');
				sb.append(keys[index]);
			}
		}
		return (sb == null) ? null : sb.toString();
	}

	/**
	 * Selects all named items in a list whose ids are in given comma delimited string
	 * @param list
	 * @param keys array with keys matching the current list
	 * @param keyString comma delimited list of keys to select
	 */
	public void selectColumnsInList(JList list, String[] keys, String keyString) {
		list.clearSelection();
		if (keyString != null) {
			for (String key:keyString.split(",")) {
				for (int i=0; i<keys.length; i++) {
					if (key.equals(keys[i])) {
						list.addSelectionInterval(i, i);
						break;
					}
				}
			}
		}
	}

	/**
	 * Checks, whether the given database query configuration is a valid one.
	 * If not, the this method should return a short and clear error message
	 * intended for the user in order to correct the dialog setting.
	 * @param configuration
	 * @return user-interpretable error message or null, if query configuration is valid
	 */
	@Override public String checkConfiguration(Properties configuration) {
		Preferences prefs = Preferences.userRoot().node(PREFERENCES_ROOT);
		if (prefs.get(PREFERENCES_KEY_TOKEN, "").length() == 0)
			return "No CDD-access token given.";

		return null;
	}

	/**
	 * This method performes the database query. Typically it reads the query configuration from
	 * the given Properties object, sends it to a database server, retrieves a result and populates
	 * a new window's table with the retrieved data. The passed IPluginHelper object provides
	 * all needed methods to create a new DataWarrior window, to allocate result columns,
	 * to populate these columns with chemical and alphanumerical content, and to show an error
	 * message if something goes wrong.<br>
	 * The query definition must be taken from the passed configuration object and NOT from
	 * any UI-elements of the dialog. The concept is to completely separate query definition
	 * from query execution, which is the basis for DataWarrior's macros to work.
	 * If an error occurrs, then this method should call dwInterface.showErrorMessage().
	 * @param configuration
	 * @param dwInterface
	 */
	@Override public void run(Properties configuration, IPluginHelper dwInterface) {
		String vault = configuration.getProperty(CONFIGURATION_VAULT, "");
		String search = configuration.getProperty(CONFIGURATION_SEARCH, "");
		String projects = configuration.getProperty(CONFIGURATION_PROJECTS, "");
		String datasets = configuration.getProperty(CONFIGURATION_DATASETS, "");
		String searchName = configuration.getProperty(CONFIGURATION_SEARCH_NAME, "CDD-Vault Search");

		String exportURL = EXPORT_URL.replace("VAULT", vault)
									 .replace("SEARCH", search)
									 .replace("PROJECTS", projects);

		if (datasets != null && datasets.length() != 0)
			exportURL = exportURL.concat("&data_sets=").concat(datasets);

		String token = Preferences.userRoot().node(PREFERENCES_ROOT).get(PREFERENCES_KEY_TOKEN, "");
		if (token.length() == 0) {
			dwInterface.showErrorMessage("No CDD-access token found.");
			return;
		}

		JSONObject so = retrieveObject(exportURL, token, true);
		if (so == null) {
			dwInterface.showErrorMessage("Request to initialize export didn't answer.");
			return;
		}

		String progressURL = PROGRESS_URL.replace("VAULT", vault)
										 .replace("EXPORT_ID", so.get("id").toString());

		while (!"finished".equals(so.get("status"))) {
			if (!"new".equals(so.get("status"))
			 && !"started".equals(so.get("status"))) {
				dwInterface.showErrorMessage("Unexpected CDD export status: "+so.get("status"));
				return;
			}

			so = retrieveObject(progressURL, token, false);
			if (so == null) {
				dwInterface.showErrorMessage("Request for export status wasn't answered.");
				return;
			}

			try { Thread.sleep(1000); } catch (InterruptedException ie) {}
		}

		String resultURL = RESULT_URL.replace("VAULT", vault)
									 .replace("EXPORT_ID", so.get("id").toString());

		String[] title = null;
		ArrayList<String> rowList = new ArrayList<String>();

		try {
			BufferedReader reader = new BufferedReader(getInputStreamReader(resultURL, token, false, false));
			String line = reader.readLine();
			if (line != null) {
				title = splitCells(line);

				line = reader.readLine();
				while (line != null) {
					rowList.add(line);
					line = reader.readLine();
				}
			}
			reader.close();
		}
		catch (IOException ioe) {
			dwInterface.showErrorMessage("Exception during result retrieval: "+ioe.getMessage());
			return;
		}

		if (rowList.size() == 0) {
			dwInterface.showErrorMessage("Your CDD-search doesn't contains any result rows.");
			return;
		}

		int smilesColumn = -1;
		for (int i=0; i<title.length; i++) {
			if (title[i].equals("SMILES")) {	// CXSMILES is not parsed with current public DataWarrior because of unexpected white space
				smilesColumn = i;
				break;
			}
		}

		int columnCount = (smilesColumn == -1) ? title.length : title.length + 1;
		dwInterface.initializeData(columnCount, rowList.size(), searchName);

		int column = 0;
		for (String columnTitle:title) {
			if (column == smilesColumn) {
				dwInterface.setColumnType(column, IPluginHelper.COLUMN_TYPE_STRUCTURE_FROM_SMILES);
				dwInterface.setColumnTitle(column, "Structure");
				column++;
			}
			dwInterface.setColumnTitle(column, columnTitle);
			column++;
		}

		int row = 0;
		for (String rowString:rowList) {
			String[] entry = splitCells(rowString);

			column = 0;
			for (String cellEntry:entry) {
				if (column >= columnCount)
					break;	// to avoid OutOfBoundsExceptions because of unexpected delimiters in cell data

				if (column == smilesColumn) {
					dwInterface.setCellData(column, row, cellEntry);
					column++;
				}
				dwInterface.setCellData(column, row, cellEntry);
				column++;
			}
			row++;
		}

		dwInterface.finalizeData(null);
	}

	private String[] splitCells(String row) {
		ArrayList<String> cellList = new ArrayList<String>();
		int index1 = 0;
		while (index1 <= row.length()) {
			if (index1 == row.length()) {
				cellList.add("");
				break;
			}
			if (row.charAt(index1) == '"') {
				int index2 = row.indexOf('"', index1+1);
				if (index2 == -1) {	// we are missing a closing double quote
					cellList.add(row.substring(index1+1));
					break;
				}
				// don't accept closing double quotes if they are not followed by a comma
				// or they are not at the end of the row string
				while (index2 < row.length()-1 && row.charAt(index2+1) != ',') {
					index2 = row.indexOf('"', index2 + 1);
					if (index2 == -1) {	// we are missing a closing double quote
						cellList.add(row.substring(index1+1));
						break;
					}
				}
				cellList.add(row.substring(index1+1, index2));
				index1 = index2 + 2;
			} else {
				int index2 = row.indexOf(',', index1);
				if (index2 == -1) {	// no more comma, i.e. last cell entry
					cellList.add(row.substring(index1));
					break;
				}
				cellList.add(row.substring(index1, index2));
				index1 = index2+1;
			}
		}
		return cellList.toArray(new String[0]);
	}

	private JSONObject retrieveObject(String url, String token, boolean isPost) {
		try {
			return new JSONObject(new JSONTokener(getInputStreamReader(url, token, isPost, true)));
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private JSONArray retrieveArray(String url, String token) {
		try {
			return new JSONArray(new JSONTokener(getInputStreamReader(url, token, false, true)));
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private InputStreamReader getInputStreamReader(String url, String token, boolean isPost, boolean isJSON) throws IOException {
		URL myURL = new URL(url);

		// curl -H X-CDD-Token:$TOKEN 'https://app.collaborativedrug.com/api/v1/vaults

		HttpURLConnection connection = (HttpURLConnection) myURL.openConnection();
		//		String encodedToken = new String(new Base64().encode(token.getBytes()));
		connection.setRequestMethod(isPost ? "POST" : "GET");
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");
		connection.setRequestProperty("X-CDD-Token", token);
		connection.setRequestProperty("Content-Type", isJSON ? "application/json" : "text/plain");
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);

		return new InputStreamReader(connection.getInputStream());
	}

	private String[][] createTable(String[] keys, JSONArray jsonArray) {
		if (jsonArray == null)
			return null;

		String[][] table = new String[keys.length][jsonArray.length()];
		Iterator<Object> objects = jsonArray.iterator();
		int index2 = 0;
		while (objects.hasNext()) {
			JSONObject jsonObject = (JSONObject)objects.next();
			int index1 = 0;
			for (String key:keys)
				table[index1++][index2] = jsonObject.get(key).toString();
			index2++;
		}
		return table;
	}

	/**
	 * This can be used to test the dialog, but not to test the actual retrieval,
	 * because dwInterface is null and cannot populate a table.
	 * One would need to create a dummy class implementing IPluginHelper and to pass
	 * an instance of it to CDDVaultTask().run() to be able to test the run() method
	 * as well.
	 * @param args
	 */
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			final CDDVaultTask task = new CDDVaultTask();
			public void run() {
				ActionListener al = new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (e.getActionCommand().equals("OK")) {
							sTestConfiguration = task.getDialogConfiguration();
							new CDDVaultTask().run(sTestConfiguration, null);
						}
					}
				};

				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BorderLayout());
				JPanel ibp = new JPanel();
				ibp.setLayout(new GridLayout(1, 2, 8, 0));
				JButton bcancel = new JButton("Cancel");
				bcancel.addActionListener(al);
				ibp.add(bcancel);
				JButton bok = new JButton("OK");
				bok.addActionListener(al);
				ibp.add(bok);
				buttonPanel.add(ibp, BorderLayout.EAST);
				buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));

				JDialog dialog = new JDialog((Frame) null, "CDD Test", true);
				dialog.getContentPane().setLayout(new BorderLayout());
				dialog.getContentPane().add(task.createDialogContent(null), BorderLayout.CENTER);
				dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
				dialog.getRootPane().setDefaultButton(bok);

				if (sTestConfiguration != null)
					task.setDialogConfiguration(sTestConfiguration);

				dialog.pack();
				dialog.setVisible(true);
			}
		} );
	}
}
