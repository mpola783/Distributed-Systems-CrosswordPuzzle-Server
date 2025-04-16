import game.Game;

public class GameTest {
    public static void main(String[] args) {
        // Create a simple 3x5 solution grid
        char[][] solutionGrid = {
            {'H', 'E', 'L', 'L', 'O'},
            {'W', 'O', 'R', 'L', 'D'},
            {'C', 'A', 'T', '.', 'S'}
        };

        // Initialize game
        Game game = new Game("test-123", 3, 3, new String[] { "HELLO", "WORLD", "CATS" }, false, 1);
        game.setFinishedGridRow(0, solutionGrid[0]);
        game.setFinishedGridRow(1, solutionGrid[1]);
        game.setFinishedGridRow(2, solutionGrid[2]);

        char[][] emptyGrid = new char[3][5];
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                emptyGrid[y][x] = '-';
            }
        }
        game.setPlayerGrid(emptyGrid);

        // Print starting grid
        System.out.println("Initial Grid:");
        printGrid(game.getPlayerGrid());

        // Simulate a guess (letter)
        game.checkGuess("S", "Player1");

        // Simulate a guess (full word)
        game.checkGuess("WORLD", "Player1");

        game.checkGuess("EOA", "Player1");

        // Print updated grid
        System.out.println("\nUpdated Grid:");
        printGrid(game.getPlayerGrid());

        // Check win/loss
        System.out.println("\nGame Status: " + game.getGameStatus());
        System.out.println("Lives Remaining: " + game.getLives());

        game.checkGuess("HELLO", "Player1");
        game.checkGuess("CAT", "Player1");
        game.displayGrid("player");
        game.displayGrid("finished");
    }

    public static void printGrid(char[][] grid) {
        for (char[] row : grid) {
            for (char c : row) {
                System.out.print(c + " ");
            }
            System.out.println();
        }
    }

}