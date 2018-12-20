package com.adaptiveenergy.imagej;

import java.text.NumberFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFormattedTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import javax.swing.text.NumberFormatter;

import java.awt.Event;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.gui.ShapeRoi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class Contrast_Sensitivity implements PlugInFilter {
	protected ImagePlus image;

	// image property members
	private int width;
	private int height;

	// plugin parameters
	public double value;
	public String name;

	@Override
	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		image = imp;
		return DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
	}

	@Override
	public void run(ImageProcessor ip) {
		// get width and height
		width = ip.getWidth();
		height = ip.getHeight();
		
		new CSWindow(image,ip);
	}

	public void showAbout() {
		IJ.showMessage("Signal level and SNR wizard"
		);
	}
}
class CSWindow extends JFrame implements RoiListener, WindowListener{
	int i;
	
	private JPanel contentPane;
	private JFormattedTextField textField_x;
	private JFormattedTextField textField_y;
	private JFormattedTextField textField_d;
	private JFormattedTextField textField_mt_step;
	private JFormattedTextField textField_mt_iqi;
	private JFormattedTextField textField_gbv;
	private JLabel lblMaterialThicknessOf;
	private JLabel lblGbv;
	private JLabel lblGvOfHole;
	private JLabel lblGvHoleLabel;
	private JLabel lblGvOfMean;
	private JLabel lblGvMeanLabel;
	private JLabel lblSigma;
	private JLabel lblSigmaValueLabel;
	private JLabel lblCnr;
	private JLabel lblCNRLabel;
	private JLabel lblContrastSensitivity;
	private JLabel lblCSLabel;
	
	NumberFormatter XFieldFormatter;
	NumberFormatter YFieldFormatter;
	NumberFormatter DFieldFormatter;
	
	ImagePlus imp;
	private ImageProcessor ip;
	
	Roi roi;
	Rectangle rect;
	
	Roi halfroi;	
	ImagePlus halfroiImp;
	ImageProcessor halfroiIp;
	ImageStatistics halfmeasure;
	
	ShapeRoi doubleroi;
	ShapeRoi quadroi;
	
	ShapeRoi bandroi;
	ImagePlus bandroiImp;
	ImageProcessor bandroiIp;
	ImageStatistics bandmeasure;
		
	private int width;
	private int height;
	
	private boolean FLAG = true;
	private boolean SFLAG = false;
	private boolean closeflag = false;
	private long moditime;
	
	public CSWindow(ImagePlus image, ImageProcessor ip) {
		i = 0;
		
		this.ip = ip;
		this.imp = image;		
		Roi.addRoiListener(this);
		addWindowListener(this);
		width = ip.getWidth();
		height = ip.getHeight();
		
		setTitle("Contrast Sensitivity");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 887, 201);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		JLabel lblXOfCenter = new JLabel("X coordinate of square");		
		JLabel lblYCoordinateOf = new JLabel("Y coordinate of square");	
		JLabel lblDOfCenter = new JLabel("width of square");	
		JLabel lblMaterial = new JLabel("MT of step (mm)");	
		lblGbv = new JLabel("GBV");		
		lblMaterialThicknessOf = new JLabel("MT of IQI (mm)");	
		lblGvOfHole = new JLabel("GV of hole");		
		lblGvHoleLabel = new JLabel("");		
		lblGvOfMean = new JLabel("GV of mean");		
		lblGvMeanLabel = new JLabel("");		
		lblSigma = new JLabel("Sigma");		
		lblSigmaValueLabel = new JLabel("");		
		lblCnr = new JLabel("contrast noise ratio");		
		lblCNRLabel = new JLabel("");		
		lblContrastSensitivity = new JLabel("contrast sensitivity");		
		lblCSLabel = new JLabel("");
		
		
		NumberFormat IntegerFieldFormat = NumberFormat.getIntegerInstance();
		XFieldFormatter = new NumberFormatter(IntegerFieldFormat);
		XFieldFormatter.setMinimum(0);
		XFieldFormatter.setMaximum(width);
		YFieldFormatter = new NumberFormatter(IntegerFieldFormat);
		YFieldFormatter.setMinimum(0);
		YFieldFormatter.setMaximum(height);
		DFieldFormatter = new NumberFormatter(IntegerFieldFormat);
		DFieldFormatter.setMinimum(0);
		DFieldFormatter.setMaximum(Math.min(width, height));
		DecimalFormat InputFormatter = new DecimalFormat("##.##");
		
