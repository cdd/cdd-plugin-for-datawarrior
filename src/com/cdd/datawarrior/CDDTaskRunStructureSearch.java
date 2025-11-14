package com.cdd.datawarrior;

import info.clearthought.layout.TableLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openmolecules.datawarrior.plugin.IChemistryPanel;
import org.openmolecules.datawarrior.plugin.IPluginHelper;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TreeMap;

public class CDDTaskRunStructureSearch extends AbstractProjectTask {
	private static final String CONFIGURATION_SEARCH_TYPE = "searchType";
	private static final String CONFIGURATION_SIMILARITY = "similarity";
	private static final String[] SEARCH_TYPE_OPTIONS = { "Substructure", "Similarity" };
	private static final String[] SEARCH_TYPE_CODE = { "sss", "sim" };
	private static final int SEARCH_TYPE_SSS = 0;
	private static final int SEARCH_TYPE_SIM = 1;
	private static final String[] SUPPRESSED_COLUMNS = { "class", "registration_type" ,"cdd_registry_number" };

	public CDDTaskRunStructureSearch() {
		super(true);
	}

	private final String STRUCTURE_SEARCH_URL = "https://app.collaborativedrug.com/api/v1/vaults/VAULT/molecules";
	private final String CONFIGURATION_MOLFILE = "molfile";


	@Override public String getTaskCode() {
		return "CDDVaultTask002";
	}

	@Override public String getTaskName() {
		return "Run CDD Vault® Structure Search";
	}

	private IChemistryPanel mChemistryPanel;
	private JComboBox<String> mComboBoxSearchType;
	private JSlider mSimilaritySlider;


	@Override
	public JComponent createInnerDialogContent(int gap, IUserInterfaceHelper dialogHelper) {
		final double[][] size = { { TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED, gap, TableLayout.FILL },
								  { TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED } };

		JPanel innerContent = new JPanel();
		innerContent.setLayout(new TableLayout(size));

		mComboBoxSearchType = new JComboBox<>(SEARCH_TYPE_OPTIONS);
		mComboBoxSearchType.addActionListener(e -> searchModeUpdated() );
		mChemistryPanel = dialogHelper.getChemicalEditor();
		mChemistryPanel.setMode(IChemistryPanel.MODE_FRAGMENT);
		((JComponent)mChemistryPanel).setBorder(new TitledBorder("Structure"));
		innerContent.add(new JLabel("Searchtype:"), "0,0");
		innerContent.add(mComboBoxSearchType, "2,0");
		innerContent.add(createSimilaritySlider(dialogHelper), "0,2,2,2");
		innerContent.add((JComponent)mChemistryPanel, "4,0,4,2");

		return innerContent;
	}

	protected JComponent createSimilaritySlider(IUserInterfaceHelper dialogHelper) {
		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(70, new JLabel("70%"));
		labels.put(80, new JLabel("80%"));
		labels.put(90, new JLabel("90%"));
		labels.put(100, new JLabel("100%"));
		mSimilaritySlider = new JSlider(JSlider.VERTICAL, 70, 100, 90);
		mSimilaritySlider.setMinorTickSpacing(1);
		mSimilaritySlider.setMajorTickSpacing(10);
		mSimilaritySlider.setLabelTable(labels);
		mSimilaritySlider.setPaintLabels(true);
		mSimilaritySlider.setPaintTicks(true);
		int width = mSimilaritySlider.getPreferredSize().width;
		int height = Math.round(166f * dialogHelper.getUIScaleFactor());
		mSimilaritySlider.setMinimumSize(new Dimension(width, height));
		mSimilaritySlider.setPreferredSize(new Dimension(width, height));
		mSimilaritySlider.setEnabled(false);
		JPanel spanel = new JPanel();
		spanel.add(mSimilaritySlider);
		spanel.setBorder(BorderFactory.createTitledBorder("Similarity"));
		return spanel;
	}

	@Override
	public void addUpdateWorkers(ArrayList<UpdateWorker> workerList, String vaultID, Properties configuration) {}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		if (!mChemistryPanel.isEmptyMolecule()) {
			String molfile = mChemistryPanel.getMoleculeAsMolfileV3();
			configuration.setProperty(CONFIGURATION_MOLFILE, molfile);
			configuration.setProperty(CONFIGURATION_SEARCH_TYPE, SEARCH_TYPE_CODE[mComboBoxSearchType.getSelectedIndex()]);
			if (mSimilaritySlider.isEnabled())
				configuration.setProperty(CONFIGURATION_SIMILARITY, Float.toString(0.01f * mSimilaritySlider.getValue()));
		}

		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		String molfile = configuration.getProperty(CONFIGURATION_MOLFILE, "");
		if (molfile != null) {
			mComboBoxSearchType.setSelectedIndex(SEARCH_TYPE_CODE[SEARCH_TYPE_SSS].equals(configuration.getProperty(CONFIGURATION_SEARCH_TYPE)) ? SEARCH_TYPE_SSS : SEARCH_TYPE_SIM);
			mChemistryPanel.setMoleculeFromMolfile(molfile);
		}
		String value = configuration.getProperty(CONFIGURATION_SIMILARITY);
		if (value != null)
			mSimilaritySlider.setValue(Math.max(70, Math.min(100, Math.round(100f*Float.parseFloat(value)))));

