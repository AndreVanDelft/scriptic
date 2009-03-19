package scriptic.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import scriptic.vm.FromJava;

public class AnchorPropertyChangeListener extends PropertyChangeEventHolder implements PropertyChangeListener {

	public void propertyChange(PropertyChangeEvent e) {this.event=e; FromJava.doCodeAtAnchor(this);}
}