		textField_x = new JFormattedTextField(XFieldFormatter);
		textField_x.setColumns(10);			
		textField_y = new JFormattedTextField(YFieldFormatter);
		textField_y.setColumns(10);			
		textField_d = new JFormattedTextField(DFieldFormatter);
		textField_d.setColumns(10);			
		textField_mt_step = new JFormattedTextField(InputFormatter);
		textField_mt_step.setValue(50.0);
		textField_mt_step.setColumns(10);					
		textField_mt_iqi = new JFormattedTextField(InputFormatter);
		textField_mt_iqi.setValue(0.5);
		textField_mt_iqi.setColumns(10);		
		textField_gbv = new JFormattedTextField(InputFormatter);
		textField_gbv.setValue(2.5);
		textField_gbv.setColumns(10);	
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING, false)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(108)
							.addComponent(lblCnr, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addContainerGap()
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING, false)
									.addComponent(lblMaterial, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(lblXOfCenter, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE))
								.addComponent(lblGvOfHole, GroupLayout.PREFERRED_SIZE, 78, GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addComponent(textField_x, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(textField_mt_step, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblGvHoleLabel))))
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(48)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING, false)
									.addComponent(lblMaterialThicknessOf, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(lblYCoordinateOf, GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE))
								.addComponent(lblGvOfMean, GroupLayout.PREFERRED_SIZE, 84, GroupLayout.PREFERRED_SIZE)))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lblCNRLabel)))
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addComponent(textField_y, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(textField_mt_iqi, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblGvMeanLabel))
							.addGap(18)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addComponent(lblDOfCenter, GroupLayout.PREFERRED_SIZE, 128, GroupLayout.PREFERRED_SIZE)
								.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING, false)
									.addComponent(lblSigma, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(lblGbv, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addComponent(textField_d, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
									.addComponent(lblSigmaValueLabel)
									.addComponent(textField_gbv, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(77)
							.addComponent(lblContrastSensitivity, GroupLayout.PREFERRED_SIZE, 128, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lblCSLabel)))
					.addContainerGap(125, Short.MAX_VALUE))
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblXOfCenter)
						.addComponent(lblYCoordinateOf)
						.addComponent(textField_y, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(textField_x, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblDOfCenter)
						.addComponent(textField_d, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblMaterial)
						.addComponent(textField_gbv, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(textField_mt_step, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblMaterialThicknessOf)
						.addComponent(textField_mt_iqi, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblGbv))
					.addGap(18)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblGvOfHole)
						.addComponent(lblGvHoleLabel)
						.addComponent(lblGvOfMean)
						.addComponent(lblGvMeanLabel)
						.addComponent(lblSigma)
						.addComponent(lblSigmaValueLabel))
					.addGap(18)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblCnr)
						.addComponent(lblCNRLabel)
						.addComponent(lblContrastSensitivity)
						.addComponent(lblCSLabel))
					.addContainerGap(19, Short.MAX_VALUE))
		);
		
		contentPane.setLayout(gl_contentPane);
			
		textField_x.addPropertyChangeListener("value", new PropertyChangeListener()
	    {
	        @Override
	        public void propertyChange(PropertyChangeEvent evt)
	        {	
	        	Rectangle temprect = roi.getBounds();
	        	if(FLAG == false) {
	        		Roi temproi = new Roi((int)textField_x.getValue(),temprect.y,temprect.width,temprect.width);
	        		roi = temproi;
	        		imp.setRoi(roi);
	        		roiModified(imp,3);
	        	}    	        	
	        	DFieldFormatter.setMaximum(Math.min(height-temprect.y,width-(int)textField_x.getValue()));
	        }
	    });
		
		textField_y.addPropertyChangeListener("value", new PropertyChangeListener()
	    {
	        @Override
	        public void propertyChange(PropertyChangeEvent evt)
	        {	
	        	Rectangle temprect = roi.getBounds();
	        	if(FLAG == false) {	        		
	        		Roi temproi = new Roi(temprect.x,(int)textField_y.getValue(),temprect.width,temprect.width);
	        		roi = temproi;
	        		imp.setRoi(roi);
	        		roiModified(imp,3);
	        	}      
	        	DFieldFormatter.setMaximum(Math.min(height-(int)textField_y.getValue(),width-temprect.x));
	        }
	    });
		
		textField_d.addPropertyChangeListener("value", new PropertyChangeListener()
	    {
	        @Override
	        public void propertyChange(PropertyChangeEvent evt)
	        {
	        	Rectangle temprect = roi.getBounds();
	        	if(FLAG == false) {
	        		Roi temproi = new Roi(temprect.x,temprect.y,(int)textField_d.getValue(),(int)textField_d.getValue());
		        	roi = temproi;
		        	imp.setRoi(roi); 
		        	roiModified(imp,3);
	        	} 
	        	XFieldFormatter.setMaximum(width-(int)textField_d.getValue());
	        	YFieldFormatter.setMaximum(height-(int)textField_d.getValue());
	        }
	    });
			
		if(!closeflag&&imp.getRoi()!=null) {
			rect = imp.getRoi().getBounds();
			if(imp.getRoi().getType()==Roi.RECTANGLE) {
				Roi temproi = new Roi(rect.x,rect.y,(rect.width+rect.height)/2,(rect.width+rect.height)/2);
        		roi = temproi;
        		imp.setRoi(roi);
				rect = roi.getBounds();
				halfroi = new Roi(rect.x+rect.width/2,rect.y+rect.height/2,rect.width/2,rect.height/2);
				imp.setRoi(halfroi);
				halfroiImp = halfroi.getImage();
				halfroiIp = halfroiImp.getProcessor();
				halfmeasure = halfroiIp.getStatistics();
				Rectangle doublerect = new Rectangle(rect.x-rect.width/2,rect.y-rect.height/2,2*rect.width,2*rect.height); 
				doubleroi = new ShapeRoi(doublerect);
				Rectangle quadroirect = new Rectangle(rect.x-rect.width,rect.y-rect.height,4*rect.width,4*rect.height); 
				quadroi = new ShapeRoi(quadroirect);
				bandroi = quadroi.not(doubleroi);
				imp.setRoi(bandroi);
				bandroiImp = imp.duplicate();
				bandroiIp = bandroiImp.getProcessor();
				bandmeasure = bandroiIp.getStatistics();			
				imp.setRoi(roi);
				roiModified(imp,3);
				updateROI();
			}
			else {
				//roi=null;
				//imp.setRoi(roi);
				//JOptionPane.showMessageDialog(null, "square only");  
			}
		}		
		
		Timer timer = new Timer();  
		timer.schedule(new TimerTask(){
			 public void run() {
	            	if(!closeflag&&imp.getRoi()!=null) {
	        			roi = imp.getRoi();
	        			if(roi.getType()==Roi.RECTANGLE) {
	        				if(System.currentTimeMillis()-moditime>1000) {
	        					Rectangle rect = roi.getBounds();
	        					if(rect.width!=rect.height) {
	        						Roi temproi = new Roi(rect.x,rect.y,(int)((rect.width+rect.height)/2),(int)((rect.width+rect.height)/2));
		                    		roi = temproi;   	                   		
		                    		rect = roi.getBounds();	
		            				halfroi = new Roi(rect.x+rect.width/4,rect.y+rect.height/4,rect.width/2,rect.height/2);
		            				SFLAG = true;
		            				imp.setRoi(halfroi);
		            				halfroiImp = imp.duplicate();
		            				halfroiIp = halfroiImp.getProcessor();
		            				halfmeasure = halfroiIp.getStatistics();
		            				Rectangle doublerect = new Rectangle(rect.x-rect.width/2,rect.y-rect.height/2,2*rect.width,2*rect.height); 
		            				doubleroi = new ShapeRoi(doublerect);
		            				Rectangle quadroirect = new Rectangle(rect.x-rect.width,rect.y-rect.height,4*rect.width,4*rect.height); 
		            				quadroi = new ShapeRoi(quadroirect);
		            				bandroi = quadroi.not(doubleroi);
		            				imp.setRoi(bandroi);
		            				bandroiImp = imp.duplicate();
		            				bandroiIp = bandroiImp.getProcessor();
		            				bandmeasure = bandroiIp.getStatistics();
		            				SFLAG = false;
		            				imp.setRoi(roi);			
		            				updateROI();	
		                    		
		                    		
		            				roiModified(imp,3);
	        					}  
	        					else {
	        						Roi temproi = new Roi(rect.x,rect.y,(int)((rect.width+rect.height)/2),(int)((rect.width+rect.height)/2));
		                    		roi = temproi;   	                   		
		                    		rect = roi.getBounds();	
		            				halfroi = new Roi(rect.x+rect.width/4,rect.y+rect.height/4,rect.width/2,rect.height/2);
		            				SFLAG = true;
		            				imp.setRoi(halfroi);
		            				halfroiImp = imp.duplicate();
		            				halfroiIp = halfroiImp.getProcessor();
		            				halfmeasure = halfroiIp.getStatistics();
		            				Rectangle doublerect = new Rectangle(rect.x-rect.width/2,rect.y-rect.height/2,2*rect.width,2*rect.height); 
		            				doubleroi = new ShapeRoi(doublerect);
		            				Rectangle quadroirect = new Rectangle(rect.x-rect.width,rect.y-rect.height,4*rect.width,4*rect.height); 
		            				quadroi = new ShapeRoi(quadroirect);
		            				bandroi = quadroi.not(doubleroi);
		            				imp.setRoi(bandroi);
		            				bandroiImp = imp.duplicate();
		            				bandroiIp = bandroiImp.getProcessor();
		            				bandmeasure = bandroiIp.getStatistics();
		            				SFLAG = false;
		            				imp.setRoi(roi);			
		            				updateROI();	
		                    		
		                    		
		            				roiModified(imp,3);
	        					}
	        				}
	        			}
	        			else {
	        				//roi=null;
	        				//imp.setRoi(roi);
	        				//JOptionPane.showMessageDialog(null, "square only");  
	        			}
	        		}			                
	            }
		}, 200, 1000);
		
		setVisible(true);
	}

	@Override
	public void roiModified(ImagePlus imp, int id) {		
		FLAG = true;
		if(id==DELETED) {
			deleteROI();
		}
		else {
			roi = imp.getRoi();				
			if(roi.getType()==Roi.RECTANGLE) {	
				rect = roi.getBounds();
				if(SFLAG==false) {
					moditime = System.currentTimeMillis();
				}
				else{
					//do nothing
				}
			}
			else if(roi.getType()==9) {
				//do nothing!!!!!!!!
			}
			else {				
				//roi=null;
				//imp.setRoi(roi);
				//JOptionPane.showMessageDialog(null, "square only");  
				deleteROI();	
			}				
		}
		FLAG = false;
	}

	private void deleteROI() {
		// TODO Auto-generated method stub
		textField_x.setText(null);
		textField_d.setText(null);
		textField_y.setText(null);
		lblGvHoleLabel.setText(null);	
		lblGvMeanLabel.setText(null);	
		lblSigmaValueLabel.setText(null);	
		lblCNRLabel.setText(null);	
		lblCSLabel.setText(null);		
	}
	
	private void updateROI() {
		// TODO Auto-generated method stub
		textField_x.setValue(rect.x);
		textField_d.setValue(rect.width);
		textField_y.setValue(rect.y);
		lblGvHoleLabel.setText(String.valueOf(round(halfmeasure.median,2)));
		lblGvMeanLabel.setText(String.valueOf(round(bandmeasure.mean,2)));
		lblSigmaValueLabel.setText(String.valueOf(round(bandmeasure.stdDev,2)));
		double CNR = (halfmeasure.median - bandmeasure.mean)/bandmeasure.stdDev;
		double CS = ((double)textField_gbv.getValue()/CNR)*((double)textField_mt_iqi.getValue()/(double)textField_mt_step.getValue())*100;
		lblCNRLabel.setText(String.valueOf(round(CNR,2)));
		lblCSLabel.setText(String.valueOf(round(CS,2)));
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		Roi.removeRoiListener(this);
		closeflag = true;
		dispose();
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
  	     
}

