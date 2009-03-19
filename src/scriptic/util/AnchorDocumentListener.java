package scriptic.util;

import javax.swing.event.*;

import scriptic.vm.FromJava;

public class AnchorDocumentListener implements DocumentListener {
	public DocumentEventHolder changedUpdate = new DocumentEventHolder();
	public DocumentEventHolder insertUpdate  = new DocumentEventHolder();
	public DocumentEventHolder removeUpdate  = new DocumentEventHolder();
	public DocumentEventHolder anyUpdate     = new DocumentEventHolder();

	public void changedUpdate(DocumentEvent e) {
		changedUpdate.event = e;
		    anyUpdate.event = e;
		FromJava.doCodeAtAnchor(changedUpdate);
		FromJava.doCodeAtAnchor(anyUpdate);
	}
	public void insertUpdate(DocumentEvent e) {
		insertUpdate.event = e;
		   anyUpdate.event = e;
		FromJava.doCodeAtAnchor(insertUpdate);
		FromJava.doCodeAtAnchor(anyUpdate);
	}
	public void removeUpdate(DocumentEvent e) {
		removeUpdate.event = e;
	      anyUpdate.event = e;
		FromJava.doCodeAtAnchor(removeUpdate);
		FromJava.doCodeAtAnchor(anyUpdate);
	}
}
