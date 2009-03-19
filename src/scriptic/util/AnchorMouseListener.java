package scriptic.util;

import java.awt.event.*;

import scriptic.vm.FromJava;

public class AnchorMouseListener implements MouseListener {
	public MouseEventHolder clicked  = new MouseEventHolder();
	public MouseEventHolder entered  = new MouseEventHolder();
	public MouseEventHolder exited   = new MouseEventHolder();
	public MouseEventHolder pressed  = new MouseEventHolder();
	public MouseEventHolder released = new MouseEventHolder();
	@Override
	public void mouseClicked(MouseEvent e) {
		clicked.event = e;
		FromJava.doCodeAtAnchor(clicked);
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		entered.event = e;
		FromJava.doCodeAtAnchor(entered);
	}
	@Override
	public void mouseExited(MouseEvent e) {
		exited.event = e;
		FromJava.doCodeAtAnchor(exited);
	}
	@Override
	public void mousePressed(MouseEvent e) {
		pressed.event = e;
		FromJava.doCodeAtAnchor(pressed);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		released.event = e;
		FromJava.doCodeAtAnchor(released);
	}
}
