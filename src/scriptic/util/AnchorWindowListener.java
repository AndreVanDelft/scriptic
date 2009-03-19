package scriptic.util;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import scriptic.vm.FromJava;

public class AnchorWindowListener implements WindowListener {
	public WindowEventHolder activated   = new WindowEventHolder();
	public WindowEventHolder closed      = new WindowEventHolder();
	public WindowEventHolder closing     = new WindowEventHolder();
	public WindowEventHolder deactivated = new WindowEventHolder();
	public WindowEventHolder deiconified = new WindowEventHolder();
	public WindowEventHolder iconified   = new WindowEventHolder();
	public WindowEventHolder opened      = new WindowEventHolder();

	public void windowActivated(WindowEvent e) {
		activated.event=e; FromJava.doCodeAtAnchor(activated);
	}
	public void windowClosed(WindowEvent e) {
		closed.event=e; FromJava.doCodeAtAnchor(closed);
	}
	public void windowClosing(WindowEvent e) {
		closing.event=e; FromJava.doCodeAtAnchor(closing);
	}
	public void windowDeactivated(WindowEvent e) {
		deactivated.event=e; FromJava.doCodeAtAnchor(deactivated);
	}
	public void windowDeiconified(WindowEvent e) {
		deiconified.event=e; FromJava.doCodeAtAnchor(deiconified);
	}
	public void windowIconified(WindowEvent e) {
		iconified.event=e; FromJava.doCodeAtAnchor(iconified);
	}
	public void windowOpened(WindowEvent e) {
		opened.event=e; FromJava.doCodeAtAnchor(opened);
	}
}
