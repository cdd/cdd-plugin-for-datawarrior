package com.cdd.datawarrior;

import com.actelion.research.chem.io.SDFileParser;
import info.clearthought.layout.TableLayout;
import org.json.JSONObject;
import org.openmolecules.datawarrior.plugin.IPluginHelper;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;

public class CDDTaskRetrieveSearchResults extends AbstractProjectTask {
	private final String CONFIGURATION_SEARCH_NAME = "searchName";
	private final String CONFIGURATION_SEARCH_ID = "searchID";
	private final String SEARCHES_URL = "https://app.collaborativedrug.com/api/v1/vaults/VAULT/searches";
	private final String EXPORT_URL = "https://app.collaborativedrug.com/api/v1/vaults/VAULT/exports?search=SEARCH";
	private static final boolean RETRIEVE_SDF = true;
	private static final boolean RETRIEVE_ZIP = true;

	private JComboBox<String> mComboBoxSearch;
	private String[] mSearchID;

	public CDDTaskRetrieveSearchResults() {
		super(true);
	}

	@Override
	public String getTaskCode() {
		return "CDDVaultTask001";
	}

	@Override
	public String getTaskName() {
		return "Retrieve CDD Vault® Search Result";
	}

	private UpdateWorker createUpdateSearchesWorker(String vaultID, Properties configuration) {
		String url = SEARCHES_URL.replace("VAULT", vaultID);
		return new UpdateWorker(this, url, "Updating searches...", table -> {
			mSearchID = table[DEFAULT_ID_INDEX];
			mComboBoxSearch.removeAllItems();
			for (String searchName:table[DEFAULT_NAME_INDEX])
				mComboBoxSearch.addItem(searchName);
			int index = 0;
			if (configuration != null) {
				String searchID = configuration.getProperty(CONFIGURATION_SEARCH_ID);
				for (int i=0; i<mSearchID.length; i++) {
					if (mSearchID[i].equals(searchID)) {
						index = i;
						break;
					}
				}
			}
			if (index < mComboBoxSearch.getItemCount())
				mComboBoxSearch.setSelectedIndex(index);

			nextWorker();
		} );
	}

	@Override
	public JComponent createInnerDialogContent(int gap, IUserInterfaceHelper dialogHelper) {
		final double[][] size = { { TableLayout.FILL, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL },
				{ TableLayout.FILL, TableLayout.PREFERRED } };

		JPanel innerContent = new JPanel();
		innerContent.setLayout(new TableLayout(size));

		mComboBoxSearch = new JComboBox<>();
		innerContent.add(new JLabel("Query Name:"), "1,1");
		innerContent.add(mComboBoxSearch, "3,1");

		return innerContent;
	}

	@Override
	public void addUpdateWorkers(ArrayList<UpdateWorker> workerList, String vaultID, Properties configuration) {
		workerList.add(createUpdateSearchesWorker(vaultID, configuration));
	}

	/**
	 * This method is called after the users presses the dialog's 'OK' button.
	 * At this time the dialog is still shown. This method expects a Properties
	 * object containing all UI-elements' states converted into key-value pairs
	 * describing the user defined database query. This query configuration is
	 * used later for two purposes:<br>
	 * - to run the query independent of the actual dialog<br>
	 * - to populate a dialog with a query that has been performed earlier<br>
	 * @return query configuration
	 */
	@Override public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		if (mComboBoxSearch.getSelectedItem() != null) {
			configuration.setProperty(CONFIGURATION_SEARCH_ID, mSearchID[mComboBoxSearch.getSelectedIndex()]);
			configuration.setProperty(CONFIGURATION_SEARCH_NAME, (String)mComboBoxSearch.getSelectedItem());
		}

