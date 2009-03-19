package scriptic.util;

import java.awt.event.*;

import javax.swing.event.*;

import scriptic.vm.FromJava;

public class AnchorChangeListener extends ChangeEventHolder implements ChangeListener {
	@Override
    public void stateChanged(ChangeEvent e) {this.event=e; FromJava.doCodeAtAnchor(this);}
}
