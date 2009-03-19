package scriptic.util;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import scriptic.vm.FromJava;

public class AnchorFocusListener implements FocusListener {
	public FocusEventHolder gained = new FocusEventHolder();
	public FocusEventHolder lost   = new FocusEventHolder();
	@Override
	public void focusGained(FocusEvent e) {
		gained.event = e;
		FromJava.doCodeAtAnchor(gained);
	}
	@Override
	public void focusLost(FocusEvent e) {
		lost.event = e;
		FromJava.doCodeAtAnchor(lost);
	}
}
