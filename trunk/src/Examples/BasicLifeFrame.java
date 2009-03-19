package Examples;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class BasicLifeFrame extends javax.swing.JFrame {
	public JToolBar toolBar;
	public JButton stopButton;
	public JButton randomizeButton;
	public JButton clearButton;
	public JButton fasterButton;
	public JButton slowerButton;
	public JButton stepButton;
	public LifeCanvas canvas;
	public JButton exitButton;
	public JSlider speedSlider;
	public JButton maxSpeedButton;
	public JButton minSpeedButton;
	public JLabel speedLabel;
	public JButton startButton;

	/**
	* Auto-generated main method to display this JFrame
	*/
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				BasicLifeFrame inst = new BasicLifeFrame();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}
	
	public BasicLifeFrame() {
		super();
		initGUI();
	}
	
	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			getContentPane().setLayout(thisLayout);
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			this.setMinimumSize(new java.awt.Dimension(551, 367));
			   {
				toolBar = new JToolBar();
				getContentPane().add(toolBar, BorderLayout.NORTH);
				FlowLayout jToolBar1Layout = new FlowLayout();
				toolBar.setLayout(jToolBar1Layout);
				toolBar.setPreferredSize(new java.awt.Dimension(392, 40));
				{
					stepButton = new JButton();
					toolBar.add(stepButton);
					stepButton.setText("Step");
					stepButton.setEnabled(false);
					stepButton.setFocusable(false);
				}
				{
					startButton = new JButton();
					toolBar.add(startButton);
					startButton.setText("Start");
					startButton.setEnabled(false);
					startButton.setFocusable(false);
				}
				{
					stopButton = new JButton();
					toolBar.add(stopButton);
					stopButton.setText("Stop");
					stopButton.setEnabled(false);
					stopButton.setFocusable(false);
				}
				{
					randomizeButton = new JButton();
					toolBar.add(randomizeButton);
					randomizeButton.setText("Random");
					randomizeButton.setEnabled(false);
					randomizeButton.setFocusable(false);
				}
				{
					clearButton = new JButton();
					toolBar.add(clearButton);
					clearButton.setText("Clear");
					clearButton.setEnabled(false);
					clearButton.setFocusable(false);
				}
				{
					exitButton = new JButton();
					toolBar.add(exitButton);
					exitButton.setText("Exit");
					exitButton.setEnabled(false);
					exitButton.setFocusable(false);
				}
				{
					speedLabel = new JLabel();
					toolBar.add(speedLabel);
					speedLabel.setText("Speed:            ");
					speedLabel.setMinimumSize(new Dimension(70,14));
				}
				{
					minSpeedButton = new JButton();
					toolBar.add(minSpeedButton);
					minSpeedButton.setText("<<");
					minSpeedButton.setEnabled(false);
					minSpeedButton.setFocusable(false);
				}
				{
					slowerButton = new JButton();
					toolBar.add(slowerButton);
					slowerButton.setText("<");
					slowerButton.setEnabled(false);
					slowerButton.setFocusable(false);
				}
				{
					speedSlider = new JSlider();
					toolBar.add(speedSlider);
					speedSlider.setPreferredSize(new java.awt.Dimension(67, 16));
					speedSlider.setIgnoreRepaint(true);
					speedSlider.setMaximum(10);
					speedSlider.setMinimum(1);
					speedSlider.setEnabled(false);
					speedSlider.setFocusable(false);
				}
				{
					fasterButton = new JButton();
					toolBar.add(fasterButton);
					fasterButton.setText(">");
					fasterButton.setEnabled(false);
					fasterButton.setFocusable(false);
				}
				{
					maxSpeedButton = new JButton();
					toolBar.add(maxSpeedButton);
					maxSpeedButton.setText(">>");
					maxSpeedButton.setEnabled(false);
					maxSpeedButton.setFocusable(false);
				}
			}
			{
				canvas = new LifeCanvas();
				getContentPane().add(canvas, BorderLayout.CENTER);
				canvas.setPreferredSize(new java.awt.Dimension(407, 226));
				canvas.setFocusable(false);
			}
			pack();
			this.setSize(551, 367);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
