/*
 * Created on 13 feb 2009
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package scriptic.util;

import scriptic.vm.CodeInvoker;
import javax.swing.SwingUtilities;

public class SwingCodeInvoker implements CodeInvoker {

	@Override
	public void invokeAndWait(Runnable r) throws Exception {
		SwingUtilities.invokeAndWait(r);
	}
	@Override
	public void invokeLater(Runnable r) throws Exception {
		SwingUtilities.invokeLater(r);
	}
}
