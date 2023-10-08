package ru.incube.lite.battlepass.database;

import lombok.Data;
import org.bukkit.entity.Player;
import ru.incube.lite.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Data
public class BattlePassDatabase {
    private static final String HOST = Main.plugin.getConfig().getString("BPdatabase.host");
    private static final String PORT = Main.plugin.getConfig().getString("BPdatabase.port");
    private static final String DATABASE = Main.plugin.getConfig().getString("BPdatabase.database");
    private static final String USERNAME = Main.plugin.getConfig().getString("BPdatabase.username");
    private static final String PASSWORD = Main.plugin.getConfig().getString("BPdatabase.password");
    private static final String TABLE = Main.plugin.getConfig().getString("BPdatabase.table");
    private static final boolean useSSL = Main.plugin.getConfig().getBoolean("BPdatabase.useSSL");
    private static final boolean autoReconnect = Main.plugin.getConfig().getBoolean("BPdatabase.autoReconnect");
    private Connection connection;

    public BattlePassDatabase() {
        initializeDatabase().join();
    }

    /**
     * Инициализировать подключение к базе данных
     *
     * @return CompletableFuture Завершение инициализации
     */
    private CompletableFuture<Void> initializeDatabase() {
        return CompletableFuture.runAsync(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + "?useSSL=" + useSSL + "&autoReconnect=" + autoReconnect;
                connection = DriverManager.getConnection(url, USERNAME, PASSWORD);
                createTable().join();
            } catch (ClassNotFoundException | SQLException e) {
                Main.log.severe("Ошибка подключения к базе данных: " + e.getMessage());
            }
        });
    }

    /**
     * Закрыть соединение с базой данных
     *
     * @return CompletableFuture Завершение соединения
     */
    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                Main.log.severe("Ошибка закрытия соединения с базой данных: " + e.getMessage());
            }
        });
    }

    /**
     * Создать таблицу для хранения информации об игроке, если она не существует
     *
     * @return CompletableFuture Создание таблицы
     */
    public CompletableFuture<Void> createTable() {
        return CompletableFuture.runAsync(() -> {
            // TODO: 08.10.2023 Переписать инструкции на подходящие
            String createTableSQL = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + "uuid CHAR(36) PRIMARY KEY,"
                    + "player_name VARCHAR(64),"
                    + "isPaid BOOLEAN,"
                    + "freeBattlePassLevel INT,"
                    + "paidBattlePassLevel INT,"
                    + "freeAwardsCollected INT,"
                    + "paidAwardsCollected INT);";

            try (PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка создания таблицы: " + e.getMessage());
            }
        });
    }

    /**
     * Добавить игрока в БД с определёнными характеристиками
     *
     * @param player Игрок
     * @return CompletableFuture Добавление игрока
     */
    public CompletableFuture<Void> addPlayer(Player player) {
        return CompletableFuture.runAsync(() -> {
            // TODO: 08.10.2023 Переписать инструкции на подходящие
            String insertPlayerSQL = "INSERT INTO " + TABLE + " (uuid, player_name, isPaid, freeBattlePassLevel, paidBattlePassLevel, freeAwardsCollected, paidAwardsCollected) VALUES (?, ?, false, 0, 0, 0, 0);";
            try (PreparedStatement statement = connection.prepareStatement(insertPlayerSQL)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка добавления игрока " + player.getName() + " в базу данных: " + e.getMessage());
            }
        });
    }

    /**
     * Удалить игрока из БД
     *
     * @param player Игрок
     * @return CompletableFuture Удаление игрока
     */
    public CompletableFuture<Void> deletePlayer(Player player) {
        return CompletableFuture.runAsync(() -> {
            UUID uuid = player.getUniqueId();
            String deletePlayerSQL = "DELETE FROM " + TABLE + " WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(deletePlayerSQL)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка удаления игрока " + player.getName() + " из базы данных: " + e.getMessage());
            }
        });
    }

    /**
     * Установить оплаченный статус БП игроку
     *
     * @param player Игрок
     * @return CompletableFuture Установка статуса
     */
    public CompletableFuture<Void> setPaidStatus(Player player) {
        return CompletableFuture.runAsync(() -> {
            UUID uuid = player.getUniqueId();
            String setPaidStatusSQL = "UPDATE " + TABLE + " SET isPaid = true WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(setPaidStatusSQL)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка установки оплаченного статуса игроку " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Установить бесплатный уровень БП игроку
     *
     * @param player Игрок
     * @param level  Уровень
     * @return CompletableFuture Установка уровня
     */
    public CompletableFuture<Void> setFreeLevel(Player player, int level) {
        return CompletableFuture.runAsync(() -> {
            UUID uuid = player.getUniqueId();
            String setFreeLevelSQL = "UPDATE " + TABLE + " SET freeBattlePassLevel = ? WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(setFreeLevelSQL)) {
                statement.setInt(1, level);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка установки бесплатного уровня БП игроку " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Установить оплаченный уровень БП игроку
     *
     * @param player Игрок
     * @param level  Уровень
     * @return CompletableFuture Установка уровня
     */
    public CompletableFuture<Void> setPaidLevel(Player player, int level) {
        return CompletableFuture.runAsync(() -> {
            UUID uuid = player.getUniqueId();
            String setPaidLevelSQL = "UPDATE " + TABLE + " SET paidBattlePassLevel = ? WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(setPaidLevelSQL)) {
                statement.setInt(1, level);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка установки оплаченного уровня БП игроку " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Установить количество собранных бесплатных наград игроку
     *
     * @param player Игрок
     * @param count  Количество
     * @return CompletableFuture Установка количества
     */
    public CompletableFuture<Void> setFreeAwardsCollected(Player player, int count) {
        return CompletableFuture.runAsync(() -> {
            UUID uuid = player.getUniqueId();
            String setFreeAwardsCollectedSQL = "UPDATE " + TABLE + " SET freeAwardsCollected = ? WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(setFreeAwardsCollectedSQL)) {
                statement.setInt(1, count);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка установки количества собранных бесплатных наград игроку " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Установить количество собранных оплаченных наград игроку
     *
     * @param player Игрок
     * @param count  Количество
     * @return CompletableFuture Установка количества
     */
    public CompletableFuture<Void> setPaidAwardsCollected(Player player, int count) {
        return CompletableFuture.runAsync(() -> {
            UUID uuid = player.getUniqueId();
            String setPaidAwardsCollectedSQL = "UPDATE " + TABLE + " SET paidAwardsCollected = ? WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(setPaidAwardsCollectedSQL)) {
                statement.setInt(1, count);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка установки количества собранных оплаченных наград игроку " + player.getName() + ": " + e.getMessage());
            }
        });
    }
}
