package ru.incube.lite.battlepass.database;

import lombok.Data;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import ru.incube.lite.Main;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Data
@SuppressWarnings({"UnusedReturnValue", "deprecation"})
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
                Main.log.severe(String.format("[%s] Ошибка подключения к базе данных: " + e.getMessage(), Main.plugin.getDescription().getName()));
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
                Main.log.severe(String.format("[%s] Ошибка закрытия соединения с базой данных: " + e.getMessage(), Main.plugin.getDescription().getName()));
            }
        });
    }

    /**
     * Переподключиться к базе данных
     *
     * @return CompletableFuture Переподключение
     */
    public CompletableFuture<Void> reconnect() {
        return CompletableFuture.runAsync(() -> {
            close().join();
            initializeDatabase().join();
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
                    + "experience INT,"
                    + "freeBattlePassLevel INT,"
                    + "paidBattlePassLevel INT,"
                    + "freeAwardsCollected INT,"
                    + "paidAwardsCollected INT);";

            try (PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка создания таблицы: " + e.getMessage(), Main.plugin.getDescription().getName()));
            }
        });
    }

    /**
     * Добавить игрока в БД с определёнными характеристиками
     * Если игрок уже существует, то будет предупреждение в консоли
     *
     * @param player Игрок
     * @return CompletableFuture Добавление игрока
     */
    public CompletableFuture<Void> addPlayer(Player player) {
        return CompletableFuture.runAsync(() -> {
            String selectPlayerSQL = "SELECT * FROM " + TABLE + " WHERE uuid =?";
            try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                statement.setString(1, player.getUniqueId().toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Main.log.warning(String.format("[%s] Игрок %s уже существует в базе данных", Main.plugin.getDescription().getName(), player.getName()));
                        return;
                    }
                }
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка проверки существования игрока %s в базе данных: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
                return;
            }

            // Игрок начинает с первого уровня бесплатного БП
            String insertPlayerSQL = "INSERT INTO " + TABLE + " (uuid, player_name, isPaid, experience, freeBattlePassLevel, paidBattlePassLevel, freeAwardsCollected, paidAwardsCollected) VALUES (?,?, false, 0, 1, 0, 0, 0);";
            try (PreparedStatement statement = connection.prepareStatement(insertPlayerSQL)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.executeUpdate();
                Main.log.info(String.format("[%s] Игрок %s успешно добавлен в базу данных", Main.plugin.getDescription().getName(), player.getName()));
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка добавления игрока %s в базу данных: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
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
                Main.log.severe(String.format("[%s] Ошибка удаления игрока %s из базы данных: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
            }
        });
    }

    /**
     * Установить статус купленного платного БП игроку
     *
     * @param player Игрок
     * @param status Статус (true / false)
     * @return CompletableFuture Установка статуса
     */
    public CompletableFuture<Void> setPaidStatus(Player player, boolean status) {
        return CompletableFuture.runAsync(() -> {
            UUID uuid = player.getUniqueId();
            String setPaidStatusSQL = "UPDATE " + TABLE + " SET isPaid = " + status + " WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(setPaidStatusSQL)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка установки статуса купленного платного БП игроку %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
            }
        });
    }

    /**
     * Получить куплен ли платный БП игроком
     *
     * @param player Игрок
     * @return CompletableFuture Получение статуса
     */
    public CompletableFuture<Boolean> isPaid(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID uuid = player.getUniqueId();
            String isPaidSQL = "SELECT isPaid FROM " + TABLE + " WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(isPaidSQL)) {
                statement.setString(1, uuid.toString());
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return resultSet.getBoolean("isPaid");
                } else {
                    Main.log.warning(String.format("[%s] Игрок %s не найден в базе данных", Main.plugin.getDescription().getName(), player.getName()));
                    return false;
                }
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка получения статуса купленного платного БП игрока %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
                return false;
            }
        });
    }

    /**
     * Установить количество опыта игроку
     * Опыт не может быть меньше 0
     *
     * @param player     Игрок
     * @param experience Опыт
     * @return CompletableFuture Установка опыта
     */
    public CompletableFuture<Void> setExperience(Player player, int experience) {
        return CompletableFuture.runAsync(() -> {
            UUID uuid = player.getUniqueId();
            String setExperienceSQL = "UPDATE " + TABLE + " SET experience = ? WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(setExperienceSQL)) {
                statement.setInt(1, Math.max(experience, 0));
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка установки опыта игроку %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
            }
        });
    }

    /**
     * Получить количество опыта игрока
     *
     * @param player Игрок
     * @return CompletableFuture Получение опыта
     */
    public CompletableFuture<Integer> getExperience(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID uuid = player.getUniqueId();
            String getExperienceSQL = "SELECT experience FROM " + TABLE + " WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(getExperienceSQL)) {
                statement.setString(1, uuid.toString());
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return resultSet.getInt("experience");
                } else {
                    Main.log.warning(String.format("[%s] Игрок %s не найден в базе данных", Main.plugin.getDescription().getName(), player.getName()));
                    return 0;
                }
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка получения опыта игрока %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
                return 0;
            }
        });
    }

    /**
     * Установить уровень бесплатного БП игроку
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
                Main.log.severe(String.format("[%s] Ошибка установки уровня бесплатного БП игроку %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
            }
        });
    }

    /**
     * Получить уровень бесплатного БП игрока
     *
     * @param player Игрок
     * @return CompletableFuture Получение уровня
     */
    public CompletableFuture<Integer> getFreeLevel(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID uuid = player.getUniqueId();
            String getFreeLevelSQL = "SELECT freeBattlePassLevel FROM " + TABLE + " WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(getFreeLevelSQL)) {
                statement.setString(1, uuid.toString());
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return resultSet.getInt("freeBattlePassLevel");
                } else {
                    Main.log.warning(String.format("[%s] Игрок %s не найден в базе данных", Main.plugin.getDescription().getName(), player.getName()));
                    return 0;
                }
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка получения уровня бесплатного БП игрока %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
                return 0;
            }
        });
    }

    /**
     * Установить уровень платного БП игроку
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
                Main.log.severe(String.format("[%s] Ошибка установки уровня платного БП игроку %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
            }
        });
    }

    /**
     * Получить уровень платного БП игрока
     *
     * @param player Игрок
     * @return CompletableFuture Получение уровня
     */
    public CompletableFuture<Integer> getPaidLevel(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID uuid = player.getUniqueId();
            String getPaidLevelSQL = "SELECT paidBattlePassLevel FROM " + TABLE + " WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(getPaidLevelSQL)) {
                statement.setString(1, uuid.toString());
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return resultSet.getInt("paidBattlePassLevel");
                } else {
                    Main.log.warning(String.format("[%s] Игрок %s не найден в базе данных", Main.plugin.getDescription().getName(), player.getName()));
                    return 0;
                }
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка получения уровня платного БП игрока %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
                return 0;
            }
        });
    }

    /**
     * Установить количество собранных наград бесплатного БП игроку
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
                Main.log.severe(String.format("[%s] Ошибка установки количества собранных наград бесплатного БП игроку %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
            }
        });
    }

    /**
     * Получить количество собранных наград бесплатного БП игрока
     *
     * @param player Игрок
     * @return CompletableFuture Получение количества
     */
    public CompletableFuture<Integer> getFreeAwardsCollected(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID uuid = player.getUniqueId();
            String getFreeAwardsCollectedSQL = "SELECT freeAwardsCollected FROM " + TABLE + " WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(getFreeAwardsCollectedSQL)) {
                statement.setString(1, uuid.toString());
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return resultSet.getInt("freeAwardsCollected");
                } else {
                    Main.log.warning(String.format("[%s] Игрок %s не найден в базе данных", Main.plugin.getDescription().getName(), player.getName()));
                    return 0;
                }
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка получения количества собранных наград бесплатного БП игрока %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
                return 0;
            }
        });
    }

    /**
     * Установить количество собранных наград платного БП игроку
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
                Main.log.severe(String.format("[%s] Ошибка установки количества собранных наград платного БП игроку %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
            }
        });
    }

    /**
     * Получить количество собранных наград платного БП игрока
     *
     * @param player Игрок
     * @return CompletableFuture Получение количества
     */
    public CompletableFuture<Integer> getPaidAwardsCollected(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID uuid = player.getUniqueId();
            String getPaidAwardsCollectedSQL = "SELECT paidAwardsCollected FROM " + TABLE + " WHERE uuid = ?;";
            try (PreparedStatement statement = connection.prepareStatement(getPaidAwardsCollectedSQL)) {
                statement.setString(1, uuid.toString());
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return resultSet.getInt("paidAwardsCollected");
                } else {
                    Main.log.warning(String.format("[%s] Игрок %s не найден в базе данных", Main.plugin.getDescription().getName(), player.getName()));
                    return 0;
                }
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка получения количества собранных наград платного БП игрока %s: " + e.getMessage(), Main.plugin.getDescription().getName(), player.getName()));
                return 0;
            }
        });
    }

    /**
     * Тест проверка наличия таблицы в БД
     *
     * @param tableName Название таблицы
     * @return CompletableFuture Существование таблицы
     */
    public CompletableFuture<Boolean> tableExists(String tableName) {
        return CompletableFuture.supplyAsync(() -> {
            String tableExistsSQL = "SELECT * FROM information_schema.tables WHERE table_schema = ? AND table_name = ? LIMIT 1;";
            try (PreparedStatement statement = connection.prepareStatement(tableExistsSQL)) {
                statement.setString(1, DATABASE);
                statement.setString(2, tableName);
                ResultSet resultSet = statement.executeQuery();

                return resultSet.next();
            } catch (SQLException e) {
                Main.log.severe(String.format("[%s] Ошибка проверки наличия таблицы %s в базе данных: " + e.getMessage(), Main.plugin.getDescription().getName(), tableName));
                return false;
            }
        });
    }

    /**
     * Вывести все данные об игроке
     *
     * @param player Игрок
     * @return CompletableFuture Вывод данных
     */
    @SuppressWarnings("deprecation")
    public CompletableFuture<Void> printPlayerData(Player player) {
        return CompletableFuture.runAsync(() -> {
            player.sendMessage("Данные об игроке " + player.getName() + ":");
            player.sendMessage("Платный ли БП: " + ChatColor.GREEN + isPaid(player).join());
            player.sendMessage("Опыт: " + ChatColor.GREEN + getExperience(player).join());
            player.sendMessage("Уровень бесплатного БП: " + ChatColor.GREEN + getFreeLevel(player).join());
            player.sendMessage("Уровень платного БП: " + ChatColor.GREEN + getPaidLevel(player).join());
            player.sendMessage("Количество собранных наград бесплатного БП: " + ChatColor.GREEN + getFreeAwardsCollected(player).join());
            player.sendMessage("Количество собранных наград платного БП: " + ChatColor.GREEN + getPaidAwardsCollected(player).join());
        });
    }
}
