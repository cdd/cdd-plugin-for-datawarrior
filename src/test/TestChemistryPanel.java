package test;

import com.actelion.research.chem.*;
import com.actelion.research.gui.JEditableStructureView;
import org.openmolecules.datawarrior.plugin.IChemistryPanel;

public class TestChemistryPanel extends JEditableStructureView implements IChemistryPanel {
	public TestChemistryPanel() {
		super(new StereoMolecule());
	}

	@Override public void setMode(int mode) {
		getMolecule().setFragment(mode != IChemistryPanel.MODE_MOLECULE);
		setAllowQueryFeatures(mode != MODE_FRAGMENT_WITHOUT_QUERY_FEATURES);
		structureChanged();
	}

	@Override public boolean isEmptyMolecule() {
		return getMolecule().getAllAtoms() == 0;
	}

	@Override public String getMoleculeAsIDCode() {
		Canonizer canonizer = new Canonizer(getMolecule());
		return canonizer.getIDCode().concat(" ").concat(canonizer.getEncodedCoordinates());
	}

	@Override public String getMoleculeAsMolfileV2() {
		return new MolfileCreator(getMolecule()).getMolfile();
	}

	@Override public String getMoleculeAsMolfileV3() {
		return new MolfileV3Creator(getMolecule()).getMolfile();
	}

	@Override public String getMoleculeAsSmiles() {
		return new IsomericSmilesCreator(getMolecule()).getSmiles();
	}

	@Override public void setMoleculeFromIDCode(String idcode) {
		int index = idcode.indexOf(" ");
		if (index == -1)
			setIDCode(idcode);
		else
			setIDCode(idcode.substring(0, index), idcode.substring(index+1));
	}

	@Override public void setMoleculeFromMolfile(String molfile) {
		new MolfileParser().parse(getMolecule(), molfile);
		structureChanged();
	}

	@Override public void setMoleculeFromSmiles(String smiles) {
		try {
			new SmilesParser().parse(getMolecule(), smiles);
			structureChanged();
		} catch (Exception e) {}
	}
}