		return configuration;
	}

	@Override public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		// mComboBoxSearch selection is not set here, because the selected index can only be updated
		// once the cascaded retrieval of options has finished:
	}

	/**
	 * Checks, whether the given database query configuration is a valid one.
	 * If not, this method should return a short and clear error message
	 * intended for the user in order to correct the dialog setting.
	 * @param configuration
	 * @return user-interpretable error message or null, if query configuration is valid
	 */
	@Override public String checkConfiguration(Properties configuration) {
		if (configuration.getProperty(CONFIGURATION_SEARCH_ID, "").isEmpty()) {
			return "No search specified.";
		}
		return super.checkConfiguration(configuration);
	}

	/**
	 * This method performes the database query. Typically, it reads the query configuration from
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
		String vault = configuration.getProperty(CONFIGURATION_VAULT_ID, "");
		String searchID = configuration.getProperty(CONFIGURATION_SEARCH_ID, "");
		String projects = configuration.getProperty(CONFIGURATION_PROJECTS, "");
		String datasets = configuration.getProperty(CONFIGURATION_DATASETS, "");
		String searchName = configuration.getProperty(CONFIGURATION_SEARCH_NAME, "CDD Vault® Search");

		String exportURL = EXPORT_URL.replace("VAULT", vault)
				.replace("SEARCH", searchID);

		if (projects != null && !projects.isEmpty()) {
			exportURL = exportURL.concat("&projects=").concat(projects);
		}

		if (datasets != null && !datasets.isEmpty()) {
			exportURL = exportURL.concat("&data_sets=").concat(datasets);
		}

		exportURL = exportURL.concat(RETRIEVE_SDF ? "&format=sdf" : "&format=csv");

		if (RETRIEVE_ZIP)
			exportURL = exportURL.concat("&zip=true");

		String token = Token.get();
		if (token.isEmpty()) {
			dwInterface.showErrorMessage("No CDD API Key found.");
			return;
		}

		JSONObject so = Communicator.retrieveObject(exportURL, token, null, true);
		if (so == null) {
			dwInterface.showErrorMessage("Request to initialize export didn't answer.");
			return;
		}

		String progressURL = PROGRESS_URL.replace("VAULT", vault)
				.replace("EXPORT_ID", so.get("id").toString());

		while (!"finished".equals(so.get("status"))) {
			if (!"new".equals(so.get("status")) && !"started".equals(so.get("status"))) {
				dwInterface.showErrorMessage("Unexpected CDD export status: "+so.get("status"));
				return;
			}

			so = Communicator.retrieveObject(progressURL, token, null, false);
			if (so == null) {
				dwInterface.showErrorMessage("Request for export status wasn't answered.");
				return;
			}

			try { Thread.sleep(1000); } catch (InterruptedException ie) {}
		}

		String resultURL = RESULT_URL.replace("VAULT", vault)
				.replace("EXPORT_ID", so.get("id").toString());

		if (RETRIEVE_SDF)
			retrieveSDF(resultURL, searchName, dwInterface);
		else
			retrieveCSV(resultURL, searchName, dwInterface);
	}

	private void retrieveSDF(String resultURL, String searchName, IPluginHelper dwInterface) {
		String token = Token.get();
		TreeMap<String,Integer> titleToColumnMap = new TreeMap<>(); // during SDF-parsing collects growing list of title->columnIndex associations
		ArrayList<String[]> rowList = new ArrayList<>();    // idcode,coords,data1,data2,...

		try {
			BufferedReader reader = new BufferedReader(Communicator.getInputStreamReader(resultURL, token, null, false, false, RETRIEVE_ZIP));
			SDFileParser sdParser = new SDFileParser(reader);
			while (sdParser.next()) {
				String idcode = sdParser.getIDCode();
				String coords = sdParser.getCoordinates();
				ArrayList<String[]> titleAndDataList = parseSDFData(sdParser.getNextFieldData());
				int maxIndex = 0;
				for (String[] titleAndData:titleAndDataList) {
					Integer columnIndex = titleToColumnMap.get(titleAndData[0]);
					if (columnIndex == null) {
						columnIndex = 1+titleToColumnMap.size();
						titleToColumnMap.put(titleAndData[0], columnIndex);
					}
					maxIndex = Math.max(maxIndex, columnIndex);
				}

				String[] row = new String[maxIndex+1];
				row[0] = (idcode == null || idcode.isEmpty()) ? null
						: (coords == null || coords.isEmpty()) ? idcode : idcode.concat(" ").concat(coords);
				for (String[] titleAndData:titleAndDataList)
					row[titleToColumnMap.get(titleAndData[0])] = titleAndData[1];

				rowList.add(row);
			}

			reader.close();
		}
		catch (IOException | URISyntaxException ioe) {
			dwInterface.showErrorMessage("Exception during result retrieval: "+ioe.getMessage());
			return;
		}

		if (rowList.isEmpty()) {
			dwInterface.showErrorMessage("Your CDD search doesn't contains any result rows.");
			return;
		}

		int columnCount = 1 + titleToColumnMap.size();
		dwInterface.initializeData(columnCount, rowList.size(), searchName);

		dwInterface.setColumnType(0, IPluginHelper.COLUMN_TYPE_STRUCTURE_FROM_IDCODE);
		dwInterface.setColumnTitle(0, "Structure");

		for (String title:titleToColumnMap.keySet())
			dwInterface.setColumnTitle(titleToColumnMap.get(title), title);

		int rowIndex = 0;
		for (String[] row:rowList) {
			for (int column=0; column<row.length; column++)
				if (row[column] != null && !row[column].isEmpty())
					dwInterface.setCellData(column, rowIndex, row[column]);
			rowIndex++;
		}

		dwInterface.finalizeData(null);
	}

	private void retrieveCSV(String resultURL, String searchName, IPluginHelper dwInterface) {
		String token = Token.get();
		String[] title = null;
		ArrayList<String> rowList = new ArrayList<String>();

		try {
			BufferedReader reader = new BufferedReader(Communicator.getInputStreamReader(resultURL, token, null, false, false, RETRIEVE_ZIP));

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
		catch (IOException | URISyntaxException ioe) {
			dwInterface.showErrorMessage("Exception during result retrieval: "+ioe.getMessage());
			return;
		}

		if (rowList.isEmpty()) {
			dwInterface.showErrorMessage("Your CDD search doesn't contains any result rows.");
			return;
		}

		int smilesColumn = -1;
		for (int i=0; i<title.length; i++) {
			if (title[i].equals("SMILES")) {  // CXSMILES is not parsed with current public DataWarrior because of unexpected white space
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
				if (column >= columnCount) {
					break;  // to avoid OutOfBoundsExceptions because of unexpected delimiters in cell data
				}

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

	private ArrayList<String[]> parseSDFData(String data) {
		ArrayList<String[]> dataList = new ArrayList<>();
		if (data != null && !data.isEmpty()) {
			BufferedReader reader = new BufferedReader(new StringReader(data));
			try {
				String line = reader.readLine();
				String fieldName = null;
				String fieldData = null;
				while (line != null) {
					if (line.isEmpty())
						fieldName = null;
					else if (fieldName == null)
						fieldName = extractSDFFieldName(line);
					else
						fieldData = (fieldData == null) ? line : fieldData.concat("\n".concat(line));

					line = reader.readLine();

					if ((line == null || line.isEmpty()) && fieldName != null && !fieldName.isEmpty() && fieldData != null && !fieldData.isEmpty()) {
						String[] titleAndData = new String[2];
						titleAndData[0] = fieldName;
						titleAndData[1] = fieldData;
						dataList.add(titleAndData);
						fieldName = null;
						fieldData = null;
					}
				}
			}
			catch (IOException ioe) {}
		}

		return dataList;
	}

	protected String extractSDFFieldName(String line) {
		if (line.isEmpty()
				|| line.charAt(0) != '>')
			return null;

		int index = 1;
		int openBracket = 0;
		int closeBracket = 0;
		while (index < line.length()) {
			if (line.charAt(index) == '<') {
				if (openBracket != 0)
					return null;
				openBracket = index;
			}
			else if (line.charAt(index) == '>') {
				if (closeBracket != 0)
					return null;
				closeBracket = index;
			}
			index++;
		}

		if (openBracket != 0 && openBracket < closeBracket)
			return line.substring(openBracket+1, closeBracket);

		// allow for MACCS-II field numbers, which have format DTn
		index = line.indexOf("DT", 1);
		if (index == -1)
			return null;

		int i = index+2;
		while (line.length()>i && Character.isDigit(line.charAt(i)))
			i++;

		return (i == index+2) ? null : line.substring(index, i);
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
				if (index2 == -1) {  // we are missing a closing double quote
					cellList.add(row.substring(index1+1));
					break;
				}
				// don't accept closing double quotes if they are not followed by a comma
				// or they are not at the end of the row string
				while (index2 < row.length()-1 && row.charAt(index2+1) != ',') {
					index2 = row.indexOf('"', index2 + 1);
					if (index2 == -1) {  // we are missing a closing double quote
						cellList.add(row.substring(index1+1));
						break;
					}
				}
				cellList.add(row.substring(index1+1, index2));
				index1 = index2 + 2;
			} else {
				int index2 = row.indexOf(',', index1);
				if (index2 == -1) {  // no more comma, i.e. last cell entry
					cellList.add(row.substring(index1));
					break;
				}
				cellList.add(row.substring(index1, index2));
				index1 = index2+1;
			}
		}
		return cellList.toArray(new String[0]);
	}
}
