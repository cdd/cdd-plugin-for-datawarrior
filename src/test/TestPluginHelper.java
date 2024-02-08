package test;

import org.openmolecules.datawarrior.plugin.IPluginHelper;

public class TestPluginHelper implements IPluginHelper {
	private int mCurrentCellDataRow;

	@Override
	public void initializeData(int columnCount, int rowCount, String newWindowName) {
		System.out.println("initializeData(columns:"+columnCount+", rows:"+rowCount+" windowName:"+newWindowName+")");
	}

	@Override
	public void setColumnTitle(int column, String title) {
		System.out.println("setColumnTitle(column:"+column+", title:"+title+")");
	}

	@Override
	public void setColumnType(int column, int type) {
		System.out.println("setColumnType(column:"+column+", type:"+type+")");
	}

	@Override
	public void setCellData(int column, int row, String value) {
		if (mCurrentCellDataRow != row) {
			mCurrentCellDataRow = row;
			System.out.print(".");
			if ((row & 0xFF) == 0xFF)
				System.out.println();
		}
	}

	@Override
	public void finalizeData(String template) {
		System.out.println();
		System.out.println("finalizeData(template:"+(template==null?"null":template.substring(0, 40))+"...)");
	}

	@Override
	public void showErrorMessage(String message) {
		System.out.println("showErrorMessage(message:"+message+")");
	}

	@Override
	public boolean isCancelled() {
		return false;
	}
}
