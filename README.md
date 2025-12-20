# NFCAAB Backend

**NFCAAB Backend** is a Spring Boot service with a REST API created for **NFCAAB (National Fake College Athletics Association Baseball)**. The service acts as the backend for the game, handling game logic, player actions, team management, and real-time updates. It integrates with the larger **NFCAAB ecosystem**, including the **NFCAAB Umpire** Discord bot.

The service is responsible for processing game data, simulating at-bats, updating player stats, managing lineups, and managing the game state. It exposes endpoints that allow the **NFCAAB Umpire** Discord bot to send requests and receive updates, enabling seamless communication between the game and Discord.

---

## Features
- **REST API**: Built with Spring Boot, the service provides a RESTful API for interacting with the game.
- **Game Logic**: Handles game logic, including at-bat simulation, player actions, and team management.
- **Team Management**: Allows players to create, manage, and customize their teams.
- **User Management**: Supports user registration, login, and authentication.
- **Stats Tracking**: Tracks player stats, team stats, game stats, season stats, conference stats, and league stats in real-time.
- **Season Management**: Manages game seasons, schedules, and standings.
- **Lineup Management**: Manages team lineups with validation.
- **Scorebug Generation**: Generates dynamic scorebug images for game display.
- **Discord Integration**: Connects with the **NFCAAB Umpire** Discord bot for player interaction and game updates.

---

## Reporting Bugs or Issues
To report bugs or submit feature requests:
- **Preferred Method**: Use the project's issue tracker. You'll need to sign up for access.
- **Alternative Method**: If the issue tracker isn't your thing, feel free to submit an issue directly.

---

## Setup Instructions
### 1. Prerequisites
- **Java Development Kit (JDK)**: Version 17 or higher.
- **Gradle**: Installed on your system for building the project.
- **Database**: MariaDB/MySQL database instance (for production) or H2 (for testing).
- **Application Properties**: A pre-configured application.properties file with sensitive information.

### 2. Clone the Repository
Clone the project to your local machine:
```bash
git clone https://github.com/akick31/nfcaab-backend.git
cd nfcaab-backend
```

### 3. Configure Application Properties

The application requires an application.properties file for configuration. This file contains sensitive information, including the database connection info, JWT tokens, email configuration, and Discord integration settings.

To get the required application.properties file:
- Contact the project maintainer directly to receive a pre-configured file.

Once you have the file:
1. Place it in the `src/main/resources` directory.
2. Double-check the values for correctness, especially database connection settings.

### 4. Set Up Database

For local development, you can use H2 (in-memory database) or set up a local MariaDB/MySQL instance:

**Using H2 (for testing):**
- H2 is included as a test dependency and will be used automatically when running tests.

**Using MariaDB/MySQL:**
- Install MariaDB or MySQL
- Create a database for the application
- Update the `application.properties` file with your database connection details

### 5. Build the Project

Use Gradle to build the project:
```bash
./gradlew build
```

### 6. Run the Application

After building, run the application with:
```bash
./gradlew bootRun
```

Or run the JAR file:
```bash
java -jar build/libs/NFCAAB-Backend-1.0.0.jar
```

### 7. Verify
- Confirm the application is running by checking the health endpoint: `http://localhost:1313/actuator/health`
- Confirm it's responding to API requests.

---

## Development Notes
- **Frontend Integration**: The application is tightly integrated with [NFCAAB-Umpire](https://github.com/akick31/NFCAAB-Umpire), but information from the Discord bot can be manually sent via Postman or other REST clients. The Discord bot displays the information on Discord for users.
- **Exposed Endpoints**: The application exposes several REST endpoints that allow the Discord bot to send requests to it. Ensure proper networking configuration if both are not running on the same machine.
- **Database**: The application uses a MariaDB/MySQL database to store game, team, user, and stat data. Make sure the database is running and accessible if you're testing the service.
- **Stats Aggregation**: After each game, stats are automatically aggregated from game stats to season stats, then to conference stats, and finally to league stats.

---

## API Endpoints

### Game Endpoints
- `POST /game` - Start a new game
- `POST /game/week` - Start all games for a week
- `GET /game/{gameId}` - Get game by ID
- `GET /game` - Get all ongoing games
- `GET /game/platform` - Get game by platform ID
- `POST /game/end` - End a game by channel ID
- `POST /game/{gameId}/end` - End a game by game ID
- `POST /game/end-all` - End all games
- `PUT /game` - Update a game

### Lineup Endpoints
- `POST /lineup/submit` - Submit a lineup for a game

### At-Bat Endpoints
- `POST /discord/at-bat/pitcher` - Submit pitcher number
- `POST /discord/at-bat/batter` - Submit batter number (swing/bunt/steal)

### Scorebug Endpoints
- `GET /scorebug?gameId={gameId}` - Get scorebug image for a game

### Other Endpoints
- `GET /actuator/health` - Health check endpoint

---

## Contributing
Contributions are welcome! If you'd like to contribute to the project, please follow these guidelines:
- **Fork the Repository**: Create your own fork of the project.
- **Create a Branch**: Make your changes in a new branch.
- **Commit Changes**: Commit your changes with clear messages.
- **Push Changes**: Push your changes to your fork.
- **Submit a Pull Request**: Create a new pull request with your changes.
- **Code Review**: Your changes will be reviewed, and feedback will be provided.
- **Merge Changes**: Once approved, your changes will be merged into the main branch.

---

Feel free to reach out with any questions or concerns. Happy coding!

---

## License
This project is licensed under the MIT License. See the LICENSE file for more information.
