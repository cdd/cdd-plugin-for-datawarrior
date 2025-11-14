package com.cdd.datawarrior;

import com.actelion.research.gui.LookAndFeelHelper;
import info.clearthought.layout.TableLayout;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Properties;

/**
 * PluginTask to retrieve data from the CDD Vault.
 */
public abstract class AbstractProjectTask extends AbstractTask {
	private final String DATASETS_SUFFIX = "VAULT/data_sets";
	private final String PROJECTS_SUFFIX = "VAULT/projects";
	protected final String PROGRESS_SUFFIX = "VAULT/export_progress/EXPORT_ID";
	protected final String RESULT_SUFFIX = "VAULT/exports/EXPORT_ID";

	protected final String CONFIGURATION_PROJECTS = "projects";
	protected final String CONFIGURATION_DATASETS = "datasets";

	private static String sServerURL;
	private final boolean mShowDatasets;
	private JList<String> mListProjects,mListDatasets;
	private String[] mProjectID,mDatasetID;
	private JCheckBox mCheckBoxSearchDatasets,mCheckBoxSearchProjects;
	private final ArrayList<UpdateWorker> mPendingWorkerList;

	public AbstractProjectTask(boolean showDatasets) {
		mShowDatasets = showDatasets;
		mPendingWorkerList = new ArrayList<>();
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
		int listHeight = (int)(f * 160);

		JPanel commonContent = createCommonDialogContent(gap);

		final double[] sizeH = { gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL, 3*gap, TableLayout.PREFERRED, gap };
		final double[] sizeV_withDatasets = { gap, TableLayout.PREFERRED, 2*gap, TableLayout.FILL, 3*gap, TableLayout.PREFERRED, gap, listHeight, gap };
		final double[] sizeV_withoutDatasets = { gap, TableLayout.PREFERRED, 2*gap, TableLayout.FILL, gap };
		double[][] size = { sizeH, mShowDatasets ? sizeV_withDatasets : sizeV_withoutDatasets };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(commonContent, "1,1,4,1");

		JComponent innerContent = createInnerDialogContent(gap, dialogHelper);
		if (innerContent != null)
			content.add(innerContent, "1,3,4,3");

		JPanel projectPanel = new JPanel();
		final double[][] projectPanelSize = { { TableLayout.PREFERRED }, { TableLayout.PREFERRED, gap/2, listHeight, TableLayout.FILL } };
		projectPanel.setLayout(new TableLayout(projectPanelSize));
		mCheckBoxSearchProjects = new JCheckBox("Search Selected Projects:", true);
		mCheckBoxSearchProjects.addActionListener(e -> mListProjects.setEnabled(mCheckBoxSearchProjects.isSelected()));
		mListProjects = new JList<>();
		mListProjects.setModel(new DefaultListModel<>());
		mListProjects.setBackground(LookAndFeelHelper.isDarkLookAndFeel() ? mListProjects.getBackground().brighter() : mListProjects.getBackground().darker());
		JScrollPane projectScrollPane = new JScrollPane(mListProjects, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		projectPanel.add(mCheckBoxSearchProjects, "0,0");
		projectPanel.add(projectScrollPane, "0,2,0,3");
		content.add(projectPanel, "6,1,6,3");

		if (mShowDatasets) {
			mCheckBoxSearchDatasets = new JCheckBox("Search Selected Public DataSets:");
			mCheckBoxSearchDatasets.addActionListener(e -> mListDatasets.setEnabled(mCheckBoxSearchDatasets.isSelected()));
			mListDatasets = new JList<>();
			mListDatasets.setModel(new DefaultListModel<>());
			mListDatasets.setBackground(LookAndFeelHelper.isDarkLookAndFeel() ? mListDatasets.getBackground().brighter() : mListDatasets.getBackground().darker());
			mListDatasets.setEnabled(false);
			JScrollPane datasetScrollPane = new JScrollPane(mListDatasets, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			content.add(mCheckBoxSearchDatasets, "1,5,6,5");
			content.add(datasetScrollPane, "1,7,6,7");
		}

		return content;
	}

	public JComponent createInnerDialogContent(int gap, IUserInterfaceHelper dialogHelper) {
		return null;
	}

	@Override
	protected final void vaultUpdated(String vaultID, Properties configuration) {
		cancelWorkers();

		mPendingWorkerList.add(createUpdateProjectsWorker(vaultID, configuration));
		if (mShowDatasets)
			mPendingWorkerList.add(createUpdateDatasetsWorker(vaultID, configuration));

		addUpdateWorkers(mPendingWorkerList, vaultID, configuration);

		nextWorker();
	}

	public abstract void addUpdateWorkers(ArrayList<UpdateWorker> workerList, String vaultID, Properties configuration);

	protected void nextWorker() {
		if (!mPendingWorkerList.isEmpty()) {
			UpdateWorker worker = mPendingWorkerList.remove(0);
			startProgress(worker.getMessage());
			worker.execute();
		}
		else {
			stopProgress();

			JDialog dialog = getDialog();
			if (dialog != null)
				dialog.setMinimumSize(dialog.getPreferredSize());
		}
	}

	@Override
	protected void cancelWorkers() {
		for (UpdateWorker worker:mPendingWorkerList)
			worker.cancel(false);

		mPendingWorkerList.clear();
	}

	private UpdateWorker createUpdateProjectsWorker(String vaultID, Properties configuration) {
		String url = CDDTaskChooseServer.getServerURL().concat(PROJECTS_SUFFIX.replace("VAULT", vaultID));
		return new UpdateWorker(this, url, "Updating projects...", table -> {
			mProjectID = table[DEFAULT_ID_INDEX];
			((DefaultListModel<String>)mListProjects.getModel()).clear();
			for (String projectName:table[DEFAULT_NAME_INDEX]) {
				((DefaultListModel<String>)mListProjects.getModel()).addElement(projectName);
			}
			if (configuration == null) {
				mCheckBoxSearchProjects.setSelected(true);
				mListProjects.setEnabled(true);
				mListProjects.getSelectionModel().addSelectionInterval(0, mProjectID.length - 1);
			}
			else {
				String projects = configuration.getProperty(CONFIGURATION_PROJECTS, "");
				if (!projects.isEmpty()) {
					mCheckBoxSearchProjects.setSelected(true);
					mListProjects.setEnabled(true);
					selectColumnsInList(mListProjects, mProjectID, projects);
				}
				else {
					mListProjects.setEnabled(false);
					mCheckBoxSearchProjects.setSelected(false);
				}
			}

			nextWorker();
		} );
	}

	private UpdateWorker createUpdateDatasetsWorker(String vaultID, Properties configuration) {
		String url = CDDTaskChooseServer.getServerURL().concat(DATASETS_SUFFIX.replace("VAULT", vaultID));
		return new UpdateWorker(this, url, "Updating datasets...", table -> {
			mDatasetID = table[DEFAULT_ID_INDEX];
			((DefaultListModel<String>)mListDatasets.getModel()).clear();
			for (String datasetName:table[DEFAULT_NAME_INDEX])
				((DefaultListModel<String>)mListDatasets.getModel()).addElement(datasetName);
			if (configuration == null) {
				mListDatasets.setEnabled(false);
				mCheckBoxSearchDatasets.setSelected(false);
			}
			else {
				String datasets = configuration.getProperty(CONFIGURATION_DATASETS, "");
				if (!datasets.isEmpty()) {
					mCheckBoxSearchDatasets.setSelected(true);
					mListDatasets.setEnabled(true);
					selectColumnsInList(mListDatasets, mDatasetID, datasets);
				}
				else {
					mListDatasets.setEnabled(false);
					mCheckBoxSearchDatasets.setSelected(false);
				}
			}

			nextWorker();
		} );
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

		if (mCheckBoxSearchProjects.isSelected()) {
			String projects = getSelectedColumnsFromList(mListProjects, mProjectID);
			if (projects != null && !projects.isEmpty()) {
				configuration.setProperty(CONFIGURATION_PROJECTS, projects);
			}
		}

		if (mShowDatasets && mCheckBoxSearchDatasets.isSelected()) {
			String datasets = getSelectedColumnsFromList(mListDatasets, mDatasetID);
			if (datasets != null && !datasets.isEmpty()) {
				configuration.setProperty(CONFIGURATION_DATASETS, datasets);
			}
		}

		return configuration;
	}

	/**
	 * Creates a comma delimited String containing ids of the names selected in the list.
	 * @param list
	 * @param keys
	 * @return null or comma delimited column name list
	 */
	public String getSelectedColumnsFromList(JList<String> list, String[] keys) {
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
	 * Selects all named items in a list whose ids are in given comma-delimited string
	 * @param list
	 * @param keys array with keys matching the current list
	 * @param keyString comma-delimited list of keys to select
	 */
	public void selectColumnsInList(JList<String> list, String[] keys, String keyString) {
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
	 * If not, this method should return a short and clear error message
	 * intended for the user in order to correct the dialog setting.
	 * @param configuration
	 * @return user-interpretable error message or null, if query configuration is valid
	 */
	@Override public String checkConfiguration(Properties configuration) {
		if (configuration.getProperty(CONFIGURATION_PROJECTS, "").isEmpty()
		 && configuration.getProperty(CONFIGURATION_DATASETS, "").isEmpty()) {
			return "You didn't select any own project nor any public dataset to be searched.";
		}
		return null;
	}
}