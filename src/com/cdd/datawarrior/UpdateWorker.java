package com.cdd.datawarrior;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class UpdateWorker extends SwingWorker<JSONArray, Object> {
	private static final String CONNECTION_ERROR_MESSAGE = "Something went wrong during data retrieval.";
	private static final String UNAUTHORIZED_ERROR_MESSAGE = "You are not authorized to access this server.";
	private static final String[] DEFAULT_KEYS = { "name", "id" };
	private static final int DEFAULT_KEY_INDEX_FOR_SORTING = 0;

	private AbstractTask mParentTask;
	private final String mURL,mMessage;
	private final DoneAction mDoneAction;

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
				JOptionPane.showMessageDialog(mParentTask.getDialog(), Communicator.sErrorCode == 401 ?
						UNAUTHORIZED_ERROR_MESSAGE : CONNECTION_ERROR_MESSAGE);
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

		TreeSet<String> nameSet = new TreeSet<>((s1,s2) -> s1.compareToIgnoreCase(s2));
		for (Object o : jsonArray)
			nameSet.add(((JSONObject)o).get(DEFAULT_KEYS[DEFAULT_KEY_INDEX_FOR_SORTING]).toString());
		int index = 0;
		TreeMap<String,Integer> nameToIndexMap = new TreeMap<>();
		for (String name:nameSet)
			nameToIndexMap.put(name, index++);

		String[][] table = new String[DEFAULT_KEYS.length][jsonArray.length()];
		for (Object o : jsonArray) {
			JSONObject jsonObject = (JSONObject)o;
			int index1 = nameToIndexMap.get(jsonObject.get(DEFAULT_KEYS[DEFAULT_KEY_INDEX_FOR_SORTING]).toString());
			int index2 = 0;
			for (String key:DEFAULT_KEYS) {
				table[index2++][index1] = jsonObject.get(key).toString();
			}
		}

		return table;
	}

	public interface DoneAction {
		// After successfully retrieving a name-and-ID table the performDoneAction() method is called
		// with table[2][n]: table[0] contains names and table[1] contains associated IDs.
		void performDoneAction(String[][] table);
	}
}
