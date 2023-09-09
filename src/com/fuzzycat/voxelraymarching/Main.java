package com.fuzzycat.voxelraymarching;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.fuzzycat.distancefunction.util.MathUtil;
import com.fuzzycat.voxelraymarching.graphics.Camera;
import com.fuzzycat.voxelraymarching.graphics.Render;
import com.fuzzycat.voxelraymarching.graphics.RenderContext;
import com.fuzzycat.voxelraymarching.graphics.Vector3;
import com.fuzzycat.voxelraymarching.voxel.DistanceFieldGenerator;
import com.fuzzycat.voxelraymarching.voxel.NormalFieldGenerator;
import com.fuzzycat.voxelraymarching.voxel.VoxelFile;

public class Main extends JPanel implements Runnable, KeyListener, MouseListener, MouseWheelListener {
	
	private static final int WIDTH = 800;
	private static final int HEIGHT = 700;
	
	private JFrame frame;
	private boolean close;
	private long numFrames;
	private long startTime;
	
	private Render render;
	private BufferedImage screenPixels;
	private BufferedImage photoSphereColor;
	private RenderContext renderContext;
	
	private Robot mouse;
	private boolean mouseLook;
	private Cursor noCursor;
	private int zoom;
	
	public Main() {
		close = false;
		
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setMinimumSize(getPreferredSize());
		setMaximumSize(getPreferredSize());
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		if (render != null) {
			synchronized (render) {
				g.drawImage(screenPixels, 0, 0, null);
			}
		}
	}
	
	/*
	 * Teacup:
	 *   teacup_330x.bin
	 *   beach_blurred_3.png
	 *   diffuseSpecularRatio = 0.2
	 *   diffuseColor = 0xff4000
	 *   
	 * Horse:
	 *   horse_330x.bin
	 *   beach_blurred_2.png
	 *   diffuseSpecularRatio = 0.8
	 *   diffuseColor = 0xff3c0b
	 *   
	 * Skull:
	 *   skull_330x.bin
	 *   beach_blurred_1.png
	 *   diffuseSpecularRatio = 0.3
	 *   diffuseColor = 0xffffff
	 */
	public void beginLoop() {
		int s = 330;
		int[] torusVoxelMap = VoxelFile.loadBitmap("skull_330x.bin", s, s, s);
		double[] torusDistanceField = DistanceFieldGenerator.createSignedDistanceFieldFromMap(VoxelFile.createDistanceMapFromBitmap(torusVoxelMap, s, s, s), s, s);
		double[] torusNormalField = NormalFieldGenerator.createNormalFieldFromSignedDistanceField(torusDistanceField, s, s, 7);
		
		numFrames = 0;
		startTime = System.nanoTime();
		
		// Change number of threads to suit your CPU capabilities
		render = new Render(WIDTH, HEIGHT, 60.0, 12);
		zoom = 0;
		screenPixels = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		
		BufferedImage photoSphereImage = null;
		try {
			photoSphereImage = ImageIO.read(new File("beach_blurred_1.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		photoSphereColor = new BufferedImage(photoSphereImage.getWidth(), photoSphereImage.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D photoSphereGraphics = photoSphereColor.createGraphics();
		photoSphereGraphics.drawImage(photoSphereImage, 0, 0, null);
		photoSphereGraphics.dispose();
		
		int[] photoSphereData = ((DataBufferInt) photoSphereColor.getRaster().getDataBuffer()).getData();
		int[] screenData = ((DataBufferInt) screenPixels.getRaster().getDataBuffer()).getData();
		renderContext = new RenderContext();
		renderContext.photoSphereColor = photoSphereData;
		renderContext.photoSphereHeight = photoSphereColor.getHeight();
		renderContext.screenPixels = screenData;
		renderContext.sdf = torusDistanceField;
		renderContext.normals = torusNormalField;
		renderContext.diffuseSpecularRatio = 0.3;
		renderContext.diffuseColor = 0xffffff;
		renderContext.voxelsDimension = s;
		
		render.begin();
		
		try {
			mouse = new Robot();
			mouse.mouseMove(frame.getX() + WIDTH / 2, frame.getY() + HEIGHT / 2);
			mouseLook = true;
		} catch (AWTException e) {
			e.printStackTrace();
		}
		
		while (!close) {
			int mx = MouseInfo.getPointerInfo().getLocation().x;
			int my = MouseInfo.getPointerInfo().getLocation().y;
			if (mouseLook && frame.hasFocus()) {
				Camera cam = render.getCamera();
				cam.yaw += 0.002 * (WIDTH / 2 - (mx - frame.getX()));
				cam.pitch += 0.002 * (HEIGHT / 2 - (my - frame.getY()));
				if (cam.pitch < -MathUtil.PI_2) cam.pitch = -MathUtil.PI_2;
				if (cam.pitch > MathUtil.PI_2) cam.pitch = MathUtil.PI_2;
				cam.position.set(new Vector3(0.0, 0.0, 0.9 * Math.pow(1.2, zoom / 2)));
				cam.position.rotateYZ(cam.pitch);
				cam.position.rotateZX(cam.yaw);
				cam.position.x += 0.5;
				cam.position.y += 0.5;
				cam.position.z += 0.5;
				mouse.mouseMove(frame.getX() + WIDTH / 2, frame.getY() + HEIGHT / 2);
				frame.getContentPane().setCursor(noCursor);
			} else {
				frame.getContentPane().setCursor(null);
			}
			
			synchronized (render) {
				render.rayMarchVoxels(renderContext);
			}
			repaint();

			numFrames++;
			if (System.nanoTime() - startTime > 1000000000) {
				System.out.println((int) (numFrames / ((System.nanoTime() - startTime) / 1.0e9)));
				startTime = System.nanoTime();
				numFrames = 0;
			}
			
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		render.end();
		frame.setVisible(false);
		frame.dispose();
	}

	@Override
	public void run() {
		frame = new JFrame("FuzzyCat Ray Marching");
		frame.add(this);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close = true;
			}
		});
		noCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "no cursor");
		frame.addKeyListener(this);
		frame.addMouseListener(this);
		frame.addMouseWheelListener(this);
		frame.setResizable(false);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);
	}
	
	public static void main(String[] args) {
		Main main = new Main();
		try {
			SwingUtilities.invokeAndWait(main);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		main.beginLoop();
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			mouseLook = false;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		mouseLook = true;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		zoom += e.getWheelRotation();
		if (zoom < 0)
			zoom = 0;
	}
}
