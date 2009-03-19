package scriptic.util;

import java.awt.event.*;

import scriptic.vm.FromJava;

public class AnchorMouseMotionListener extends EventHolder<ActionEvent> implements MouseMotionListener {
	public MouseEventHolder dragged = new MouseEventHolder();
	public MouseEventHolder moved   = new MouseEventHolder();
	@Override
	public void mouseDragged(MouseEvent e) {
		dragged.event = e;
		FromJava.doCodeAtAnchor(dragged);
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		moved.event = e;
		FromJava.doCodeAtAnchor(moved);
	}
}
