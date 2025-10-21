import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MarioRunner extends JPanel implements ActionListener, KeyListener {
    private int marioX = 50;       // Mario's X position
    private int marioY = 300;      // Mario's Y position
    private int speed = 2;         // Movement speed (lower = smoother)
    private int jumpStrength = 12; // Jump height
    private int gravity = 1;       // Gravity force
    private int velocityY = 0;     // Vertical velocity
    private boolean isJumping = false;
    private Timer timer;

    public MarioRunner() {
        setPreferredSize(new Dimension(800, 400));
        setBackground(new Color(135, 206, 235)); // Sky blue background
        setFocusable(true);
        addKeyListener(this);

        // Refresh rate: 20ms (~50 FPS)
        timer = new Timer(20, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Ground
        g.setColor(new Color(34, 139, 34)); // green
        g.fillRect(0, 350, 800, 50);

        // Mario (red square)
        g.setColor(Color.RED);
        g.fillRect(marioX, marioY, 40, 40);

        // Draw simple blocks (like tiles)
        g.setColor(Color.ORANGE);
        for (int i = 0; i < 8; i++) {
            g.fillRect(i * 100, 300, 50, 50);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Gravity effect
        if (isJumping) {
            marioY -= velocityY;
            velocityY -= gravity;

            // Stop jump when Mario reaches ground
            if (marioY >= 300) {
                marioY = 300;
                isJumping = false;
                velocityY = 0;
            }
        }

        // Keep movement smooth
        repaint();
    }

    // --- Key Controls ---
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_RIGHT) {
            marioX += speed; // move right smoothly
        }
        if (key == KeyEvent.VK_LEFT) {
            marioX -= speed; // move left smoothly
        }
        if (key == KeyEvent.VK_SPACE && !isJumping) {
            isJumping = true;
            velocityY = jumpStrength; // jump up
        }
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    // --- Main Method ---
    public static void main(String[] args) {
        JFrame frame = new JFrame("Mario Runner - Smooth Movement");
        MarioRunner game = new MarioRunner();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

