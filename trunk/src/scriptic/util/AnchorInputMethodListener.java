package scriptic.util;

import java.awt.event.*;

import scriptic.vm.FromJava;

public class AnchorInputMethodListener implements InputMethodListener {
	public InputMethodEventHolder caretPositionChanged   = new InputMethodEventHolder();
	public InputMethodEventHolder inputMethodTextChanged = new InputMethodEventHolder();

	public void caretPositionChanged(InputMethodEvent e) {
		caretPositionChanged.event = e;
		FromJava.doCodeAtAnchor(caretPositionChanged);
	}
	public void inputMethodTextChanged(InputMethodEvent e) {
		inputMethodTextChanged.event = e;
		FromJava.doCodeAtAnchor(inputMethodTextChanged);
	}
}
