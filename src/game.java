
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class game extends JPanel implements KeyListener, Runnable {
	
	
	boolean al_hold;
	int ene_x_hold;
	int ene_y_hold;
	int ene_health_hold;
	// game properties:
	public int screenWidth;
	public int screenHeight;
	
	public final int DELAY = 20;
	
	public boolean running = false;
	
	public BufferedImage bg;
	//BufferedImage bg = new BufferedImage(screenWidth, screenHeight, )
	public int bgY = 0;
	public final int BGDELAY = 12;
	// end of game properties
	
	// misc:
	public int score = 0;
	String todisp;
	// end of misc.
	
	// player properties:
	public final int SPEED = 2;
	public final int MOVEDELAY = 50;
	
	public final double coolDownInMillis = 360;
	public double lastTime = 0;
	public double thisTime = 0;
	
	public final int PROJ_SPEED = 1;
	//int _speed, int _pierce, int posX, int posY
	public int projSpeedY = 12;
	public int projPierce = 1;
	public int movement = 0;
	public int hits = 0;
	
	public boolean shooting = false;
	
	public int x;
	public int y;
	
	public final int PLAYER_WIDTH = 64;
	public final int PLAYER_HEIGHT = 48;
	
	// player properties end
	
	public Random random = new Random();
	
	// enemies:
	public List<Boolean> enemyAlive = new ArrayList<Boolean>();
	
	public List<Integer> enemyX = new ArrayList<Integer>();
	public List<Integer> enemyY = new ArrayList<Integer>();
	
	public List<Integer> enemyHealth = new ArrayList<Integer>();
	
	public final int enemyW = 50;
	public final int enemyH = 50;
	
	public BufferedImage enemy1;
	public BufferedImage enemy2;
	public BufferedImage enemy3;
	// end of enemies
	
	// game
	// JFrame frame;
	public JFrame frame;
	public JPanel panel;
	
	public Timer ready;
	// end of frame properties
	
	// projectile:
	public int projIndex = 0;
	
	public final int SIZE = 10;
	public int[] positionX = {0,0,0,0,0,0,0}; // array of positions for existing projectiles, 
	// so only a set amount of projectiles can be on screen at a given time
	public int[] positionY = {0,0,0,0,0,0,0}; // ^
	
	public boolean[] active = {false, false, false, false, false, false, false};
	
	public int[] projSpeed = {0,0,0,0,0,0,0}; // ^
	public int[] speedX = {0,0,0,0,0,0,0};
	public int[] pierce = {0,0,0,0,0,0,0};
	
	public int[] projDelay = {0,0,0,0,0,0,0};
	
	/*
	Timer projTimerv = new Timer(DELAY,
			(e) -> {
				projMove();
	});
	*/
	
	public Thread projMoveThread = new Thread(
			() -> {
				Timer projTimerv = new Timer(DELAY,
						(e) -> {
							projMove();
				});
				projTimerv.start();
	});
	
	// end of projectile
	
	game() {
		
		
		frame = new JFrame();
		panel = new JPanel();
		
		makeFrameFullSize(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setBounds(0,0,screenWidth, screenHeight);
		
		frame.setResizable(false);
		
		// we need to have the players position set up
		x = (screenWidth - PLAYER_WIDTH)/2;
		y = screenHeight - (72 + PLAYER_HEIGHT);
		
		try {
			enemy1 = ImageIO.read(
					new File("assets\\enemy1.png"));/*.getScaledInstance(enemyW, enemyH, Image.SCALE_DEFAULT);*/
			enemy1 = resize(enemy1, enemyW, enemyH);
		} catch (IOException e1) {}
		
		try {
			enemy2 = ImageIO.read(
					new File("assets\\enemy2.png"));
			enemy2 = resize(enemy2, enemyW, enemyH);
		} catch (IOException e2) {}
		
		try {
			enemy3 = ImageIO.read(
					new File("assets\\enemy3.png"));
			enemy3 = resize(enemy3, enemyW, enemyH);
		} catch (IOException e3) {}
		
		try {
			bg = ImageIO.read(
					new File("assets\\background.png"));
			bg = resize(bg, screenWidth, screenHeight);
		} catch (IOException bge) {}
		
		
		frame.add(this);
		frame.addKeyListener(this);
		
		
		frame.setVisible(true);
		
		
		Timer moveTimer = new Timer(MOVEDELAY, 
				e -> {
					move();
					if (shooting) {
						try {
							
							thisTime = System.currentTimeMillis();
						    if(thisTime - lastTime >= coolDownInMillis){
						    	
						    	projectile(projSpeedY, projPierce, x + (PLAYER_WIDTH - SIZE)/2, y - SIZE, (int) movement/2);
						        lastTime = System.currentTimeMillis();
						        
						    }
						} catch (IOException io) {
						}
					}
		});
		
		Thread raster = new Thread(this);
		
		Thread backgroundMover = new Thread(
				() -> {
					Timer bgm = new Timer(BGDELAY, 
							(e) -> {
								bgY += 10;
								if (bgY >= screenHeight) bgY = 0;
					});
					bgm.start();
		});
		
		Thread enemySpawnerThread = new Thread(
				() -> {
					Timer enemySpawn = new Timer(2000, 
							e -> {
								makeEnemy(random.nextInt(1,3));
							}
					);
					enemySpawn.start();
				}
		);
		
		// todo
		/*
		Thread enemyMover = new Thread(
				() -> {
					
				}
		);
		*/
		
		raster.start();						// controls the graphics being updated
		moveTimer.start();					// moves the player if their movement indicates they should be moving
		backgroundMover.start();			// moves the background
		enemySpawnerThread.start();			// spawns enemies
		projMoveThread.start(); // added	// controls the movement of projectiles
		
	}
	public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
	    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
	    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2d = dimg.createGraphics();
	    g2d.drawImage(tmp, 0, 0, null);
	    g2d.dispose();

	    return dimg;
	} 
	
	public void paint(Graphics g) {
		
		// background
		g.drawImage(bg, 0, bgY, null);
		g.drawImage(bg, 0, bgY - screenHeight, null); // so we have a constant background thats always moving
		// end of background
		
		
		// enemy:
		//g.drawImage(enemy3, 50, 50, null);
		for (int j = 0; j < enemyAlive.size(); j++) {
			if(enemyHealth.get(j) == 3) {
				g.drawImage(enemy3, enemyX.get(j), enemyY.get(j), null);
			}
			else if(enemyHealth.get(j) == 2) {
				g.drawImage(enemy2, enemyX.get(j), enemyY.get(j), null);
			}
			else if(enemyHealth.get(j) == 1) {
				g.drawImage(enemy1, enemyX.get(j), enemyY.get(j), null);
			}
		}
		// end of enemy
		
		
		// projectile
		g.setColor(Color.red);
		for (int proj = 0; proj < active.length - 1; proj++) {
			if (active[proj]) {
				g.fillRect(positionX[proj], positionY[proj] ,SIZE, SIZE);
			}
		}
		// end of projectile
		
		
		// player
		g.setColor(Color.yellow);
		g.fillRect(x,y, PLAYER_WIDTH, PLAYER_HEIGHT);
		// end of player
		
		
		// misc:
		g.setColor(Color.white);
		todisp = String.format("Score: %d", score);
		g.setFont(new Font("MV Boli", Font.PLAIN, 22));
		FontMetrics metrics = getFontMetrics(g.getFont());
		g.drawString(todisp, (int) (screenWidth - (1.4 * metrics.stringWidth(todisp))), 
				(int) (1.2 * g.getFont().getSize()));
		
		todisp = String.format("Enemies: %d", enemyAlive.size());
		g.setFont(new Font("MV Boli", Font.PLAIN, 22));
		metrics = getFontMetrics(g.getFont());
		g.drawString(todisp, (int) (screenWidth - (1.4 * metrics.stringWidth(todisp))), 
				(int) (2.2 * g.getFont().getSize()));
		// end of misc.
		
		
		// dispose of Graphics:
		g.dispose();
		
	}
	

	private void move() {
		if (x <= 0) {
			movement = 0;
			x = 1;
		}
		else if (x >= screenWidth - PLAYER_WIDTH) {
			movement = 0;
			x = screenWidth - (PLAYER_WIDTH + 1);
		}
		x += movement * 12;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_A) {
			movement = -SPEED;
		}
		else if (e.getKeyCode() == KeyEvent.VK_D) {
			movement = +SPEED;
		}
		else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			
			shooting = true;
			
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_D) {
			movement = 0;
		}
		else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			shooting = false;
		}
	}
	
	// make the frame take the size of the whole screen; we wont change the size of assets in the game
	private void makeFrameFullSize(JFrame aFrame) {
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    
	    // just to store them in our global variables
	    screenWidth = screenSize.width;
	    screenHeight = screenSize.height;
	    
	    aFrame.setSize(screenSize.width, screenSize.height);
	    
	}
	
	
	// projectile
	
	public void projectile(int _speed, int _pierce, int posX, int posY, int _speedX) throws IOException {
		
		projIndex ++;
		if (projIndex >= active.length) projIndex = 0;
		
		this.positionX[projIndex] = posX;
		this.positionY[projIndex] = posY;
		
		this.projSpeed[projIndex] = _speed;
		this.pierce[projIndex] = _pierce;
		this.speedX[projIndex] = _speedX;
		this.active[projIndex] = true;
		
		this.projDelay[projIndex] = (int) (500/_speed);
		
	}

	void projMove() {
		
		for (int i = 0; i < active.length; i++) {
			positionX[i] += speedX[i];
			positionY[i] -= projSpeed[i];
			intersect(i);
		}
		
	}
		
	void hit(int i) {
		hits += 1;
		pierce[i] -= 1;
		if (pierce[i] <= 0) {
			active[i] = false;
		}
	}
		
	public void intersect(int j) {
		for (int i = 0; i < enemyAlive.size(); i++) {
			if (positionX[j] > enemyX.get(i) - SIZE
					&& positionX[j] < enemyX.get(i) + enemyW
					&& positionY[j] > enemyY.get(i) - SIZE
					&& positionY[j] < enemyY.get(i) + enemyH
					) {
				if (active[j]) {
					if (enemyHealth.get(i) > 0) {
						hit(j);
					}
					enemyHealth.set(i,  enemyHealth.get(i) - 1);
					checkEnemyHealth(i);
				}
			}
		}
	}
	
	// end of projectile
	
	// utility functions:
	protected void listUpdate(int x, int y, int health, boolean alive) {
		
		for (int i = 0; i < enemyAlive.size(); i++) {
			if (!enemyAlive.get(i)) {
				enemyAlive.add(i, alive);
				enemyX.add(i, x);
				enemyY.add(i, y);
				enemyHealth.add(i, health);
				return;
			}
		}
		enemyAlive.add(alive);
		enemyX.add(x);
		enemyY.add(y);
		enemyHealth.add(health);
		
	}
	
	public void makeEnemy(int count) {
		for (int i = 0; i < count ; i++) {
			listUpdate(random.nextInt(0, screenWidth - enemyW),
					10 * random.nextInt(0, (int) (screenHeight * 0.6)/10),
					random.nextInt(1, 4),
					true);
		}
	}
	
	protected void checkEnemyHealth(int i) {
		if(enemyHealth.get(i) <= 0) {
			deadEnemy(i);
		}
	}
	
	protected void deadEnemy(int i) {
		score += 1;
		al_hold = enemyAlive.remove(i);
		ene_x_hold = enemyX.remove(i);
		ene_y_hold = enemyY.remove(i);
		ene_health_hold = enemyHealth.remove(i);
	}
	
	// end of utilites
	

	@Override
	public void run() {
		Timer running = new Timer(DELAY, 
				(e) -> {
					repaint();
		});
		running.start();
	}

}
