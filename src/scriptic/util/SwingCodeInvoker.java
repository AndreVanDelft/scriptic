/*
 * Created on 13 feb 2009
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package scriptic.util;

import scriptic.vm.CodeInvokerSynchronous;
import scriptic.vm.CodeInvokerAsynchronous;
import javax.swing.SwingUtilities;

public class SwingCodeInvoker implements CodeInvokerSynchronous, CodeInvokerAsynchronous {

	@Override
	public void invokeSynchronously(Runnable r) throws Exception {
		SwingUtilities.invokeAndWait(r);
	}
	@Override
	public void invokeAsynchronously(Runnable r) throws Exception {
		SwingUtilities.invokeLater(r);
	}
}
