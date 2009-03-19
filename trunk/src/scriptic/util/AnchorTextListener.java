package scriptic.util;

import java.awt.event.TextEvent;
import java.awt.event.TextListener;

import scriptic.vm.FromJava;

public class AnchorTextListener extends TextEventHolder implements TextListener {

	public void textValueChanged(TextEvent e) {this.event=e; FromJava.doCodeAtAnchor(this);}
}
