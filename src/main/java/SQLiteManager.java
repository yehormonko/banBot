import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class SQLiteManager {
    private String url = "jdbc:sqlite:db";

    public SQLiteManager() {
        createNewDatabase();
    }

    public void createNewDatabase() {
        File file = new File("db");

        if (file.exists()) {
            return;
        }
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String sql = "CREATE TABLE chats (" +
                        "chat_id text PRIMARY KEY," +
                        "chat_title text," +
                        "value_type TEXT ," +
                        "value INTEGER DEFAULT 0," +
                        "    greet TEXT " +
                        ");" +
                        "";
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.execute();
                sql = "CREATE TABLE ban(" +
                        "    chat_id text," +
                        "    user_id text," +
                        "    removal_date TEXT" +
                        ");";
                PreparedStatement statement1 = conn.prepareStatement(sql);
                statement1.execute();
                statement.close();
                statement1.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addChat(String chatId, String chat_title) {
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String select = "select count(*) from chats where chat_id = ?";
                PreparedStatement statement = conn.prepareStatement(select);
                statement.setString(1, chatId);
                ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                if (resultSet.getInt(1) == 0) {
                    String insert = "insert into chats (chat_id, chat_title) values (?,?)";
                    statement = conn.prepareStatement(insert);
                    statement.setString(1, chatId);
                    statement.setString(2, chat_title);
                    statement.execute();
                }
                statement.close();
                resultSet.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<String> getChats() {
        ArrayList<String> chats = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String select = "select chat_id from chats";
                PreparedStatement statement = conn.prepareStatement(select);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    chats.add(resultSet.getString(1));
                }
                statement.close();
                resultSet.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return chats;
    }

    public ChatInfo getChatInfo(String chatId) {
        ChatInfo chatInfo = new ChatInfo();
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String select = "select * from chats where chat_id = ?";
                PreparedStatement statement = conn.prepareStatement(select);
                statement.setString(1, chatId);
                ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                chatInfo.setChat_id(resultSet.getString(1));
                chatInfo.setChat_title(resultSet.getString(2));
                chatInfo.setValue_type(resultSet.getString(3));
                chatInfo.setValue(resultSet.getInt(4));
                chatInfo.setGreet(resultSet.getString(5));
                statement.close();
                resultSet.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return chatInfo;
    }

    public void update(String column, String value, String chatId) {
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String update = "update chats set " + column + "=? where chat_id = ?";
                PreparedStatement statement = conn.prepareStatement(update);
                statement.setString(2, chatId);
                if (column.equals("value")) statement.setInt(1, Integer.parseInt(value));
                else statement.setString(1, value);
                statement.executeUpdate();
                statement.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void changeValue(String value, String chatId) {
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String update = "update chats set value = value" + (value.equals("valuePlus") ? "+" : "-") + "1 " +
                        "where chat_id = ?" + (value.equals("valuePlus") ? ";" : " and (value-1)>=0");
                PreparedStatement statement = conn.prepareStatement(update);
                statement.setString(1, chatId);
                statement.execute();
                statement.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addPotentialBan(String chatId, String userId, LocalDateTime removalDate) {
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String insert = "insert into ban (chat_id, user_id, removal_date) values (?,?,?)";
                PreparedStatement statement = conn.prepareStatement(insert);
                statement.setString(1, chatId);
                statement.setString(2, userId);
                statement.setString(3, removalDate.toString());
                statement.execute();
                statement.close();
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public int tryToRemoveBan(String chatId, String userId) {
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String delete = "delete from ban where chat_id=? and user_id=?";
                PreparedStatement statement = conn.prepareStatement(delete);
                statement.setString(1, chatId);
                statement.setString(2, userId);
                int res = statement.executeUpdate();
                statement.close();
                return res;
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0;
    }

    public ArrayList<BanInfo> getAllBans() {
        ArrayList<BanInfo> banInfos = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String select = "select * from ban";
                PreparedStatement statement = conn.prepareStatement(select);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    BanInfo banInfo = new BanInfo();
                    banInfo.setChatId(resultSet.getString(1));
                    banInfo.setUserId(resultSet.getString(2));
                    banInfo.setBanDate(LocalDateTime.parse(resultSet.getString(3)));
                    banInfos.add(banInfo);
                }
                statement.close();
                return banInfos;
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return banInfos;
    }
}
