package test;

import com.cdd.datawarrior.AbstractTask;
import com.cdd.datawarrior.CDDTaskRetrieveSearchResults;
import com.cdd.datawarrior.CDDTaskRunStructureSearch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Properties;

public class CDDPluginTest {
	private static Properties sTestConfiguration;
	private static JDialog sDialog;
	private static final AbstractTask[] TASK = { new CDDTaskRetrieveSearchResults(), new CDDTaskRunStructureSearch() };

	/**
	 * This class can be used to test any of the CDD tasks. It tests the dialog, performs
	 * the task action, typically some database retrieval, and calls stub methods to populate
	 * a DataWarrior table. Of course, since DataWarrior is not part of this project, only
	 * messages are printed that indicate what would happen in the DataWarrior context.
	 * @param args
	 */
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(() -> {
			String[] taskName = new String[TASK.length];
			for (int i=0; i<taskName.length; i++)
				taskName[i] = TASK[i].getTaskName();

			while (true) {
				int option = JOptionPane.showOptionDialog(null, "Which task do you intend to test?", "CDD Vault Task Tester",
						JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE, null, taskName, TASK[0].getTaskName());

				if (option == JOptionPane.CLOSED_OPTION)
					break;

				AbstractTask task = TASK[option];
				ActionListener al = e -> {
					if (e.getActionCommand().equals("OK")) {
						sTestConfiguration = task.getDialogConfiguration();
						String error = task.checkConfiguration(sTestConfiguration);
						if (error != null) {
							JOptionPane.showMessageDialog(sDialog, error, "Invalid Configuration", JOptionPane.WARNING_MESSAGE);
						}
						else {
							task.startProgress("Performing task...");
							new Thread(() -> {
								task.run(sTestConfiguration, new TestPluginHelper());
								task.stopProgress();
								SwingUtilities.invokeLater(() -> {
									sDialog.setVisible(false);
									sDialog.dispose();
								} );
							} ).start();
						}
					}
					else {
						sDialog.setVisible(false);
						sDialog.dispose();
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

				sDialog = new JDialog((Frame) null, task.getTaskName(), true);
				sDialog.getContentPane().setLayout(new BorderLayout());
				sDialog.getContentPane().add(task.createDialogContent(new TestUserInterfaceHelper()), BorderLayout.CENTER);
				sDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
				sDialog.getRootPane().setDefaultButton(bok);

				if (sTestConfiguration != null) {
					task.setDialogConfiguration(sTestConfiguration);
				}

				sDialog.pack();
				sDialog.setVisible(true);
			}
		} );
	}
}
