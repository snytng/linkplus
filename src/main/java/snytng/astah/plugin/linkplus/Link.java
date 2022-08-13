package snytng.astah.plugin.linkplus;

import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.presentation.IPresentation;

public class Link {
	String label;
	IPresentation presentation;
	IDiagram diagram;

	Link(IPresentation presentation, IDiagram diagram){
		this.label = presentation.getLabel();
		this.presentation = presentation;
		this.diagram = diagram;
	}

	Link(String label, IPresentation presentation, IDiagram diagram){
		this.label = label;
		this.presentation = presentation;
		this.diagram = diagram;
	}

	public String getLabel() {
		return label;
	}

	public String getPackageName() {
		StringBuilder sb = new StringBuilder();
		IElement owner = diagram.getOwner();
		while (owner instanceof INamedElement && owner.getOwner() != null) {
			sb.insert(0, ((INamedElement) owner).getName() + "::");
			owner = owner.getOwner();
		}
		return sb.toString();
	}

	public String toString() {
		return getLabel();
	}
}