		searchModeUpdated();
	}

	private void searchModeUpdated() {
		boolean isSSS = (mComboBoxSearchType.getSelectedIndex() == SEARCH_TYPE_SSS);
		mChemistryPanel.setMode(isSSS ?  IChemistryPanel.MODE_FRAGMENT : IChemistryPanel.MODE_MOLECULE);
		mSimilaritySlider.setEnabled(!isSSS);
	}

	@Override
	public String checkConfiguration(Properties configuration) {
		// molfile is allowed to be null
		return super.checkConfiguration(configuration);
	}

	@Override
	public void run(Properties configuration, IPluginHelper dwInterface) {
		String vault = configuration.getProperty(CONFIGURATION_VAULT_ID, "");
		String projects = configuration.getProperty(CONFIGURATION_PROJECTS, "");
		String datasets = configuration.getProperty(CONFIGURATION_DATASETS, "");
		String searchType = configuration.getProperty(CONFIGURATION_SEARCH_TYPE, "");
		String molfile = configuration.getProperty(CONFIGURATION_MOLFILE, "");

		String url = STRUCTURE_SEARCH_URL.replace("VAULT", vault);

		Properties properties = new Properties();

		properties.setProperty("async", "true");

		if (projects != null && !projects.isEmpty())
			properties.setProperty("projects", projects);

		if (datasets != null && !datasets.isEmpty())
			properties.setProperty("data_sets", datasets);

		if (!searchType.isEmpty() && !molfile.isEmpty()) {
			try {
				properties.setProperty("structure", URLEncoder.encode(molfile, "UTF-8"));
			} catch (UnsupportedEncodingException e) { return; }
		}

		if (SEARCH_TYPE_CODE[SEARCH_TYPE_SSS].equals(searchType)) {
			properties.setProperty("structure_search_type", "substructure");
		}
		else if (SEARCH_TYPE_CODE[SEARCH_TYPE_SIM].equals(searchType)) {
			properties.setProperty("structure_search_type", "similarity");
			properties.setProperty("structure_similarity_threshold", configuration.getProperty(CONFIGURATION_SIMILARITY, "0.8"));
		}

		String token = Token.get();
		if (token.isEmpty()) {
			dwInterface.showErrorMessage("No CDD API Key found.");
			return;
		}

		JSONObject so = Communicator.retrieveObject(url, token, properties, false);
		if (so == null) {
			dwInterface.showErrorMessage("Request to initialize search didn't answer.");
			return;
		}

		String progressURL = CDDTaskChooseServer.getServerURL().concat(PROGRESS_SUFFIX.replace("VAULT", vault)
				.replace("EXPORT_ID", so.get("id").toString()));

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

		String resultURL = CDDTaskChooseServer.getServerURL().concat(RESULT_SUFFIX.replace("VAULT", vault)
				.replace("EXPORT_ID", so.get("id").toString()));

		retrieveJSON(resultURL, dwInterface);
	}

	private void retrieveJSON(String resultURL, IPluginHelper dwInterface) {
		String token = Token.get();
		JSONObject result = Communicator.retrieveObject(resultURL, token, null, false);
		if (result == null) {
			dwInterface.showErrorMessage("Request to retrieve result didn't answer.");
			return;
		}
		JSONArray array = result.getJSONArray("objects");
		if (array == null || array.isEmpty()) {
			dwInterface.showErrorMessage("Your query didn't return any results.");
			return;
		}
		boolean hasMolfile = false;
		int column = 0;
		TreeMap<String,Integer> titleMap = new TreeMap<>();
		for (int i=0; i<array.length(); i++) {
			JSONObject row = array.getJSONObject(i);
			for (String key:row.keySet()) {
				if (key.equals("molfile"))
					hasMolfile = true;
				else if (!titleMap.containsKey(key) && !suppressColumn(key))
					titleMap.put(key, column++);
			}
		}

		boolean hasSmiles = titleMap.containsKey("smiles");
		int columnCount = titleMap.size();

		int offset = 0;
		if (hasMolfile || hasSmiles) {
			columnCount++;
			offset++;
		}

		dwInterface.initializeData(columnCount, array.length(), "CDD Structure Search Result");

		if (hasMolfile || hasSmiles) {
			dwInterface.setColumnTitle(0, "Structure");
			dwInterface.setColumnType(0, hasMolfile ? IPluginHelper.COLUMN_TYPE_STRUCTURE_FROM_MOLFILE
					: IPluginHelper.COLUMN_TYPE_STRUCTURE_FROM_SMILES);
		}

		for (String title: titleMap.keySet())
			dwInterface.setColumnTitle(titleMap.get(title)+offset, title);

		for (int i=0; i<array.length(); i++) {
			JSONObject row = array.getJSONObject(i);
			for (String title: row.keySet()) {
				Object value = row.get(title);
				if (title.equals("molfile")) {
					if (value instanceof String)
						dwInterface.setCellData(0, i, (String)value);
				}
				else if (titleMap.containsKey(title)) {
					if (title.equals("smiles") && value instanceof String)
						dwInterface.setCellData(0, i, (String)value);
					dwInterface.setCellData(titleMap.get(title) + offset, i, toString(value));
				}
			}
		}

		dwInterface.finalizeData(null);
	}

	private boolean  suppressColumn(String title) {
		for (String suppressedTitle: SUPPRESSED_COLUMNS)
			if (title.equals(suppressedTitle))
				return true;

		return false;
	}
}
