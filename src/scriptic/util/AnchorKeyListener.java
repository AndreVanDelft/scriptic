package scriptic.util;

import java.awt.event.*;

import javax.swing.event.*;

import scriptic.vm.FromJava;

public class AnchorKeyListener implements KeyListener {
	public KeyEventHolder pressed  = new KeyEventHolder ();
	public KeyEventHolder released = new KeyEventHolder();
	public KeyEventHolder typed    = new KeyEventHolder ();
	@Override
	public void keyPressed(KeyEvent e) {
		pressed.event=e; 
		FromJava.doCodeAtAnchor(pressed);	
	}
	@Override
	public void keyReleased(KeyEvent e) {
		released.event=e; 
		FromJava.doCodeAtAnchor(released);	
	}
	@Override
	public void keyTyped(KeyEvent e) {
		typed.event=e; 
		FromJava.doCodeAtAnchor(typed);	
	}
}
