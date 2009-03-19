package scriptic.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import scriptic.vm.FromJava;

public class AnchorActionListener extends ActionEventHolder implements ActionListener {
    public void actionPerformed(ActionEvent e) {this.event=e; FromJava.doCodeAtAnchor(this);}
}
