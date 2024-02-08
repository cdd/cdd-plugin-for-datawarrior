package com.cdd.datawarrior;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.util.Iterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class UpdateWorker extends SwingWorker<JSONArray, Object> {
	private static final String CONNECTION_ERROR_MESSAGE = "Something went wrong during data retrieval.";
	private static final String[] DEFAULT_KEYS = { "name", "id" };

	AbstractTask mParentTask;
	final String mURL,mMessage;
	final DoneAction mDoneAction;

	public UpdateWorker(AbstractTask parentTask, String url, String message, DoneAction doneAction) {
		mParentTask = parentTask;
		mURL = url;
		mMessage = message;
		mDoneAction = doneAction;
	}

	public String getMessage() {
		return mMessage;
	}

	@Override
	public JSONArray doInBackground() {
		return Communicator.retrieveArray(mURL, Token.get());
	}

	@Override public void done() {
		try {
			String[][] table = createTable(get());
			if (table == null) {
				JOptionPane.showMessageDialog(null, CONNECTION_ERROR_MESSAGE);
				mParentTask.stopProgress();
				return;
			}

			mDoneAction.performDoneAction(table);

		} catch (CancellationException | InterruptedException e) {
			mParentTask.stopProgress();

			JDialog dialog = mParentTask.getDialog();
			if (dialog != null) {
				dialog.setMinimumSize(dialog.getPreferredSize());
			}
		} catch (ExecutionException e) {
			mParentTask.stopProgress();

			JDialog dialog = mParentTask.getDialog();
			if (dialog != null) {
				dialog.setMinimumSize(dialog.getPreferredSize());
			}

			JOptionPane.showMessageDialog(null, CONNECTION_ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	private static String[][] createTable(JSONArray jsonArray) {
		if (jsonArray == null) {
			return null;
		}

		String[][] table = new String[DEFAULT_KEYS.length][jsonArray.length()];
		Iterator<Object> objects = jsonArray.iterator();
		int index2 = 0;
		while (objects.hasNext()) {
			JSONObject jsonObject = (JSONObject)objects.next();
			int index1 = 0;
			for (String key:DEFAULT_KEYS) {
				table[index1++][index2] = jsonObject.get(key).toString();
			}
			index2++;
		}
		return table;
	}

	public interface DoneAction {
		// After successfully retrieving a name-and-ID table the performDoneAction() method is called
		// with table[2][n]: table[0] contains names and table[1] contains associated IDs.
		void performDoneAction(String[][] table);
	}
}
