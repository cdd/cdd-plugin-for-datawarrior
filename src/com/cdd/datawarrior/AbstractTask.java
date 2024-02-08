package com.cdd.datawarrior;

import info.clearthought.layout.TableLayout;
import org.openmolecules.datawarrior.plugin.IPluginTask;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.util.Properties;

public abstract class AbstractTask implements IPluginTask {
	protected final String CONFIGURATION_VAULT_ID = "vault";
	private static final String EMPTY_TOKEN = "<no API Key>";
	private final String VAULT_URL = "https://app.collaborativedrug.com/api/v1/vaults";
	protected static final int DEFAULT_NAME_INDEX = 0;
	protected static final int DEFAULT_ID_INDEX = 1;

	private JLabel mLabelToken, mProgressLabel;
	private JProgressBar mProgressBar;
	private JComboBox<String> mComboBoxVaultID;
	private String mCurrentVaultID;
	private String[] mVaultID;
	private UpdateWorker mUpdateVaultsWorker;
	private Properties mRecentConfiguration;

	protected JPanel createCommonDialogContent(int gap) {
		mCurrentVaultID = null;
		final String token = Token.get();

		double[][] size = {
				{TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL},
				{TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL}};

		JPanel content = new JPanel();
		content.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent e) {
				// to trigger the initial cascade of updates
				if (token != null && !token.isEmpty()) {
					tokenUpdated();
				}
			}

			@Override
			public void ancestorMoved(AncestorEvent e) {}

			@Override
			public void ancestorRemoved(AncestorEvent e) {
				cancelAllWorkers();
			}
		});
		content.setLayout(new TableLayout(size));

		mLabelToken = new JLabel(formatTokenForDisplay(token));
		JButton buttonSetToken = new JButton("Set API Key");
		buttonSetToken.addActionListener(e -> {
			JTextField tokenInput = new JTextField();
			JCheckBox saveTokenInput = new JCheckBox("Save API Key for future sessions");
			Object[] setTokenDialogComponents = {
					"Please provide your CDD API Key.",
					tokenInput,
					saveTokenInput
			};
			int result = JOptionPane.showConfirmDialog(null, setTokenDialogComponents, "CDD API Key", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				Token.set(tokenInput.getText(), saveTokenInput.isSelected());
				mLabelToken.setText(formatTokenForDisplay(Token.get()));
				tokenUpdated();
			}
		});

		content.add(buttonSetToken, "0,0");
		content.add(mLabelToken, "2,0");

		mComboBoxVaultID = new JComboBox<>();
		mComboBoxVaultID.addActionListener(e -> {
			int vault = mComboBoxVaultID.getSelectedIndex();
			if (vault>=0 && vault<mVaultID.length && !mVaultID[vault].equals(mCurrentVaultID)) {
				mCurrentVaultID = mVaultID[vault];
				vaultUpdated(mVaultID[vault], mRecentConfiguration);
				mRecentConfiguration = null;
			}
		});
		content.add(new JLabel("CDD Vault®:", JLabel.RIGHT), "0,2");
		content.add(mComboBoxVaultID, "2,2");

		mProgressLabel = new JLabel();
		mProgressLabel.setForeground(Color.GREEN);
		content.add(mProgressLabel, "2,4");
		mProgressBar = new JProgressBar();
		mProgressBar.setIndeterminate(true);
		mProgressBar.setVisible(false);
		content.add(mProgressBar, "0,4");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		if (mComboBoxVaultID.getSelectedItem() != null) {
			configuration.setProperty(CONFIGURATION_VAULT_ID, mVaultID[mComboBoxVaultID.getSelectedIndex()]);
		}

		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mRecentConfiguration = configuration;
		// We cannot apply directly, because dialog element content will be retrieved by a cascade of SwingWorkers.
		// We rather pass the configuration to every worker to select proper options after retrieving all options.
	}

	private String formatTokenForDisplay(String token) {
		return token == null || token.isEmpty() ? EMPTY_TOKEN : "API Key: " + (token.length()>12 ? token.substring(0, 12) + "..." : token);
	}

	private void tokenUpdated() {
		startProgress("updating vaults...");

		cancelAllWorkers();

		mUpdateVaultsWorker = createUpdateVaultsWorker();
		mUpdateVaultsWorker.execute();
	}

	public void startProgress(String message) {
		mProgressLabel.setText(message);
		mProgressBar.setVisible(true);
	}

	protected JDialog getDialog() {
		return mLabelToken == null ? null : (JDialog)SwingUtilities.getRoot(mLabelToken);
	}

	public void stopProgress() {
		mProgressLabel.setText("");
		mProgressBar.setVisible(false);
	}

	private void cancelAllWorkers() {
		if (mUpdateVaultsWorker != null)
			mUpdateVaultsWorker.cancel(false);

		cancelWorkers();
	}

	protected abstract void cancelWorkers();

	protected abstract void vaultUpdated(String vaultID, Properties configuration);

	private UpdateWorker createUpdateVaultsWorker() {
		return new UpdateWorker(this, VAULT_URL, "Updating vaults...", table -> {
			mVaultID = table[DEFAULT_ID_INDEX];
			mComboBoxVaultID.removeAllItems();
			for (String vaultName : table[DEFAULT_NAME_INDEX])
				mComboBoxVaultID.addItem(vaultName);

			if (mComboBoxVaultID.getItemCount() != 0) {
				int vaultIndex = 0;     // default
				if (mRecentConfiguration != null) {
					String vaultID = mRecentConfiguration.getProperty(CONFIGURATION_VAULT_ID);
					for (int i=0; i<mVaultID.length; i++) {
						if (mVaultID[i].equals(vaultID)) {
							vaultIndex = i;
							break;
						}
					}
				}
				mComboBoxVaultID.setSelectedIndex(vaultIndex);
				mCurrentVaultID = mVaultID[vaultIndex];
			}
		});
	}
}