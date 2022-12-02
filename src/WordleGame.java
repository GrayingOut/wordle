import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

class WordleGame {
    private final JFrame frame;
    private final JPanel mainPanel;
    private final JLabel title;
    private final JPanel wordGuessesPanel;
    private final JPanel[] wordGuessRowPanels;

    /**
     * Custom font
     */
    private Font robotoBlack24px;
    private Font robotoBlack18px;

    /**
     * The current row and column in
     * the play grid
     */
    private int cursorRow;
    private int cursorCol;

    /**
     * Colors
     */
    private static final Color characterDefaultColor = new Color(230, 230, 230);
    private static final Color characterNotPresentColor = new Color(200, 200, 200);
    private static final Color characterPresentAndWrongPosition = new Color(255, 255, 127);
    private static final Color characterPresentAndCorrectPosition = new Color(127, 255, 127);

    /*
     * If a game is active
     */
    private boolean gameActive;

    /*
     * The random word needing to be guessed
     */
    private String guessWord = "";

    public WordleGame() {
        /* Register the custom Roboto font */
        GraphicsEnvironment gEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            gEnvironment.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("Roboto-Black.ttf")));

            robotoBlack24px = new Font("Cooper Black", Font.PLAIN, 24);
            robotoBlack18px = new Font("Cooper Black", Font.PLAIN, 18);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }

        /* Choose a random first word */
        chooseRandomWord();

        /* Set game to active */
        gameActive = true;
        
        /* Initial cursor position */
        cursorRow = 0;
        cursorCol = -1;

        /* Create JFrame */
        frame = new JFrame("Wordle");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                /* Reset game on F5 */
                if (event.getKeyCode() == KeyEvent.VK_F5) {
                    /* Choose new random word, reset cursor,
                     * and set game to active
                     */
                    chooseRandomWord();
                    
                    cursorCol = -1;
                    cursorRow = 0;

                    gameActive = true;

                    /* Clear the board */
                    for (int y = 0; y < 5; y++) {
                        for (int x = 0; x < 5; x++) {
                            setGuessCharacter(y, x, characterDefaultColor, ' ');
                        }
                    }
                    return;
                }

                /* Ignore other inputs if there is no game active */
                if (!gameActive) {
                    return;
                }

                /* Check if pressed a character */
                if (Character.isLetter(event.getKeyChar())) {
                    /* Ignore if at end of row */
                    if (cursorCol >= 4) {
                        return;
                    }

                    /* Increment column and insert character */
                    cursorCol++;
                    setGuessCharacter(cursorRow, cursorCol, characterDefaultColor, Character.toUpperCase(event.getKeyChar()));
                    return;
                }
                
                /* Check if BACKSPACE - remove character */
                if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    /* Ignore if at start of row */
                    if (cursorCol < 0) {
                        return;
                    }
                    
                    /* Set character to ' ' and decrement column */
                    setGuessCharacter(cursorRow, cursorCol, characterDefaultColor, ' ');
                    cursorCol--;
                    return;
                }

                /* Check if ENTER - confirmed guess */
                if (event.getKeyChar() == KeyEvent.VK_ENTER) {
                    /* Ignore if not enough letters */
                    if (cursorCol < 4) {
                        return;
                    }

                    /* Construct word and check it exists in the file */
                    String word = "";
                    for (int i = 0; i < 5; i++) {
                        word = word + getGuessCharacter(cursorRow, i);
                    }
                    if (!wordInWordsFile(word)) {
                        JOptionPane.showMessageDialog(frame, "That word does not exist", "Non-existant word", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    /* Check if guess was correct */
                    boolean success = checkGuess();

                    /* If not success, but not on last row, increment row and reset column */
                    if (!success && cursorRow < 4) {
                        cursorCol = -1;
                        cursorRow++;
                        return;
                    }

                    /* If not success and at on last row, stop game and reveal answer */
                    if (!success && !(cursorRow < 4)) {
                        JOptionPane.showMessageDialog(frame, "The word was: "+guessWord+". Press F5 to restart", "You failed", JOptionPane.ERROR_MESSAGE);
                        gameActive = false;
                        return;
                    }

                    /* They guessed the word! */
                    JOptionPane.showMessageDialog(frame, "Well done! You guessed the word. Press F5 to restart", "You win", JOptionPane.INFORMATION_MESSAGE);
                    gameActive = false;
                }
            }
        });

        /* Main panel */
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        frame.add(mainPanel);

        /* Title label */
        title = new JLabel("Wordle");
        title.setFont(robotoBlack24px);
        title.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        mainPanel.add(title);

        /* Holds the rows */
        wordGuessesPanel = new JPanel();
        wordGuessesPanel.setLayout(new BoxLayout(wordGuessesPanel, BoxLayout.Y_AXIS));
        mainPanel.add(wordGuessesPanel);

        // This holds the word guess rows
        wordGuessRowPanels = new JPanel[5];

        /* Create 5x5 grid */
        for (int y = 0; y < 5; y++) {
            /* Crete row container */
            JPanel wordGuessRowPanel = new JPanel();
            wordGuessRowPanel.setLayout(new BoxLayout(wordGuessRowPanel, BoxLayout.X_AXIS));
            wordGuessRowPanels[y] = wordGuessRowPanel;

            /* Create the character containers */
            for (int x = 0; x < 5; x++) {
                JPanel characterPanel = new JPanel();
                characterPanel.setLayout(new BorderLayout());
                characterPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                characterPanel.setMinimumSize(new Dimension(50, 50));
                characterPanel.setPreferredSize(new Dimension(50, 50));
                characterPanel.setMaximumSize(new Dimension(50, 50));

                JLabel characterLabel = new JLabel("", SwingConstants.CENTER);
                characterLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                characterLabel.setOpaque(true);
                characterLabel.setFont(robotoBlack18px);
                characterLabel.setBackground(characterDefaultColor);
                characterPanel.add(characterLabel);

                /* Add character container to row */
                wordGuessRowPanel.add(characterPanel);
            }

            /* Add row to rows container */
            wordGuessesPanel.add(wordGuessRowPanel);
        }

        /* Fit to size */
        frame.pack();
    }

    /**
     * Checks if {@code word} is present in the words file
     * 
     * @param word The world
     * @return If the word is present
     */
    private final boolean wordInWordsFile(String word) {
        boolean inWordsFile = false;

        /* Linear search for word, def could be optimised */
        try {
            long count = Files.readAllLines(new File("words.txt").toPath())
                .stream()
                .filter(s -> s.equalsIgnoreCase(word))
                .count();
            
            if (count > 0) {
                inWordsFile = true;
            }
        } catch (IOException e) {
            /* bruhhhhhhhhhhh, why ignore */
        }

        return inWordsFile;
    }

    /**
     * Chooses a random word and sets the {@code guessWord} attribute
     * to it
     */
    private final void chooseRandomWord() {
        File file = new File("words.txt");
        try {
            List<String> words = Files.readAllLines(file.toPath());

            guessWord = words.get(new Random().nextInt(words.size())).toUpperCase();
        } catch (IOException e) {
            // Default to 'HELLO' if it fails -??????????? why?
            guessWord = "HELLO";
        }
    }

    /**
     * Checks if the users inputted word matches the word
     * to be guessed
     * 
     * @return If the words match
     */
    private final boolean checkGuess() {
        /* What did my previous self even do here??? */

        // Stores the count of each character checked
        Hashtable<Character, Integer> characterCounts = new Hashtable<>();

        // Pre-populate the hashtable
        for (int i = 0; i < 5; i++) {
            char c = getGuessCharacter(cursorRow, i);

            if (characterCounts.get(c) == null) {
                characterCounts.put(c, 0);
            }
        }

        // Holds what character indexes have been checked
        Boolean[] checkedIndexes = new Boolean[] {false, false, false, false, false};

        // Holds whether all the characters are in the
        // correct locations
        boolean isAllCorrect = true;

        // First pass, going through and comparing each
        // character to see if they are the same
        for (int i = 0; i < 5; i++) {
            char c = getGuessCharacter(cursorRow, i);

            if (guessWord.charAt(i) == c) {
                // Correct
                checkedIndexes[i] = true;
                setGuessCharacter(cursorRow, i, characterPresentAndCorrectPosition, c);
                characterCounts.put(c, characterCounts.get(c)+1);
                continue;
            }

            isAllCorrect = false;
        }

        // Check if the word is already correct after the first pass
        if (isAllCorrect) {
            return true;
        }

        // Send pass, going through all the missed indexes and seeing
        // if they exist in the word
        for (int i = 0; i < 5; i++) {
            if (checkedIndexes[i]) {
                // Already checked
                continue;
            }

            char c = getGuessCharacter(cursorRow, i);

            long characterAppearances = guessWord.chars().filter(ch -> ch == c).count();

            // Check if we have already seen all appearances
            if (characterCounts.get(c) >= characterAppearances) {
                setGuessCharacter(cursorRow, i, characterNotPresentColor, c);
                characterCounts.put(c, characterCounts.get(c)+1);
                continue;
            }

            // Not seen all appearances, so it is present, just not in
            // the correct position
            setGuessCharacter(cursorRow, i, characterPresentAndWrongPosition, c);
            characterCounts.put(c, characterCounts.get(c)+1);
        }

        return false;
    }

    /**
     * Gets the character at the specific row and col
     * 
     * @param row The row
     * @param col The col
     * @return The character
     */
    private final char getGuessCharacter(int row, int col) {
        JPanel characterPanel = (JPanel) wordGuessRowPanels[row].getComponent(col);
        JLabel characterLabel = (JLabel) characterPanel.getComponent(0);

        return characterLabel.getText().toCharArray()[0];
    }

    /**
     * Set the character and color of a grid cell thing
     * 
     * @param row       The row
     * @param col       The col
     * @param color     The color
     * @param character The character
     */
    private final void setGuessCharacter(int row, int col, Color color, char character) {
        JPanel characterPanel = (JPanel) wordGuessRowPanels[row].getComponent(col);
        JLabel characterLabel = (JLabel) characterPanel.getComponent(0);

        characterLabel.setText(Character.toString(character));
        characterLabel.setBackground(color);
    }

    /**
     * Starts the game
     */
    public final void start() {
        frame.setVisible(true);
    }
}
