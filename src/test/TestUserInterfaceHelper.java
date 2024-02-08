package test;

import org.openmolecules.datawarrior.plugin.IChemistryPanel;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

public class TestUserInterfaceHelper implements IUserInterfaceHelper {
	@Override
	public float getUIScaleFactor() {
		return 1.5f;
	}

	@Override
	public float getRetinaScaleFactor() {
		return 1;
	}

	@Override
	public IChemistryPanel getChemicalEditor() {
		return new TestChemistryPanel();
	}
}
