package com.mycompany.snakejavas;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class SnakeJavas {
    static final int N = 20, C = 24, S = N * C;                
    static final String[] MODES = {"Easy", "Medium", "Hard"};
    static final int[] DELAY = {200, 140, 90};                   
    static final int[] DX = {0, 0, -1, 1}, DY = {-1, 1, 0, 0};   
    static final Preferences P = Preferences.userNodeForPackage(SnakeJavas.class);;;;

    record Entry(String name, int score, String mode) {}

    static List<Entry> loadScores() {
        List<Entry> list = new ArrayList<>();
        for (String line : P.get("scores", "").split("\n")) {
            String[] a = line.split("\t");
            if (a.length == 3) {
                try { list.add(new Entry(a[0], Integer.parseInt(a[1]), a[2])); } catch (Exception ignore) { }
            }
        }
        return list;
    }

    static void saveScores(List<Entry> list) {
        StringBuilder sb = new StringBuilder();
        for (Entry e : list) sb.append(e.name()).append('\t').append(e.score()).append('\t').append(e.mode()).append('\n');
        P.put("scores", sb.toString());
    }

    static void addScore(Entry e) {
        List<Entry> list = loadScores();
        list.add(e);
        list.sort((a, b) -> b.score() - a.score());
        saveScores(list.size() > 10 ? list.subList(0, 10) : list);
    }

    JFrame frame = new JFrame("Snake");
    CardLayout cards = new CardLayout();
    JPanel root = new JPanel(cards);
    JTextField nameField = new JTextField(P.get("name", "Player"), 12);
    JComboBox<String> modeBox = new JComboBox<>(MODES);
    JTextArea scoreArea = new JTextArea();
    Board board = new Board();

    SnakeJavas() {
        root.add(buildMenu(), "menu");
        root.add(board, "game");
        modeBox.setSelectedIndex(1);
        frame.setContentPane(root);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        showScores();
        frame.setVisible(true);
    }

    JPanel buildMenu() {
        JPanel menu = new JPanel(new BorderLayout(10, 10));
        menu.setPreferredSize(new Dimension(S, S));
        menu.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Snake", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 36));

        JButton play = new JButton("Play");
        play.addActionListener(e -> startGame());
        JPanel options = new JPanel(new FlowLayout());
        options.add(new JLabel("Name:"));
        options.add(nameField);
        options.add(modeBox);
        options.add(play);

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(title);
        top.add(options);

        scoreArea.setEditable(false);
        scoreArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JButton clear = new JButton("Clear scores");
        clear.addActionListener(e -> { saveScores(new ArrayList<>()); showScores(); });

        menu.add(top, BorderLayout.NORTH);
        menu.add(new JScrollPane(scoreArea), BorderLayout.CENTER);
        menu.add(clear, BorderLayout.SOUTH);
        return menu;
    }

    void showScores() {
        StringBuilder sb = new StringBuilder("High scores\n\n");
        int rank = 1;
        for (Entry e : loadScores())
            sb.append(String.format("%2d. %-12s %4d  (%s)%n", rank++, e.name(), e.score(), e.mode()));
        if (rank == 1) sb.append("No scores yet.");
        scoreArea.setText(sb.toString());
    }

    void startGame() {
        String name = nameField.getText().trim().replaceAll("[\\t\\r\\n]", "");
        if (name.isEmpty()) name = "Player";
        P.put("name", name);
        cards.show(root, "game");
        board.start(name, modeBox.getSelectedIndex());
        board.requestFocusInWindow();
    }

    void toMenu() {
        board.timer.stop();
        showScores();
        cards.show(root, "menu");
    }

    class Board extends JPanel {
        List<Point> snake = new ArrayList<>();
        Deque<Integer> queue = new ArrayDeque<>(); 
         
        Point food;
        int dir, score, mode;
        String name = "Player";
        boolean over, paused;
        javax.swing.Timer timer = new javax.swing.Timer(150, e -> tick());
        Random rnd = new Random();

        Board() {
            setPreferredSize(new Dimension(S, S));
            setFocusable(true);
            addKeyListener(new KeyAdapter() { @Override public void keyPressed(KeyEvent e) { key(e.getKeyCode()); }
            });
        }

        void start(String playerName, int modeIndex) {
            name = playerName;
            mode = modeIndex;
            snake.clear();
            snake.add(new Point(10, 10));
            snake.add(new Point(9, 10));
            snake.add(new Point(8, 10));
            queue.clear();
            dir = 3;
            score = 0;
            over = false;
            paused = false;
            placeFood();
            timer.setDelay(DELAY[mode]);
            timer.start();
            repaint();
        }

        void placeFood() {
            List<Point> free = new ArrayList<>();
            for (int x = 0; x < N; x++)
                for (int y = 0; y < N; y++)
                    if (!snake.contains(new Point(x, y))) free.add(new Point(x, y));
            food = free.isEmpty() ? null : free.get(rnd.nextInt(free.size()));
        }

        void key(int code) {
            switch (code) {
                case KeyEvent.VK_UP, KeyEvent.VK_W -> turn(0);
                case KeyEvent.VK_DOWN, KeyEvent.VK_S -> turn(1);
                case KeyEvent.VK_LEFT, KeyEvent.VK_A -> turn(2);
                case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> turn(3);
                case KeyEvent.VK_SPACE, KeyEvent.VK_P -> togglePause();
                case KeyEvent.VK_ENTER -> { if (over) start(name, mode); }
                case KeyEvent.VK_ESCAPE -> toMenu();
                default -> { }
            }
        }

        void turn(int k) {
            if (over || paused) return;
            int last = queue.isEmpty() ? dir : queue.peekLast();
            if (k == last || (k ^ 1) == last) return;    
            if (queue.size() < 2) queue.add(k);
        }

        void togglePause() {
            if (over) return;
            paused = !paused;
            if (paused) timer.stop(); else timer.start();
            repaint();
        }

        void tick() {
            if (!queue.isEmpty()) dir = queue.poll();
            Point head = new Point(snake.get(0).x + DX[dir], snake.get(0).y + DY[dir]);
            boolean eat = head.equals(food);
            int check = eat ? snake.size() : snake.size() - 1;   
            boolean hitWall = head.x < 0 || head.y < 0 || head.x >= N || head.y >= N;
            if (hitWall || snake.subList(0, check).contains(head)) { die(); return; }

            snake.add(0, head);
            if (eat) {
                score++;
                placeFood();
                timer.setDelay(Math.max(50, DELAY[mode] - score * 3));
            } else {
                snake.remove(snake.size() - 1);
            }
            repaint();
        }

        void die() {
            over = true;
            timer.stop();
            addScore(new Entry(name, score, MODES[mode]));
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int x = 0; x < N; x++)
                for (int y = 0; y < N; y++) {
                    g.setColor((x + y) % 2 == 0 ? new Color(0x68b944) : new Color(0x5fae3c));
                    g.fillRect(x * C, y * C, C, C);
                }
            if (food != null) {
                g.setColor(Color.GRAY);
                g.fillOval(food.x * C + 3, food.y * C + 3, C - 6, C - 6);
            }
            for (int i = 0; i < snake.size(); i++) {
                Point p = snake.get(i);
                g.setColor(i == 0 ? new Color(0x2f3b1f) : new Color(0x4b5320));
                g.fillRoundRect(p.x * C + 1, p.y * C + 1, C - 2, C - 2, 10, 10);
            }
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString(name + " - Score: " + score, 8, 20);

            if (over) overlay(g, "Game over", "Enter = restart, Esc = menu");
            else if (paused) overlay(g, "Paused", "Space = resume, Esc = menu");
        }

        void overlay(Graphics2D g, String title, String hint) {
            g.setColor(new Color(0, 0, 0, 170));
            g.fillRect(0, 0, S, S);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            g.drawString(title, (S - g.getFontMetrics().stringWidth(title)) / 2, S / 2 - 10);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.drawString(hint, (S - g.getFontMetrics().stringWidth(hint)) / 2, S / 2 + 25);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SnakeJavas::new);
    }
}

